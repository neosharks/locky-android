package com.neosharks.locky

import android.content.Context

/** Persists the set of locked package names in SharedPreferences. */
class LockRepository(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getLockedApps(): Set<String> =
        prefs.getStringSet(KEY_LOCKED, emptySet())?.toSet() ?: emptySet()

    fun isLocked(pkg: String): Boolean =
        prefs.getStringSet(KEY_LOCKED, emptySet())?.contains(pkg) == true

    fun lock(pkg: String) = save(getLockedApps() + pkg)

    fun unlock(pkg: String) {
        save(getLockedApps() - pkg)
        UnlockState.unlocked.remove(pkg)
    }

    private fun save(set: Set<String>) {
        // Store a fresh copy — SharedPreferences must not be handed a mutated set.
        prefs.edit().putStringSet(KEY_LOCKED, HashSet(set)).apply()
    }

    companion object {
        private const val PREFS = "applock_prefs"
        private const val KEY_LOCKED = "locked_apps"
    }
}
