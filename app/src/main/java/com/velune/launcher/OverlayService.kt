package com.velune.launcher

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.*
import android.view.*
import android.widget.TextView
import android.widget.Button

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val SESSION_END_COUNTDOWN_SECONDS = 20
    }

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()

        overlayView = LayoutInflater.from(this)
            .inflate(R.layout.overlay_timer, null)

        val messageText = overlayView.findViewById<TextView>(R.id.messageText)
        val btn1 = overlayView.findViewById<Button>(R.id.btn1Min)
        val btn5 = overlayView.findViewById<Button>(R.id.btn5Min)
        val btn10 = overlayView.findViewById<Button>(R.id.btn10Min)
        val btn15 = overlayView.findViewById<Button>(R.id.btn15Min)

        btn1.visibility = View.GONE
        btn5.visibility = View.GONE
        btn10.visibility = View.GONE
        btn15.visibility = View.GONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            0,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(overlayView, params)

        val countdownText = overlayView.findViewById<TextView>(R.id.countdownText)
        var seconds = SESSION_END_COUNTDOWN_SECONDS

        handler.post(object : Runnable {
            override fun run() {
                countdownText.text = seconds.toString()
                seconds--

                if (seconds >= 0) {
                    handler.postDelayed(this, 1000)
                } else {
                    countdownText.visibility = View.GONE
                    messageText.text = "Continue Session?"

                    btn1.visibility = View.VISIBLE
                    btn5.visibility = View.VISIBLE
                    btn10.visibility = View.VISIBLE
                    btn15.visibility = View.VISIBLE
                }
            }
        })

        // Extending the session: dismiss the overlay so the user can keep using the app,
        // then bring this same overlay back after the chosen duration to check in again.
        btn1.setOnClickListener { extendSession(1) }
        btn5.setOnClickListener { extendSession(5) }
        btn10.setOnClickListener { extendSession(10) }
        btn15.setOnClickListener { extendSession(15) }
    }

    private fun extendSession(minutes: Int) {
        val delayMillis = minutes * 60 * 1000L
        val appContext = applicationContext

        handler.postDelayed({
            appContext.startService(Intent(appContext, OverlayService::class.java))
        }, delayMillis)

        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)

        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }
}
