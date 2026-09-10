package com.velune.launcher

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.view.animation.AnimationUtils
import java.util.*

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.splash_screen)

        val logo = findViewById<android.widget.ImageView>(R.id.logoImage)

        val animation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)

        animation.duration = 1500

        logo.startAnimation(animation)

        Handler(Looper.getMainLooper()).postDelayed({

            startActivity(Intent(this, MainActivity::class.java))
            finish()

        }, 2500)

    }
}