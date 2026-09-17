package com.velune.launcher

import android.content.Context

/**
 * Centralized settings storage for Velune. Every toggle in SettingsActivity
 * reads/writes through here so the rest of the app has one place to check
 * "is this feature on for this app / in general".
 */
object Prefs {

    private const val PREFS_NAME = "velune_settings"

    // ---- Mindful Launch Delay ----

    private const val KEY_LAUNCH_DELAY_ENABLED = "launch_delay_enabled"
    private const val KEY_LAUNCH_DELAY_SECONDS = "launch_delay_seconds"

    fun isLaunchDelayEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_LAUNCH_DELAY_ENABLED, false)

    fun setLaunchDelayEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_LAUNCH_DELAY_ENABLED, enabled).apply()
    }

    fun getLaunchDelaySeconds(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_LAUNCH_DELAY_SECONDS, 15)

    fun setLaunchDelaySeconds(context: Context, seconds: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_LAUNCH_DELAY_SECONDS, seconds).apply()
    }

    // ---- Notification Filter (storage ready; capture logic ships in a later phase) ----

    private const val KEY_NOTIFICATION_FILTER_ENABLED = "notification_filter_enabled"

    fun isNotificationFilterEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTIFICATION_FILTER_ENABLED, false)

    fun setNotificationFilterEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_NOTIFICATION_FILTER_ENABLED, enabled).apply()
    }

    // ---- Monochrome mode (storage ready; greyscale rendering ships in a later phase) ----

    private const val KEY_MONOCHROME_APPS = "monochrome_apps"

    fun getMonochromeApps(context: Context): Set<String> =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_MONOCHROME_APPS, emptySet()) ?: emptySet()

    fun setAppMonochrome(context: Context, packageName: String, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = getMonochromeApps(context).toMutableSet()
        if (enabled) current.add(packageName) else current.remove(packageName)
        prefs.edit().putStringSet(KEY_MONOCHROME_APPS, current).apply()
    }

    // ---- Hidden apps (same storage the existing home-screen long-press dialog uses) ----

    fun getHiddenApps(context: Context): Set<String> =
        context.getSharedPreferences("hidden_apps", Context.MODE_PRIVATE)
            .getStringSet("apps", emptySet()) ?: emptySet()

    fun setAppHidden(context: Context, packageName: String, hidden: Boolean) {
        val prefs = context.getSharedPreferences("hidden_apps", Context.MODE_PRIVATE)
        val current = getHiddenApps(context).toMutableSet()
        if (hidden) current.add(packageName) else current.remove(packageName)
        prefs.edit().putStringSet("apps", current).apply()
    }
}
