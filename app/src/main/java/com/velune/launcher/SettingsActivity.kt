package com.velune.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.view.View
import android.widget.Button
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

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
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.stay, R.anim.slide_down_out)
    }

    private fun loadInstalledApps(): List<AppEntry> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { AppEntry(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .distinctBy { it.packageName }
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

            listView.adapter = ArrayAdapter(this, R.layout.list_item_app_name, labels)

            listView.setOnItemClickListener { _, _, position, _ ->
                BlockManager.unblockApp(this, entries[position].packageName)
                Toast.makeText(this, "${entries[position].name} unblocked", Toast.LENGTH_SHORT).show()
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

        switch.isChecked = Prefs.isLaunchDelayEnabled(this)
        secondsField.setText(Prefs.getLaunchDelaySeconds(this).toString())

        switch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setLaunchDelayEnabled(this, isChecked)
        }

        saveBtn.setOnClickListener {
            val seconds = secondsField.text.toString().trim().toIntOrNull()
            if (seconds == null || seconds <= 0) {
                Toast.makeText(this, "Enter a valid number of seconds", Toast.LENGTH_SHORT).show()
            } else {
                Prefs.setLaunchDelaySeconds(this, seconds)
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
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

            listView.adapter = ArrayAdapter(
                this,
                R.layout.list_item_app_name,
                hiddenEntries.map { it.name }
            )

            listView.setOnItemClickListener { _, _, position, _ ->
                Prefs.setAppHidden(this, hiddenEntries[position].packageName, false)
                Toast.makeText(this, "${hiddenEntries[position].name} unhidden", Toast.LENGTH_SHORT).show()
                render()
            }
        }

        render()
    }

    // ---- Notification Filter (scaffold) ----

    private fun setupNotificationFilterSection() {
        val switch = findViewById<Switch>(R.id.notificationFilterSwitch)
        switch.isChecked = Prefs.isNotificationFilterEnabled(this)
        switch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setNotificationFilterEnabled(this, isChecked)
        }
    }
}
