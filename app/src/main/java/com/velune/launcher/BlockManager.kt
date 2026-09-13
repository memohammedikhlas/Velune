package com.velune.launcher

import android.app.usage.UsageStatsManager
import android.content.Context

/**
 * Time-bound app blocking, matching the reference app's "App Blocking" feature:
 * pick a duration (1 hour to 30 days) and the app is unopenable from Velune
 * until that time passes. Also surfaces real per-app screen time (last 7 days)
 * via UsageStatsManager, used both to show "you spent X on this app" and to
 * suggest other apps worth blocking.
 */
object BlockManager {

    private const val PREFS_NAME = "app_blocks"

    fun isBlocked(context: Context, packageName: String): Boolean {
        val until = getBlockedUntil(context, packageName)
        return until != null && until > System.currentTimeMillis()
    }

    fun getBlockedUntil(context: Context, packageName: String): Long? {
        val value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(packageName, -1L)
        return if (value <= 0L) null else value
    }

    fun blockApp(context: Context, packageName: String, durationMillis: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(packageName, System.currentTimeMillis() + durationMillis)
            .apply()
    }

    fun unblockApp(context: Context, packageName: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(packageName)
            .apply()
    }

    /** All packages with a currently-active block, mapped to their expiry timestamp. */
    fun getAllActiveBlocks(context: Context): Map<String, Long> {
        val now = System.currentTimeMillis()
        val all = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).all

        return all.mapNotNull { (pkg, value) ->
            val expiry = value as? Long ?: return@mapNotNull null
            if (expiry > now) pkg to expiry else null
        }.toMap()
    }

    /** Minutes spent in [packageName] over the last 7 days. Needs Usage Access permission. */
    fun getWeeklyUsageMinutes(context: Context, packageName: String): Long =
        getWeeklyUsageMinutesForAllApps(context)[packageName] ?: 0L

    /** Every app's package name mapped to its total foreground minutes, last 7 days. */
    fun getWeeklyUsageMinutesForAllApps(context: Context): Map<String, Long> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()

        val end = System.currentTimeMillis()
        val start = end - 7L * 24 * 60 * 60 * 1000

        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, end)
            ?: return emptyMap()

        return stats
            .groupBy { it.packageName }
            .mapValues { (_, entries) -> entries.sumOf { it.totalTimeInForeground } / 60000L }
            .filterValues { it > 0L }
    }

    fun formatMinutes(minutes: Long): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }
}
