package com.neosharks.locky

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/** Queries the system PackageManager for launchable apps. */
object AppsProvider {

    /** Every launchable app on the device (excluding AppLock itself), sorted by name. */
    fun loadAllApps(context: Context, locked: Set<String>): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val self = context.packageName
        return pm.queryIntentActivities(intent, 0)
            .asSequence()
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != self }
            .mapNotNull { pkg -> toAppInfo(pm, pkg, locked.contains(pkg)) }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    /** Only the currently locked apps (skips any that were since uninstalled). */
    fun loadLockedApps(context: Context, locked: Set<String>): List<AppInfo> {
        val pm = context.packageManager
        return locked
            .mapNotNull { pkg -> toAppInfo(pm, pkg, true) }
            .sortedBy { it.label.lowercase() }
    }

    private fun toAppInfo(pm: PackageManager, pkg: String, locked: Boolean): AppInfo? = try {
        val ai = pm.getApplicationInfo(pkg, 0)
        AppInfo(pkg, pm.getApplicationLabel(ai).toString(), pm.getApplicationIcon(ai), locked)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}
