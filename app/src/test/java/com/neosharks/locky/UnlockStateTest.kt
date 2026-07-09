package com.neosharks.locky

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Smoke tests for the in-memory unlocked-session set — the one piece of pure
 * logic not tied to the Android framework.
 */
class UnlockStateTest {

    @Before
    fun reset() {
        UnlockState.unlocked.clear()
    }

    @Test
    fun addedPackageIsUnlocked() {
        UnlockState.unlocked.add("com.example.app")
        assertTrue(UnlockState.unlocked.contains("com.example.app"))
    }

    @Test
    fun removedPackageRelocks() {
        UnlockState.unlocked.add("com.example.app")
        UnlockState.unlocked.remove("com.example.app")
        assertFalse(UnlockState.unlocked.contains("com.example.app"))
    }

    @Test
    fun unrelatedPackageStaysLocked() {
        UnlockState.unlocked.add("com.example.a")
        assertFalse(UnlockState.unlocked.contains("com.example.b"))
    }
}
