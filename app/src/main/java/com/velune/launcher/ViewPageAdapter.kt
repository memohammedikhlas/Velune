package com.velune.launcher

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.net.Uri
import android.widget.Toast
import android.content.pm.PackageManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*
import androidx.recyclerview.widget.RecyclerView

class ViewPageAdapter(
    private val context: Context
) : RecyclerView.Adapter<ViewPageAdapter.ViewHolder>() {

    private val layouts = listOf(
        R.layout.stats_screen,
        R.layout.home_screen,
        R.layout.apps_screen
    )

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val timeText: TextView? =
            view.findViewById(R.id.timeText)

        val dateText: TextView? =
            view.findViewById(R.id.dateText)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {


        val view = LayoutInflater.from(parent.context)
            .inflate(layouts[viewType], parent, false)

        return ViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemCount(): Int {
        return layouts.size
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        if (position == 0) {



            val minutesText =
                holder.itemView.findViewById<TextView>(
                    R.id.statsMinutes
                )

            val sessionsText =
                holder.itemView.findViewById<TextView>(
                    R.id.statsSessions
                )

            val prefs =
                context.getSharedPreferences(
                    "focus_stats",
                    Context.MODE_PRIVATE
                )

            minutesText.text =
                "Focus Time: ${
                    prefs.getInt(
                        "total_minutes",
                        0
                    )
                } min"

            sessionsText.text =
                "Sessions: ${
                    prefs.getInt(
                        "sessions",
                        0
                    )
                }"

            val mostUsedTimer =
                holder.itemView.findViewById<TextView>(
                    R.id.mostUsedTimer
                )

            val timer1 = prefs.getInt("timer_1", 0)
            val timer5 = prefs.getInt("timer_5", 0)
            val timer10 = prefs.getInt("timer_10", 0)
            val timer15 = prefs.getInt("timer_15", 0)

            val maxCount = maxOf(timer1, timer5, timer10, timer15)

            val mostUsed =
                if (maxCount == 0) {
                    "No Data"
                } else if (maxCount == timer15) {
                    "15 Minutes"
                } else if (maxCount == timer10) {
                    "10 Minutes"
                } else if (maxCount == timer5) {
                    "5 Minutes"
                } else {
                    "1 Minute"
                }

            mostUsedTimer.text = "Most Used Timer: $mostUsed"

            val streakText =
                holder.itemView.findViewById<TextView>(
                    R.id.streakText
                )

            streakText.text =
                "Current Streak: ${
                    prefs.getInt(
                        "streak",
                        0
                    )
                } Days"

        }

        // HOME SCREEN
        if (position == 1) {

            val logoText =
                holder.itemView.findViewById<TextView>(
                    R.id.logoText
                )

            val favoritesList =
                holder.itemView.findViewById<ListView>(
                    R.id.favoritesList
                )

            val prefs =
                context.getSharedPreferences(
                    "favorites",
                    Context.MODE_PRIVATE
                )

            val favorites =
                prefs.getStringSet(
                    "apps",
                    mutableSetOf()
                )?.toList() ?: emptyList()

            val favoriteNames =
                favorites.map { packageName ->

                    try {
                        val appInfo =
                            context.packageManager
                                .getApplicationInfo(packageName, 0)

                        context.packageManager
                            .getApplicationLabel(appInfo)
                            .toString()

                    } catch (e: Exception) {
                        packageName
                    }
                }

            val favoritesAdapter =
                ArrayAdapter(
                    context,
                    android.R.layout.simple_list_item_1,
                    favoriteNames
                )

            favoritesList.adapter = favoritesAdapter

            favoritesList.setOnItemClickListener { _, _, pos, _ ->

                val packageName = favorites[pos]

                val launchIntent =
                    context.packageManager
                        .getLaunchIntentForPackage(packageName)

                launchIntent?.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )

                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                }
            }

            logoText.setOnLongClickListener {

                val hiddenPrefs =
                    context.getSharedPreferences(
                        "hidden_apps",
                        Context.MODE_PRIVATE
                    )

                val hiddenApps =
                    hiddenPrefs.getStringSet(
                        "apps",
                        mutableSetOf()
                    )?.toList()
                        ?: emptyList()

                val pm = context.packageManager

                val hiddenAppNames = hiddenApps.map { packageName ->

                    try {
                        pm.getApplicationLabel(
                            pm.getApplicationInfo(packageName, 0)
                        ).toString()

                    } catch (e: Exception) {
                        packageName
                    }
                }

                if (hiddenApps.isEmpty()) {

                    Toast.makeText(
                        context,
                        "No Hidden Apps",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnLongClickListener true
                }

                android.app.AlertDialog.Builder(context)
                    .setTitle("Hidden Apps")
                    .setItems(hiddenAppNames.toTypedArray()) { _, which ->

                        val packageName =
                            hiddenApps[which]

                        val updatedApps =
                            hiddenApps.toMutableSet()

                        updatedApps.remove(packageName)

                        hiddenPrefs.edit()
                            .putStringSet(
                                "apps",
                                updatedApps
                            )
                            .apply()

                        Toast.makeText(
                            context,
                            "App Restored",
                            Toast.LENGTH_SHORT
                        ).show()

                        notifyDataSetChanged()
                    }
                    .show()

                true
            }

            favoritesList.setOnItemLongClickListener { _, _, pos, _ ->

                val packageName = favorites[pos]

                val options = arrayOf(
                    "Remove from Favorites",
                    "App Info"
                )

                android.app.AlertDialog.Builder(context)
                    .setTitle(favoriteNames[pos])
                    .setItems(options) { _, which ->

                        when (which) {

                            0 -> {

                                val prefs =
                                    context.getSharedPreferences(
                                        "favorites",
                                        Context.MODE_PRIVATE
                                    )

                                val favorites =
                                    prefs.getStringSet(
                                        "apps",
                                        mutableSetOf()
                                    )?.toMutableSet()
                                        ?: mutableSetOf()

                                favorites.remove(packageName)

                                prefs.edit()
                                    .putStringSet(
                                        "apps",
                                        favorites
                                    )
                                    .apply()

                                Toast.makeText(
                                    context,
                                    "Removed from Favorites",
                                    Toast.LENGTH_SHORT
                                ).show()

                                notifyItemChanged(0)
                            }

                            1 -> {

                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                )

                                intent.data =
                                    Uri.parse("package:$packageName")

                                intent.addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                )

                                context.startActivity(intent)
                            }
                        }
                    }
                    .show()

                true
            }

                    val handler =
                Handler(Looper.getMainLooper())

            handler.post(object : Runnable {

                override fun run() {

                    val currentTime =
                        Calendar.getInstance()

                    val timeFormat =
                        SimpleDateFormat(
                            "hh:mm a",
                            Locale.getDefault()
                        )

                    val dateFormat =
                        SimpleDateFormat(
                            "EEEE, dd MMM",
                            Locale.getDefault()
                        )

                    holder.timeText?.text =
                        timeFormat.format(currentTime.time)

                    holder.dateText?.text =
                        dateFormat.format(currentTime.time)

                    handler.postDelayed(this, 1000)
                }
            })
        }

        // APPS SCREEN
        if (position == 2) {

            val appsListView =
                holder.itemView.findViewById<ListView>(R.id.appsListView)

            val searchBar =
                holder.itemView.findViewById<EditText>(R.id.searchBar)

            val pm = context.packageManager

            val intent = Intent(Intent.ACTION_MAIN, null)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)

            val apps = pm.queryIntentActivities(intent, 0)

            val appList = mutableListOf<Pair<String, String>>()

            for (app in apps) {

                val appName =
                    app.loadLabel(pm).toString()

                val packageName =
                    app.activityInfo.packageName

                appList.add(
                    Pair(appName, packageName)
                )
            }

            // remove duplicate apps
            val uniqueApps =
                appList.distinctBy { it.second }
                    .sortedBy { it.first.lowercase() }

            val hiddenPrefs =
                context.getSharedPreferences(
                    "hidden_apps",
                    Context.MODE_PRIVATE
                )

            val hiddenApps =
                hiddenPrefs.getStringSet(
                    "apps",
                    mutableSetOf()
                ) ?: emptySet()

            var filteredApps =
                uniqueApps.filter {
                    !hiddenApps.contains(it.second)
                }

            val adapter = ArrayAdapter(
                context,
                android.R.layout.simple_list_item_1,
                filteredApps.map { it.first }
            )

            appsListView.adapter = adapter

            // SEARCH
            searchBar.addTextChangedListener(object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    filteredApps =
                        uniqueApps.filter {

                            !hiddenApps.contains(it.second) &&

                                    it.first.contains(
                                        s.toString(),
                                        ignoreCase = true
                                    )
                        }

                    val newAdapter = ArrayAdapter(
                        context,
                        android.R.layout.simple_list_item_1,
                        filteredApps.map { it.first }
                    )

                    appsListView.adapter = newAdapter
                }

                override fun afterTextChanged(s: Editable?) {
                }
            })

            // APP CLICK

            appsListView.setOnItemLongClickListener { _, _, i, _ ->

                val appName = filteredApps[i].first
                val packageName = filteredApps[i].second

                val options = arrayOf(
                    "Add to Favorites",
                    "Hide App",
                    "App Info"
                )

                android.app.AlertDialog.Builder(context)
                    .setTitle(appName)
                    .setItems(options) { _, which ->

                        when (which) {

                            0 -> {

                                val prefs =
                                    context.getSharedPreferences(
                                        "favorites",
                                        Context.MODE_PRIVATE
                                    )

                                val favorites =
                                    prefs.getStringSet(
                                        "apps",
                                        mutableSetOf()
                                    )?.toMutableSet()
                                        ?: mutableSetOf()

                                favorites.add(packageName)

                                prefs.edit()
                                    .putStringSet(
                                        "apps",
                                        favorites
                                    )
                                    .apply()

                                Toast.makeText(
                                    context,
                                    "$appName added to Favorites",
                                    Toast.LENGTH_SHORT
                                ).show()

                                notifyDataSetChanged()
                            }

                            1 -> {

                                val hiddenPrefs =
                                    context.getSharedPreferences(
                                        "hidden_apps",
                                        Context.MODE_PRIVATE
                                    )

                                val hiddenApps =
                                    hiddenPrefs.getStringSet(
                                        "apps",
                                        mutableSetOf()
                                    )?.toMutableSet()
                                        ?: mutableSetOf()

                                hiddenApps.add(packageName)

                                hiddenPrefs.edit()
                                    .putStringSet(
                                        "apps",
                                        hiddenApps
                                    )
                                    .apply()

                                Toast.makeText(
                                    context,
                                    "$appName hidden",
                                    Toast.LENGTH_SHORT
                                ).show()

                                notifyDataSetChanged()
                            }

                            2 -> {
                                // existing App Info code
                            }
                        }
                    }
                    .show()

                true
            }

            appsListView.setOnItemClickListener { _, _, i, _ ->

                val packageName =
                    filteredApps[i].second
                val launchIntent =
                    pm.getLaunchIntentForPackage(packageName)

                val distractingApps = listOf(
                    "com.instagram.android",
                    "com.whatsapp",
                    "com.snapchat.android",
                    "com.twitter.android",
                    "org.telegram.messenger",
                    "com.google.android.youtube"
                )

                if (distractingApps.contains(packageName)) {

                    val dialog = Dialog(context)
                    dialog.setContentView(R.layout.focus_delay)
                    dialog.show()

                    dialog.window?.attributes?.windowAnimations =
                        android.R.style.Animation_Dialog

                    val btn1 =
                        dialog.findViewById<android.widget.Button>(R.id.btn1)

                    val btn5 =
                        dialog.findViewById<android.widget.Button>(R.id.btn5)

                    val btn10 =
                        dialog.findViewById<android.widget.Button>(R.id.btn10)

                    val btn15 =
                        dialog.findViewById<android.widget.Button>(R.id.btn15)

                    val startBtn =
                        dialog.findViewById<android.widget.Button>(R.id.startBtn)

                    val customMinutes =
                        dialog.findViewById<android.widget.EditText>(R.id.customMinutes)

                    var selectedMinutes = 1



                    fun openApp() {

                        val statsPrefs =
                            context.getSharedPreferences(
                                "focus_stats",
                                Context.MODE_PRIVATE
                            )

                        val today =
                            SimpleDateFormat(
                                "yyyyMMdd",
                                Locale.getDefault()
                            ).format(Date())

                        val savedDate =
                            statsPrefs.getString(
                                "stats_date",
                                ""
                            )

                        if (savedDate != today) {

                            statsPrefs.edit()
                                .putString(
                                    "stats_date",
                                    today
                                )
                                .putInt(
                                    "total_minutes",
                                    0
                                )
                                .putInt(
                                    "sessions",
                                    0
                                )
                                .apply()
                        }

                        statsPrefs.edit()
                            .putInt(
                                "sessions",
                                statsPrefs.getInt("sessions", 0) + 1
                            )
                            .putInt(
                                "total_minutes",
                                statsPrefs.getInt("total_minutes", 0) + selectedMinutes
                            )
                            .apply()

                        val streakPrefs =
                            context.getSharedPreferences(
                                "focus_stats",
                                Context.MODE_PRIVATE
                            )

                        val lastDay =
                            streakPrefs.getString(
                                "last_focus_day",
                                ""
                            )

                        var streak =
                            streakPrefs.getInt(
                                "streak",
                                0
                            )

                        if (lastDay != today) {

                            streak++

                            streakPrefs.edit()
                                .putString(
                                    "last_focus_day",
                                    today
                                )
                                .putInt(
                                    "streak",
                                    streak
                                )
                                .apply()
                        }

                        launchIntent!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                        context.startActivity(launchIntent)

                        Handler(Looper.getMainLooper()).postDelayed({

                            context.startService(
                                Intent(
                                    context,
                                    OverlayService::class.java
                                )
                            )

                        }, selectedMinutes * 60 * 1000L)

                        dialog.dismiss()
                    }

                    btn1.setOnClickListener {

                        val prefs =
                            context.getSharedPreferences(
                                "focus_stats",
                                Context.MODE_PRIVATE
                            )

                        val count =
                            prefs.getInt("timer_1", 0)

                        prefs.edit()
                            .putInt("timer_1", count + 1)
                            .apply()

                        selectedMinutes = 1

                        openApp()
                    }

                    btn5.setOnClickListener {

                        val prefs =
                            context.getSharedPreferences(
                                "focus_stats",
                                Context.MODE_PRIVATE
                            )

                        val count =
                            prefs.getInt("timer_5", 0)

                        prefs.edit()
                            .putInt("timer_5", count + 1)
                            .apply()

                        selectedMinutes = 5

                        openApp()
                    }

                    btn10.setOnClickListener {

                        val prefs =
                            context.getSharedPreferences(
                                "focus_stats",
                                Context.MODE_PRIVATE
                            )

                        val count =
                            prefs.getInt("timer_10", 0)

                        prefs.edit()
                            .putInt("timer_10", count + 1)
                            .apply()

                        selectedMinutes = 10

                        openApp()
                    }

                    btn15.setOnClickListener {

                        val prefs =
                            context.getSharedPreferences(
                                "focus_stats",
                                Context.MODE_PRIVATE
                            )

                        val count =
                            prefs.getInt("timer_15", 0)

                        prefs.edit()
                            .putInt("timer_15", count + 1)
                            .apply()

                        selectedMinutes = 15

                        openApp()
                    }

                    startBtn.setOnClickListener {

                        val entered = customMinutes.text.toString()

                        if (entered.isNotEmpty()) {
                            selectedMinutes = entered.toInt()
                        }

                        openApp()
                    }

                } else {

                    launchIntent!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent!!)
                }
                }
            }
        }
    }
