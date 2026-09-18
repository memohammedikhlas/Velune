package com.velune.launcher

import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class BlockAppActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "package_name"

        // Slider position -> block duration in milliseconds
        private val DURATION_STEPS = listOf(
            1L * 60 * 60 * 1000 to "1 hour",
            6L * 60 * 60 * 1000 to "6 hours",
            12L * 60 * 60 * 1000 to "12 hours",
            1L * 24 * 60 * 60 * 1000 to "1 day",
            3L * 24 * 60 * 60 * 1000 to "3 days",
            7L * 24 * 60 * 60 * 1000 to "7 days",
            14L * 24 * 60 * 60 * 1000 to "14 days",
            21L * 24 * 60 * 60 * 1000 to "21 days",
            30L * 24 * 60 * 60 * 1000 to "30 days"
        )
    }

    private lateinit var targetPackage: String
    private var selectedStep = 3 // default: 1 day

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block_app)

        targetPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return finish()
        val appName = appLabel(this, targetPackage)

        findViewById<TextView>(R.id.blockTitle).text = "Block $appName"

        val weeklyMinutes = BlockManager.getWeeklyUsageMinutes(this, targetPackage)
        findViewById<TextView>(R.id.usageSummaryText).text =
            "You spent ${BlockManager.formatMinutes(weeklyMinutes)} on $appName in the last 7 days. " +
                    "Need a break but don't want to uninstall this app? Block it for up to 30 days."

        setupDurationSlider()

        findViewById<Button>(R.id.slideToBlockBtn).setOnClickListener {
            BlockManager.blockApp(this, targetPackage, DURATION_STEPS[selectedStep].first)
            Toast.makeText(
                this, "$appName blocked for ${DURATION_STEPS[selectedStep].second}", Toast.LENGTH_SHORT
            ).show()
            finish()
        }

        renderOtherApps()
    }

    private fun setupDurationSlider() {
        val durationValue = findViewById<TextView>(R.id.blockDurationValue)
        val slider = findViewById<SeekBar>(R.id.blockDurationSlider)

        slider.max = DURATION_STEPS.size - 1
        slider.progress = selectedStep
        durationValue.text = DURATION_STEPS[selectedStep].second

        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedStep = progress
                durationValue.text = DURATION_STEPS[progress].second
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.stay, R.anim.slide_down_out)
    }

    private fun renderOtherApps() {
        val container = findViewById<LinearLayout>(R.id.otherAppsContainer)
        container.removeAllViews()

        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val topOthers = BlockManager.getWeeklyUsageMinutesForAllApps(this)
            .filterKeys { it != targetPackage }
            .toList()
            .sortedByDescending { it.second }
            .take(10)

        for ((pkg, minutes) in topOthers) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                setBackgroundResource(R.drawable.card_background)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
            }

            val usageTextView = TextView(this).apply {
                text = "${appLabel(this@BlockAppActivity, pkg)} · ${BlockManager.formatMinutes(minutes)}"
                setTextColor(getColor(R.color.white))
                textSize = 14f
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val blockBtn = Button(
                ContextThemeWrapper(this, R.style.Widget_Velune_Button_PillSmall), null, 0
            ).apply {
                text = "Block"
                setOnClickListener {
                    startActivity(
                        Intent(this@BlockAppActivity, BlockAppActivity::class.java)
                            .putExtra(EXTRA_PACKAGE_NAME, pkg)
                    )
                    finish()
                }
            }

            row.addView(usageTextView)
            row.addView(blockBtn)
            container.addView(row)
        }
    }
}
