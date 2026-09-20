package com.velune.launcher

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

class OnboardingActivity : AppCompatActivity() {

    private data class Page(val emoji: String, val title: String, val description: String)

    private val pages = listOf(
        Page("👋", "Welcome to Velune", "A calmer, more intentional way to use your phone."),
        Page("🔤", "No Icons, No Cravings", "Just app names as text — no colorful icons pulling you in. Stay focused and present."),
        Page("🛡️", "Take Control", "Block distracting apps for hours or days, set weekly schedules, and add a mindful pause before anything opens."),
        Page("📁", "Stay Organized", "Rename apps, group them into folders, and keep only what matters on your Home screen."),
        Page("🔓", "One Last Thing", "Velune needs a couple of permissions to work properly — you'll be asked for them next.")
    )

    private lateinit var pager: ViewPager2
    private lateinit var dots: List<ImageView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        pager = findViewById(R.id.onboardingPager)
        pager.adapter = PageAdapter()

        setupDots()
        updateDots(0)

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                findViewById<android.widget.Button>(R.id.nextBtn).text =
                    if (position == pages.lastIndex) "GET STARTED" else "NEXT"
            }
        })

        findViewById<TextView>(R.id.skipBtn).setOnClickListener { finishOnboarding() }

        findViewById<android.widget.Button>(R.id.nextBtn).setOnClickListener {
            if (pager.currentItem == pages.lastIndex) {
                finishOnboarding()
            } else {
                pager.currentItem += 1
            }
        }
    }

    private fun setupDots() {
        val container = findViewById<android.widget.LinearLayout>(R.id.dotsContainer)
        val density = resources.displayMetrics.density

        dots = pages.indices.map {
            ImageView(this).apply {
                setImageResource(R.drawable.dot_inactive)
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    (8 * density).toInt(), (8 * density).toInt()
                ).apply { marginStart = (4 * density).toInt(); marginEnd = (4 * density).toInt() }
            }
        }
        dots.forEach { container.addView(it) }
    }

    private fun updateDots(activeIndex: Int) {
        dots.forEachIndexed { i, dot ->
            dot.setImageResource(if (i == activeIndex) R.drawable.dot_active else R.drawable.dot_inactive)
        }
    }

    private fun finishOnboarding() {
        Prefs.setOnboardingComplete(this)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private inner class PageAdapter : RecyclerView.Adapter<PageAdapter.Holder>() {
        inner class Holder(view: View) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.onboarding_page, parent, false)
            return Holder(view)
        }

        override fun getItemCount() = pages.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val page = pages[position]
            holder.itemView.findViewById<TextView>(R.id.pageEmoji).text = page.emoji
            holder.itemView.findViewById<TextView>(R.id.pageTitle).text = page.title
            holder.itemView.findViewById<TextView>(R.id.pageDescription).text = page.description
        }
    }
}
