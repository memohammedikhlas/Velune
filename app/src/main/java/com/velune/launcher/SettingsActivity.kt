package com.velune.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private data class AppEntry(val name: String, val packageName: String)

    private lateinit var allApps: List<AppEntry>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        allApps = loadInstalledApps()

        setupActiveBlocksSection()
        setupMindfulLaunchDelaySection()
        setupHiddenAppsSection()
        setupNotificationFilterSection()
        setupMonochromeSection()
        setupAppearanceSection()
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.stay, R.anim.slide_down_out)
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun loadInstalledApps(): List<AppEntry> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)

        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName }
            .distinct()
            .map { AppEntry(appLabel(this, it), it) }
            .sortedBy { it.name.lowercase() }
    }

    // ---- Active Blocks ----

    private fun setupActiveBlocksSection() {
        val listView = findViewById<ListView>(R.id.activeBlocksListView)
        val emptyText = findViewById<TextView>(R.id.noActiveBlocksText)

        fun render() {
            val active = BlockManager.getAllActiveBlocks(this)
            val entries = allApps.filter { active.containsKey(it.packageName) }

            if (entries.isEmpty()) {
                listView.visibility = View.GONE
                emptyText.visibility = View.VISIBLE
                return
            }

            listView.visibility = View.VISIBLE
            emptyText.visibility = View.GONE

            val labels = entries.map { app ->
                val until = active[app.packageName] ?: 0L
                val remainingMin = (until - System.currentTimeMillis()) / 60000L + 1
                "${app.name} — blocked ${BlockManager.formatMinutes(remainingMin)} more"
            }

            listView.adapter = appNameAdapter(this, labels)

            listView.setOnItemClickListener { _, _, position, _ ->
                BlockManager.unblockApp(this, entries[position].packageName)
                toast("${entries[position].name} unblocked")
                render()
            }
        }

        render()
    }

    // ---- Mindful Launch Delay ----

    private fun setupMindfulLaunchDelaySection() {
        val switch = findViewById<Switch>(R.id.launchDelaySwitch)
        val secondsField = findViewById<EditText>(R.id.launchDelaySeconds)
        val saveBtn = findViewById<Button>(R.id.saveDelayBtn)
        switch.thumbTintList = android.content.res.ColorStateList.valueOf(accentColor(this))

        switch.isChecked = Prefs.isLaunchDelayEnabled(this)
        secondsField.setText(Prefs.getLaunchDelaySeconds(this).toString())

        switch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setLaunchDelayEnabled(this, isChecked)
        }

        saveBtn.setOnClickListener {
            val seconds = secondsField.text.toString().trim().toIntOrNull()
            if (seconds == null || seconds <= 0) {
                toast("Enter a valid number of seconds")
            } else {
                Prefs.setLaunchDelaySeconds(this, seconds)
                toast("Saved")
            }
        }
    }

    // ---- Hidden Apps ----

    private fun setupHiddenAppsSection() {
        val listView = findViewById<ListView>(R.id.hiddenAppsListView)
        val emptyText = findViewById<TextView>(R.id.noHiddenAppsText)

        fun render() {
            val hidden = Prefs.getHiddenApps(this)
            val hiddenEntries = allApps.filter { hidden.contains(it.packageName) }

            if (hiddenEntries.isEmpty()) {
                listView.visibility = View.GONE
                emptyText.visibility = View.VISIBLE
                return
            }

            listView.visibility = View.VISIBLE
            emptyText.visibility = View.GONE

            listView.adapter = appNameAdapter(this, hiddenEntries.map { it.name })

            listView.setOnItemClickListener { _, _, position, _ ->
                Prefs.setAppHidden(this, hiddenEntries[position].packageName, false)
                toast("${hiddenEntries[position].name} unhidden")
                render()
            }
        }

        render()
    }

    // ---- Notification Filter ----

    private fun setupNotificationFilterSection() {
        val switch = findViewById<Switch>(R.id.notificationFilterSwitch)
        val statusText = findViewById<TextView>(R.id.notifAccessStatusText)
        val listView = findViewById<ListView>(R.id.filteredNotifsListView)
        val emptyText = findViewById<TextView>(R.id.noFilteredNotifsText)
        val clearBtn = findViewById<Button>(R.id.clearFilteredNotifsBtn)
        switch.thumbTintList = android.content.res.ColorStateList.valueOf(accentColor(this))

        fun renderStatus() {
            val granted = isNotificationAccessGranted(this)
            statusText.text = if (granted) "Notification access granted ✓" else "Notification access not granted — tap to grant"
        }

        fun renderList() {
            val entries = Prefs.getFilteredNotifications(this)

            if (entries.isEmpty()) {
                listView.visibility = View.GONE
                emptyText.visibility = View.VISIBLE
                return
            }

            listView.visibility = View.VISIBLE
            emptyText.visibility = View.GONE

            val timeFormat = SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault())
            val labels = entries.map { entry ->
                val parts = entry.split("|")
                if (parts.size == 4) {
                    val time = timeFormat.format(parts[0].toLongOrNull() ?: 0L)
                    val app = appLabel(this, parts[1])
                    "$app: ${parts[2]} — ${parts[3]} ($time)"
                } else entry
            }

            listView.adapter = appNameAdapter(this, labels)
        }

        renderStatus()
        renderList()

        switch.isChecked = Prefs.isNotificationFilterEnabled(this)
        switch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setNotificationFilterEnabled(this, isChecked)
            if (isChecked && !isNotificationAccessGranted(this)) {
                AlertDialog.Builder(this)
                    .setTitle("Notification Access Required")
                    .setMessage("Velune needs notification access to filter notifications from blocked apps.")
                    .setPositiveButton("Grant") { _, _ ->
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                    .setNegativeButton("Not Now", null)
                    .show()
            }
        }

        statusText.setOnClickListener {
            if (!isNotificationAccessGranted(this)) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        clearBtn.setOnClickListener {
            Prefs.clearFilteredNotifications(this)
            renderList()
        }
    }

    override fun onResume() {
        super.onResume()
        // Permission may have just been granted/revoked in system settings — refresh the status line.
        findViewById<TextView>(R.id.notifAccessStatusText).text =
            if (isNotificationAccessGranted(this)) "Notification access granted ✓" else "Notification access not granted — tap to grant"
    }

    // ---- Monochrome Mode ----

    private fun setupMonochromeSection() {
        val listView = findViewById<ListView>(R.id.monochromeAppsListView)
        val searchBar = findViewById<EditText>(R.id.monochromeSearchBar)

        fun render(filter: String) {
            val monochromeApps = Prefs.getMonochromeApps(this)
            val visible = allApps.filter { it.name.contains(filter, ignoreCase = true) }
            val labels = visible.map { if (monochromeApps.contains(it.packageName)) "✅ ${it.name}" else it.name }

            listView.adapter = appNameAdapter(this, labels)

            listView.setOnItemClickListener { _, _, position, _ ->
                val app = visible[position]
                val isOn = Prefs.getMonochromeApps(this).contains(app.packageName)
                Prefs.setAppMonochrome(this, app.packageName, !isOn)
                render(searchBar.text.toString())
            }
        }

        render("")

        searchBar.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                render(s.toString())
            }
        })
    }

    // ---- Appearance ----

    private fun setupAppearanceSection() {
        val swatchContainer = findViewById<android.widget.LinearLayout>(R.id.accentSwatchContainer)
        val fontContainer = findViewById<android.widget.LinearLayout>(R.id.fontOptionContainer)
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        swatchContainer.removeAllViews()
        fontContainer.removeAllViews()

        val currentAccent = Prefs.getAccentColorName(this)
        ACCENT_COLOR_OPTIONS.forEach { (name, hex) ->
            val swatch = View(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(dp(36), dp(36)).apply {
                    marginEnd = dp(12)
                }
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(android.graphics.Color.parseColor(hex))
                    if (name == currentAccent) setStroke(dp(3), getColor(R.color.white))
                }
                setOnClickListener {
                    Prefs.setAccentColorName(this@SettingsActivity, name)
                    setupAppearanceSection()
                }
            }
            swatchContainer.addView(swatch)
        }

        val currentFont = Prefs.getFontFamily(this)
        FONT_OPTIONS.forEach { (family, label) ->
            val row = TextView(this).apply {
                text = if (family == currentFont) "✅ $label" else label
                setTextColor(getColor(R.color.white))
                typeface = android.graphics.Typeface.create(family, android.graphics.Typeface.NORMAL)
                textSize = 15f
                setPadding(0, dp(10), 0, dp(10))
                setOnClickListener {
                    Prefs.setFontFamily(this@SettingsActivity, family)
                    setupAppearanceSection()
                }
            }
            fontContainer.addView(row)
        }
    }
}
