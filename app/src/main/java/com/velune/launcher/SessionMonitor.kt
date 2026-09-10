package com.velune.launcher

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

class SessionMonitor(private val context: Context) {

    fun isAppOpen(packageName: String): Boolean {

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE)
                    as UsageStatsManager

        val time = System.currentTimeMillis()

        val events = usageStatsManager.queryEvents(
            time - 1000 * 10,
            time
        )

        val event = UsageEvents.Event()

        var currentApp = ""

        while (events.hasNextEvent()) {

            events.getNextEvent(event)

            if (event.eventType ==
                UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {

                currentApp = event.packageName
            }
        }

        return currentApp == packageName
    }
}