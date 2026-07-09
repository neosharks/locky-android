package com.neosharks.locky

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.button.MaterialButton

/**
 * Watches which app is in the foreground. When a locked app appears and isn't
 * already unlocked for this session, it INSTANTLY covers the screen with an
 * opaque overlay window (added synchronously here — no Activity-launch latency,
 * so there is no flash of the app) and then launches a tiny transparent
 * [LockScreenActivity] that hosts the fingerprint prompt.
 *
 * Why the extra activity: the system refuses to show BiometricPrompt unless the
 * requester is the top *activity* (anti-phishing). A window/overlay isn't enough.
 * So the overlay provides the visuals (kills the flash) and the transparent
 * activity behind it satisfies that rule.
 *
 * Relocking rules:
 *  - navigating away from a locked app relocks it (and tears down the overlay);
 *  - turning the screen off relocks everything.
 */
class AppLockAccessibilityService : AccessibilityService() {

    private lateinit var repo: LockRepository
    private lateinit var windowManager: WindowManager

    private var overlay: View? = null
    private var overlayPkg: String? = null
    private var lastForeground: String? = null

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            UnlockState.unlocked.clear()
            lastForeground = null
            hideOverlay()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        repo = LockRepository(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    /** Called by the gate activity once it is drawn, so the fingerprint dialog
     *  is not hidden behind the bridge overlay. */
    fun clearBridgeOverlay() = hideOverlay()

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        // Our own gate activity — never let it move the overlay or count as leaving.
        if (pkg == packageName) return

        // While the gate is showing, the fingerprint dialog (systemui) appears on
        // top; ignore it so we don't tear the gate down or relock prematurely.
        if (overlayPkg != null) {
            when {
                pkg == SYSTEM_UI -> return
                pkg == overlayPkg -> {
                    // Same app — reveal it only if it was just unlocked.
                    if (UnlockState.unlocked.contains(pkg)) hideOverlay()
                    return
                }
                else -> hideOverlay() // user navigated away from the gate (Home/other)
            }
        }

        // Relock the app we just left. "Left" = any move to a different package,
        // INCLUDING the launcher, another app, or recents / quick settings
        // (systemui) — i.e. every time a locked app goes to the background.
        if (pkg != lastForeground) {
            lastForeground?.let { UnlockState.unlocked.remove(it) }
            lastForeground = pkg
        }

        // Don't raise the gate for systemui itself (recents/status bar).
        if (pkg != SYSTEM_UI && repo.isLocked(pkg) && !UnlockState.unlocked.contains(pkg)) {
            showOverlay(pkg)
        }
    }

    /** Instantly cover the screen, then launch the fingerprint gate. */
    private fun showOverlay(pkg: String) {
        // Inflate with the app's Material3 theme — the raw service context has none.
        val themed = ContextThemeWrapper(this, R.style.Theme_AppLock)
        val view = LayoutInflater.from(themed).inflate(R.layout.overlay_lock, null)

        val app = AppsProvider.loadLockedApps(this, setOf(pkg)).firstOrNull()
        view.findViewById<TextView>(R.id.appName).text = app?.label ?: getString(R.string.app_name)
        app?.icon?.let { view.findViewById<ImageView>(R.id.lockIcon).setImageDrawable(it) }
        // Retry button (reachable after a cancelled prompt) re-launches the gate.
        view.findViewById<MaterialButton>(R.id.unlockBtn).setOnClickListener { launchGate(pkg) }

        // Focusable so it swallows Back — Back must not reach the locked app.
        view.isFocusableInTouchMode = true
        view.setOnKeyListener { _, keyCode, e ->
            keyCode == KeyEvent.KEYCODE_BACK && e.action == KeyEvent.ACTION_UP
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE
        ).apply { gravity = Gravity.TOP or Gravity.START }

        runCatching { windowManager.addView(view, params) }
            .onSuccess {
                overlay = view
                overlayPkg = pkg
                launchGate(pkg)
            }
    }

    private fun launchGate(pkg: String) {
        val intent = Intent(this, LockScreenActivity::class.java).apply {
            putExtra(LockScreenActivity.EXTRA_PKG, pkg)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
        }
        startActivity(intent)
    }

    private fun hideOverlay() {
        overlay?.let { v -> runCatching { windowManager.removeView(v) } }
        overlay = null
        overlayPkg = null
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) instance = null
        hideOverlay()
        runCatching { unregisterReceiver(screenOffReceiver) }
    }

    companion object {
        private const val SYSTEM_UI = "com.android.systemui"

        /** Live service instance, so the gate activity can drop the bridge overlay. */
        @Volatile
        var instance: AppLockAccessibilityService? = null
            private set
    }
}
