package com.velune.launcher

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MotionEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
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
        setupSlideToBlock(appName)

        findViewById<TextView>(R.id.scheduleBtn).setOnClickListener {
            promptSetSchedule(appName)
        }

        renderOtherApps()
    }

    private fun setupSlideToBlock(appName: String) {
        val track = findViewById<FrameLayout>(R.id.slideTrack)
        val thumb = findViewById<TextView>(R.id.slideThumb)
        val label = findViewById<TextView>(R.id.slideTrackLabel)

        var downX = 0f
        var thumbStartX = 0f
        var maxTranslation = 0f
        var confirmed = false

        track.post {
            val marginPx = (4 * resources.displayMetrics.density).toInt()
            maxTranslation = (track.width - thumb.width - 2 * marginPx).toFloat().coerceAtLeast(1f)
        }

        thumb.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    thumbStartX = view.translationX
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val newX = (thumbStartX + (event.rawX - downX)).coerceIn(0f, maxTranslation)
                    view.translationX = newX
                    label.alpha = 1f - (newX / maxTranslation)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (view.translationX >= maxTranslation * 0.85f && !confirmed) {
                        confirmed = true
                        view.animate().translationX(maxTranslation).setDuration(100).start()
                        BlockManager.blockApp(this, targetPackage, DURATION_STEPS[selectedStep].first)
                        Toast.makeText(
                            this, "$appName blocked for ${DURATION_STEPS[selectedStep].second}", Toast.LENGTH_SHORT
                        ).show()
                        track.postDelayed({ finish() }, 150)
                    } else if (!confirmed) {
                        view.animate().translationX(0f).setDuration(200).start()
                        label.animate().alpha(1f).setDuration(200).start()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun promptSetSchedule(appName: String) {
        val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val existing = BlockManager.getSchedule(this, targetPackage)

        val dayToggles = dayLabels.mapIndexed { index, label ->
            ToggleButton(this).apply {
                textOn = label
                textOff = label
                textSize = 11f
                isChecked = existing?.days?.contains(index + 1) == true
            }
        }

        val dayRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            dayToggles.forEach { addView(it, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)) }
        }

        var startMinute = existing?.startMinute ?: (9 * 60)
        var endMinute = existing?.endMinute ?: (17 * 60)

        val startBtn = Button(this).apply { text = "Start: ${formatMinute(startMinute)}" }
        val endBtn = Button(this).apply { text = "End: ${formatMinute(endMinute)}" }

        startBtn.setOnClickListener {
            pickTime(startMinute) { picked -> startMinute = picked; startBtn.text = "Start: ${formatMinute(picked)}" }
        }
        endBtn.setOnClickListener {
            pickTime(endMinute) { picked -> endMinute = picked; endBtn.text = "End: ${formatMinute(picked)}" }
        }

        val timeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(startBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(endBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            addView(dayRow)
            addView(timeRow, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = pad })
        }

        AlertDialog.Builder(this)
            .setTitle("Blocking Schedule for $appName")
            .setMessage("Automatically block this app every week during the days/hours you choose.")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val selectedDays = dayToggles.mapIndexedNotNull { i, t -> if (t.isChecked) i + 1 else null }.toSet()
                if (selectedDays.isEmpty()) {
                    Toast.makeText(this, "Pick at least one day", Toast.LENGTH_SHORT).show()
                } else {
                    BlockManager.setSchedule(this, targetPackage, selectedDays, startMinute, endMinute)
                    Toast.makeText(this, "Schedule saved", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("Clear Schedule") { _, _ ->
                BlockManager.clearSchedule(this, targetPackage)
                Toast.makeText(this, "Schedule cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun pickTime(currentMinute: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(
            this,
            { _, hour, minute -> onPicked(hour * 60 + minute) },
            currentMinute / 60,
            currentMinute % 60,
            true
        ).show()
    }

    private fun formatMinute(minute: Int) = "%02d:%02d".format(minute / 60, minute % 60)

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
