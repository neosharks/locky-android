package com.neosharks.locky

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neosharks.locky.databinding.ActivityMainBinding

/**
 * Home screen. Requires unlock on entry (fingerprint / device credential), then
 * shows the list of locked apps. The + button opens the app picker.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repo: LockRepository
    private lateinit var adapter: LockedAppsAdapter
    private var authenticated = false
    private var pickerOpened = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repo = LockRepository(this)

        adapter = LockedAppsAdapter(onUnlock = ::confirmUnlock)
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        binding.fab.setOnClickListener {
            startActivity(Intent(this, AppPickerActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (authenticated) {
            refresh()
            checkPermissions()
        } else {
            authenticateEntry()
        }
    }

    private fun authenticateEntry() {
        // Hide content until the user proves identity.
        binding.root.visibility = View.INVISIBLE
        BiometricHelper.prompt(
            activity = this,
            title = getString(R.string.entry_title),
            subtitle = getString(R.string.entry_subtitle),
            onSuccess = {
                authenticated = true
                binding.root.visibility = View.VISIBLE
                refresh()
                if (!maybeOpenPickerFromShortcut()) checkPermissions()
            },
            onFailure = { finish() }
        )
    }

    /** If launched from the "Lock / Unlock an app" icon shortcut, jump straight
     *  to the picker (after the entry gate above has already verified identity). */
    private fun maybeOpenPickerFromShortcut(): Boolean {
        if (pickerOpened) return false
        if (intent?.action != ACTION_LOCK && intent?.action != ACTION_UNLOCK) return false
        pickerOpened = true
        startActivity(Intent(this, AppPickerActivity::class.java))
        return true
    }

    private fun refresh() {
        val apps = AppsProvider.loadLockedApps(this, repo.getLockedApps())
        adapter.submit(apps)
        binding.empty.visibility = if (apps.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun confirmUnlock(app: AppInfo) {
        BiometricHelper.prompt(
            activity = this,
            title = getString(R.string.unlock_title, app.label),
            subtitle = getString(R.string.confirm_identity),
            onSuccess = {
                repo.unlock(app.packageName)
                refresh()
            },
            onFailure = {}
        )
    }

    // --- Permission onboarding ---------------------------------------------

    private fun checkPermissions() {
        val hasOverlay = Settings.canDrawOverlays(this)
        val hasAccessibility = isAccessibilityEnabled()
        if (hasOverlay && hasAccessibility) return

        val msg = buildString {
            append(getString(R.string.perm_intro))
            if (!hasOverlay) append("\n\n•  ").append(getString(R.string.perm_overlay))
            if (!hasAccessibility) append("\n\n•  ").append(getString(R.string.perm_accessibility))
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.perm_title)
            .setMessage(msg)
            .setCancelable(false)
            .setPositiveButton(
                if (!hasOverlay) R.string.grant_overlay else R.string.open_accessibility
            ) { _, _ ->
                if (!hasOverlay) {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                    )
                } else {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
            .setNegativeButton(R.string.later, null)
            .show()
    }

    private fun isAccessibilityEnabled(): Boolean {
        val expected =
            ComponentName(this, AppLockAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    companion object {
        // Actions fired by the launcher-icon long-press shortcuts.
        const val ACTION_LOCK = "com.neosharks.locky.action.LOCK"
        const val ACTION_UNLOCK = "com.neosharks.locky.action.UNLOCK"
    }
}
