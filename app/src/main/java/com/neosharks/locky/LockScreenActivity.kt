package com.neosharks.locky

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

/**
 * Opaque lock screen shown over a locked app. It renders the same lock UI as the
 * service's bridge overlay (so the swap is invisible) and hosts the fingerprint
 * prompt — being a real top activity is what lets the system show BiometricPrompt.
 *
 * Once this activity is drawn it asks the service to remove the bridge overlay,
 * so the fingerprint dialog is never hidden behind it.
 *
 * Success → mark the app unlocked and finish (service reveals it).
 * Cancel / Back → go Home, so the locked app is never revealed.
 */
class LockScreenActivity : AppCompatActivity() {

    private var targetPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.overlay_lock)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })
        findViewById<MaterialButton>(R.id.unlockBtn).setOnClickListener { authenticate() }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        targetPackage = intent?.getStringExtra(EXTRA_PKG)
        val app = targetPackage?.let {
            AppsProvider.loadLockedApps(this, setOf(it)).firstOrNull()
        }
        findViewById<TextView>(R.id.appName).text = app?.label ?: getString(R.string.app_name)
        app?.icon?.let { findViewById<ImageView>(R.id.lockIcon).setImageDrawable(it) }

        // Once we're actually on screen, drop the service's bridge overlay so the
        // fingerprint dialog shows on top of this (identical-looking) activity.
        window.decorView.post { AppLockAccessibilityService.instance?.clearBridgeOverlay() }

        authenticate()
    }

    private fun authenticate() {
        val pkg = targetPackage ?: run { goHome(); return }
        BiometricHelper.prompt(
            activity = this,
            title = findViewById<TextView>(R.id.appName).text.toString(),
            subtitle = getString(R.string.fingerprint_only),
            onSuccess = {
                UnlockState.unlocked.add(pkg)
                finish()
            },
            onFailure = { goHome() }
        )
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        finish()
    }

    companion object {
        const val EXTRA_PKG = "extra_pkg"
    }
}
