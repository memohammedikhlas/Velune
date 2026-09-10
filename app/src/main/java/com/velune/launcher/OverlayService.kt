package com.velune.launcher

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.*
import android.view.*
import android.widget.TextView
import android.widget.Button
import android.widget.Toast

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()


        overlayView = LayoutInflater.from(this)
            .inflate(R.layout.overlay_timer, null)

        val messageText =
            overlayView.findViewById<TextView>(
                R.id.messageText
            )
        val btn1 =
            overlayView.findViewById<Button>(R.id.btn1Min)

        val btn5 =
            overlayView.findViewById<Button>(R.id.btn5Min)

        val btn10 =
            overlayView.findViewById<Button>(R.id.btn10Min)

        val btn15 =
            overlayView.findViewById<Button>(R.id.btn15Min)

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

        windowManager =
            getSystemService(WINDOW_SERVICE) as WindowManager

        windowManager.addView(overlayView, params)

        val countdownText =
            overlayView.findViewById<TextView>(
                R.id.countdownText
            )

        var seconds = 20

        Handler(Looper.getMainLooper())
            .post(object : Runnable {

                override fun run() {

                    countdownText.text =
                        seconds.toString()

                    seconds--

                    if (seconds >= 0) {

                        Handler(Looper.getMainLooper())
                            .postDelayed(this, 1000)

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
        btn1.setOnClickListener {

            stopSelf()

            Handler(Looper.getMainLooper())
                .postDelayed({

                    val homeIntent =
                        Intent(Intent.ACTION_MAIN)

                    homeIntent.addCategory(
                        Intent.CATEGORY_HOME
                    )

                    homeIntent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK

                    startActivity(homeIntent)

                }, 1 * 60 * 1000L)
        }

        btn5.setOnClickListener {

            stopSelf()

            Handler(Looper.getMainLooper())
                .postDelayed({

                    val homeIntent =
                        Intent(Intent.ACTION_MAIN)

                    homeIntent.addCategory(
                        Intent.CATEGORY_HOME
                    )

                    homeIntent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK

                    startActivity(homeIntent)

                }, 5 * 60 * 1000L)
        }

        btn10.setOnClickListener {

            stopSelf()

            Handler(Looper.getMainLooper())
                .postDelayed({

                    val homeIntent =
                        Intent(Intent.ACTION_MAIN)

                    homeIntent.addCategory(
                        Intent.CATEGORY_HOME
                    )

                    homeIntent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK

                    startActivity(homeIntent)

                }, 10 * 60 * 1000L)
        }

        btn15.setOnClickListener {

            stopSelf()

            Handler(Looper.getMainLooper())
                .postDelayed({

                    val homeIntent =
                        Intent(Intent.ACTION_MAIN)

                    homeIntent.addCategory(
                        Intent.CATEGORY_HOME
                    )

                    homeIntent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK

                    startActivity(homeIntent)

                }, 15 * 60 * 1000L)
        }
    }

    override fun onDestroy() {

        super.onDestroy()

        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }
}