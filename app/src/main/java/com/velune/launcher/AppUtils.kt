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

/** Maps the user's chosen accent name to an actual color int. */
fun accentColor(context: Context): Int {
    val hex = when (Prefs.getAccentColorName(context)) {
        "amber" -> "#FFC107"
        "blue" -> "#4FC3F7"
        "green" -> "#81C784"
        "pink" -> "#F48FB1"
        else -> "#FFFFFF"
    }
    return android.graphics.Color.parseColor(hex)
}

val ACCENT_COLOR_OPTIONS = listOf("white" to "#FFFFFF", "amber" to "#FFC107", "blue" to "#4FC3F7", "green" to "#81C784", "pink" to "#F48FB1")

val FONT_OPTIONS = listOf(
    "sans-serif-light" to "Light",
    "sans-serif" to "Default",
    "sans-serif-medium" to "Medium",
    "sans-serif-condensed" to "Condensed",
    "serif" to "Serif"
)

/** ArrayAdapter for app-name lists that applies the user's chosen font style (Settings > Appearance). */
fun appNameAdapter(context: Context, items: List<String>): android.widget.ArrayAdapter<String> {
    val typeface = android.graphics.Typeface.create(Prefs.getFontFamily(context), android.graphics.Typeface.NORMAL)
    return object : android.widget.ArrayAdapter<String>(context, R.layout.list_item_app_name, items) {
        override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
            val view = super.getView(position, convertView, parent) as android.widget.TextView
            view.typeface = typeface
            return view
        }
    }
}
