package com.neosharks.locky

import java.util.Collections

/**
 * In-memory record of which locked apps are currently unlocked for this session.
 * Shared (same process) between the accessibility service and the lock screen.
 * Cleared when the screen turns off or a locked app leaves the foreground, so
 * reopening a locked app always asks again.
 */
object UnlockState {
    val unlocked: MutableSet<String> = Collections.synchronizedSet(HashSet())
}
