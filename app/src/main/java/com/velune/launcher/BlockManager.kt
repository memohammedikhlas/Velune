package com.velune.launcher

import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

/**
 * App blocking, matching the reference app's "App Blocking" feature two ways:
 * an explicit time-bound block (pick 1 hour to 30 days) and a recurring
 * Blocking Schedule (block during chosen days/hours every week). Also surfaces
 * real per-app screen time (last 7 days) via UsageStatsManager, used both to
 * show "you spent X on this app" and to suggest other apps worth blocking.
 */
object BlockManager {

    private const val BLOCKS_PREFS = "app_blocks"
    private const val SCHEDULES_PREFS = "schedules"

    // ---- Explicit time-bound block ----

    fun getBlockedUntil(context: Context, packageName: String): Long? {
        val value = context.getSharedPreferences(BLOCKS_PREFS, Context.MODE_PRIVATE)
            .getLong(packageName, -1L)
        return if (value <= 0L) null else value
    }

    fun blockApp(context: Context, packageName: String, durationMillis: Long) {
        context.getSharedPreferences(BLOCKS_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(packageName, System.currentTimeMillis() + durationMillis)
            .apply()
    }

    fun unblockApp(context: Context, packageName: String) {
        context.getSharedPreferences(BLOCKS_PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(packageName)
            .apply()
    }

    /** All packages with a currently-active explicit block, mapped to their expiry timestamp. */
    fun getAllActiveBlocks(context: Context): Map<String, Long> {
        val now = System.currentTimeMillis()
        val all = context.getSharedPreferences(BLOCKS_PREFS, Context.MODE_PRIVATE).all

        return all.mapNotNull { (pkg, value) ->
            val expiry = value as? Long ?: return@mapNotNull null
            if (expiry > now) pkg to expiry else null
        }.toMap()
    }

    // ---- Blocking Schedules (recurring weekly window) ----

    /** [days] uses Calendar.DAY_OF_WEEK values (1=Sunday..7=Saturday); [startMinute]/[endMinute] are minutes since midnight. */
    data class Schedule(val days: Set<Int>, val startMinute: Int, val endMinute: Int)

    fun getSchedule(context: Context, packageName: String): Schedule? {
        val raw = context.getSharedPreferences(SCHEDULES_PREFS, Context.MODE_PRIVATE)
            .getString(packageName, null) ?: return null

        val parts = raw.split("|")
        if (parts.size != 3) return null

        val days = parts[0].split(",").mapNotNull { it.toIntOrNull() }.toSet()
        val start = parts[1].toIntOrNull() ?: return null
        val end = parts[2].toIntOrNull() ?: return null
        return Schedule(days, start, end)
    }

    fun setSchedule(context: Context, packageName: String, days: Set<Int>, startMinute: Int, endMinute: Int) {
        val raw = "${days.joinToString(",")}|$startMinute|$endMinute"
        context.getSharedPreferences(SCHEDULES_PREFS, Context.MODE_PRIVATE)
            .edit().putString(packageName, raw).apply()
    }

    fun clearSchedule(context: Context, packageName: String) {
        context.getSharedPreferences(SCHEDULES_PREFS, Context.MODE_PRIVATE)
            .edit().remove(packageName).apply()
    }

    private fun isWithinSchedule(context: Context, packageName: String): Boolean {
        val schedule = getSchedule(context, packageName) ?: return false
        val cal = Calendar.getInstance()

        if (cal.get(Calendar.DAY_OF_WEEK) !in schedule.days) return false

        val minutesNow = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return if (schedule.startMinute <= schedule.endMinute) {
            minutesNow in schedule.startMinute until schedule.endMinute
        } else {
            // overnight window, e.g. 22:00 -> 06:00
            minutesNow >= schedule.startMinute || minutesNow < schedule.endMinute
        }
    }

    // ---- Combined check used everywhere an app is opened ----

    fun isCurrentlyBlocked(context: Context, packageName: String): Boolean {
        val until = getBlockedUntil(context, packageName)
        if (until != null && until > System.currentTimeMillis()) return true
        return isWithinSchedule(context, packageName)
    }

    /** Null if [packageName] isn't blocked right now; otherwise a ready-to-show reason. */
    fun blockedMessage(context: Context, packageName: String, appName: String): String? {
        val until = getBlockedUntil(context, packageName)
        if (until != null && until > System.currentTimeMillis()) {
            val remainingMinutes = (until - System.currentTimeMillis()) / 60000L + 1
            return "$appName is blocked for ${formatMinutes(remainingMinutes)} more"
        }

        if (isWithinSchedule(context, packageName)) {
            val end = getSchedule(context, packageName)!!.endMinute
            return "$appName is blocked until %02d:%02d (scheduled)".format(end / 60, end % 60)
        }

        return null
    }

    // ---- Usage stats ----

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
