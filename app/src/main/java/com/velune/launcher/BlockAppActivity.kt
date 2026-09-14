package com.velune.launcher

import android.content.Intent
import android.os.Bundle
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

    private lateinit var packageName_: String
    private var selectedStep = 3 // default: 1 day

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block_app)

        packageName_ = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: run {
            finish()
            return
        }

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName_, 0)
            ).toString()
        } catch (e: Exception) {
            packageName_
        }

        findViewById<TextView>(R.id.blockTitle).text = "Block $appName"

        val weeklyMinutes = BlockManager.getWeeklyUsageMinutes(this, packageName_)
        findViewById<TextView>(R.id.usageSummaryText).text =
            "You spent ${BlockManager.formatMinutes(weeklyMinutes)} on $appName in the last 7 days. " +
                    "Need a break but don't want to uninstall this app? Block it for up to 30 days."

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

        findViewById<Button>(R.id.slideToBlockBtn).setOnClickListener {
            val durationMillis = DURATION_STEPS[selectedStep].first
            BlockManager.blockApp(this, packageName_, durationMillis)
            Toast.makeText(
                this,
                "$appName blocked for ${DURATION_STEPS[selectedStep].second}",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }

        renderOtherApps()
    }

    private fun renderOtherApps() {
        val container = findViewById<LinearLayout>(R.id.otherAppsContainer)
        container.removeAllViews()

        val usageMap = BlockManager.getWeeklyUsageMinutesForAllApps(this)
        val pm = packageManager

        val topOthers = usageMap
            .filterKeys { it != packageName_ }
            .toList()
            .sortedByDescending { it.second }
            .take(10)

        for ((pkg, minutes) in topOthers) {
            val label = try {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            } catch (e: Exception) {
                continue
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(20, 20, 20, 20)
                setBackgroundResource(R.drawable.card_background)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.topMargin = 8
                layoutParams = params
            }

            val usageTextView = TextView(this).apply {
                text = "$label · ${BlockManager.formatMinutes(minutes)}"
                setTextColor(getColor(R.color.white))
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val blockBtn = Button(this).apply {
                text = "Block"
                textSize = 12f
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
