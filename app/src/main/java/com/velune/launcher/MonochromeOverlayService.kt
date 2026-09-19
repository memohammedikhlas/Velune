package com.velune.launcher

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout

/**
 * NOTE: Android doesn't let a normal app apply a true per-app color filter —
 * that needs a system permission (WRITE_SECURE_SETTINGS) only grantable via
 * ADB. This approximates "less stimulating" with a translucent grey overlay
 * instead of real desaturation. Started right after launching a monochrome
 * app (see MindfulLaunch), stopped when the user comes back to Velune.
 */
class MonochromeOverlayService : Service() {

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val view = FrameLayout(this).apply {
            setBackgroundColor(0x66555555) // translucent grey tint
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

        wm.addView(view, params)
        overlayView = view
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let { windowManager?.removeView(it) }
        overlayView = null
    }
}
