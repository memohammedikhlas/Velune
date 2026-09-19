package com.velune.launcher

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Requires the user to grant "Notification access" in system settings — see
 * Prefs.isNotificationFilterEnabled / AppUtils.isNotificationAccessGranted.
 * When enabled, notifications from apps that are currently blocked (explicit
 * block or Blocking Schedule) are hidden and saved for later instead of shown.
 */
class VeluneNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (packageName == applicationContext.packageName) return

        if (!Prefs.isNotificationFilterEnabled(applicationContext)) return
        if (!BlockManager.isCurrentlyBlocked(applicationContext, packageName)) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        Prefs.storeFilteredNotification(applicationContext, packageName, title, text)
        cancelNotification(sbn.key)
    }
}
