package com.velune.launcher

import android.content.Context

/**
 * Centralized settings storage for Velune. Every toggle in SettingsActivity
 * reads/writes through here so the rest of the app has one place to check
 * "is this feature on for this app / in general".
 */
object Prefs {

    private const val PREFS_NAME = "velune_settings"

    // ---- Onboarding ----

    fun isOnboardingComplete(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("onboarding_complete", false)

    fun setOnboardingComplete(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("onboarding_complete", true).apply()
    }

    // ---- Appearance ----

    /** One of: white, amber, blue, green, pink */
    fun getAccentColorName(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("accent_color", "white") ?: "white"

    fun setAccentColorName(context: Context, name: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString("accent_color", name).apply()
    }

    /** One of the Android system font families: sans-serif, sans-serif-light, sans-serif-medium, sans-serif-condensed, serif */
    fun getFontFamily(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("font_family", "sans-serif-light") ?: "sans-serif-light"

    fun setFontFamily(context: Context, family: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString("font_family", family).apply()
    }

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

    // ---- Filtered notifications (captured by VeluneNotificationListener) ----

    private const val FILTERED_NOTIFS_PREFS = "filtered_notifications"

    /** Each entry: "timestamp|packageName|title|text" — pipes in title/text are stripped on the way in. */
    fun storeFilteredNotification(context: Context, packageName: String, title: String, text: String) {
        val prefs = context.getSharedPreferences(FILTERED_NOTIFS_PREFS, Context.MODE_PRIVATE)
        val entry = "${System.currentTimeMillis()}|$packageName|${title.replace("|", " ")}|${text.replace("|", " ")}"
        val current = (prefs.getStringSet("entries", emptySet()) ?: emptySet()).toMutableSet()
        current.add(entry)
        prefs.edit().putStringSet("entries", current).apply()
    }

    fun getFilteredNotifications(context: Context): List<String> =
        (context.getSharedPreferences(FILTERED_NOTIFS_PREFS, Context.MODE_PRIVATE)
            .getStringSet("entries", emptySet()) ?: emptySet())
            .sortedDescending()

    fun clearFilteredNotifications(context: Context) {
        context.getSharedPreferences(FILTERED_NOTIFS_PREFS, Context.MODE_PRIVATE)
            .edit().remove("entries").apply()
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

    // ---- Favorites ----

    fun getFavorites(context: Context): List<String> =
        context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
            .getStringSet("apps", emptySet())?.toList() ?: emptyList()

    fun setAppFavorite(context: Context, packageName: String, favorite: Boolean) {
        val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
        val current = getFavorites(context).toMutableSet()
        if (favorite) current.add(packageName) else current.remove(packageName)
        prefs.edit().putStringSet("apps", current).apply()
    }

    // ---- Rename ----

    fun getCustomName(context: Context, packageName: String): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("custom_name_$packageName", null)

    fun setCustomName(context: Context, packageName: String, name: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (name.isNullOrBlank()) {
            prefs.edit().remove("custom_name_$packageName").apply()
        } else {
            prefs.edit().putString("custom_name_$packageName", name.trim()).apply()
        }
    }

    // ---- Folders (an app belongs to at most one folder) ----

    private const val FOLDERS_PREFS = "folders"

    fun getFolders(context: Context): Map<String, Set<String>> {
        val all = context.getSharedPreferences(FOLDERS_PREFS, Context.MODE_PRIVATE).all
        return all.mapNotNull { (name, value) ->
            (value as? Set<*>)?.let { name to it.filterIsInstance<String>().toSet() }
        }.toMap()
    }

    fun getFolderForApp(context: Context, packageName: String): String? =
        getFolders(context).entries.find { it.value.contains(packageName) }?.key

    fun addAppToFolder(context: Context, folderName: String, packageName: String) {
        val prefs = context.getSharedPreferences(FOLDERS_PREFS, Context.MODE_PRIVATE)
        val current = getFolders(context)
        val editor = prefs.edit()

        // An app belongs to at most one folder — drop it from any other first.
        current.forEach { (name, apps) ->
            if (name != folderName && apps.contains(packageName)) {
                editor.putStringSet(name, apps - packageName)
            }
        }

        editor.putStringSet(folderName, (current[folderName] ?: emptySet()) + packageName)
        editor.apply()
    }

    fun removeAppFromFolder(context: Context, packageName: String) {
        val folderName = getFolderForApp(context, packageName) ?: return
        val updated = (getFolders(context)[folderName] ?: emptySet()) - packageName
        context.getSharedPreferences(FOLDERS_PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(folderName, updated).apply()
    }

    fun deleteFolder(context: Context, folderName: String) {
        context.getSharedPreferences(FOLDERS_PREFS, Context.MODE_PRIVATE)
            .edit().remove(folderName).apply()
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
