package com.neosharks.locky

import android.graphics.drawable.Drawable

/** A launchable app shown in the lists. */
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    var locked: Boolean
)
