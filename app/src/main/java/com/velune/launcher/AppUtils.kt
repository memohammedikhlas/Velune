package com.velune.launcher

import android.content.Context

/** Resolves an installed app's display name — a custom rename if set, else the system label. */
fun appLabel(context: Context, packageName: String): String {
    Prefs.getCustomName(context, packageName)?.let { return it }

    return try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (e: Exception) {
        packageName
    }
}

/** Whether the user has granted Velune's notification listener access (needed for Notification Filter). */
fun isNotificationAccessGranted(context: Context): Boolean =
    androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context)
        .contains(context.packageName)
