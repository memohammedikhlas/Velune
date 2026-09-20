package com.velune.launcher

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

/**
 * Single launch path for opening any app from Velune. If Mindful Launch Delay
 * is off (default), this behaves exactly like a normal startActivity call.
 * If it's on, it shows a short countdown first — a deliberate pause before
 * the app opens, per the Mindful Launch Delay setting.
 */
object MindfulLaunch {

    fun launch(context: Context, intent: Intent, onLaunched: () -> Unit = {}) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (!Prefs.isLaunchDelayEnabled(context)) {
            performLaunch(context, intent, onLaunched)
            return
        }

        var remaining = Prefs.getLaunchDelaySeconds(context)
        if (remaining <= 0) {
            performLaunch(context, intent, onLaunched)
            return
        }

        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val messageView = TextView(context).apply {
            textSize = 20f
            setTextColor(context.getColor(R.color.white))
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            gravity = Gravity.CENTER
            setPadding(dp(32), dp(40), dp(32), dp(40))
            text = "Opening in $remaining..."
        }

        val dialog = AlertDialog.Builder(context)
            .setView(messageView)
            .setCancelable(false)
            .create()
        dialog.show()

        val handler = Handler(Looper.getMainLooper())

        val tick = object : Runnable {
            override fun run() {
                remaining--
                if (remaining > 0) {
                    messageView.text = "Opening in $remaining..."
                    handler.postDelayed(this, 1000)
                } else {
                    dialog.dismiss()
                    performLaunch(context, intent, onLaunched)
                }
            }
        }
        handler.postDelayed(tick, 1000)
    }

    private fun performLaunch(context: Context, intent: Intent, onLaunched: () -> Unit) {
        context.startActivity(intent)

        val packageName = intent.component?.packageName
        if (packageName != null &&
            Prefs.getMonochromeApps(context).contains(packageName) &&
            android.provider.Settings.canDrawOverlays(context)
        ) {
            context.startService(Intent(context, MonochromeOverlayService::class.java))
        }

        onLaunched()
    }
}
