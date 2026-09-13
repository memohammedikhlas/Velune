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
            context.startActivity(intent)
            onLaunched()
            return
        }

        var remaining = Prefs.getLaunchDelaySeconds(context)
        if (remaining <= 0) {
            context.startActivity(intent)
            onLaunched()
            return
        }

        val messageView = TextView(context).apply {
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(48, 64, 48, 64)
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
                    context.startActivity(intent)
                    onLaunched()
                }
            }
        }
        handler.postDelayed(tick, 1000)
    }
}
