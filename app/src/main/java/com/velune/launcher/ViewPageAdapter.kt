package com.velune.launcher

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

    companion object {
        // Single shared clock ticker — reused across binds so re-binding the
        // Home page (e.g. on every onResume) never stacks up duplicate loops.
        private val clockHandler = Handler(Looper.getMainLooper())
        private var clockRunnable: Runnable? = null
    }

    private val layouts = listOf(
        R.layout.home_screen,
        R.layout.apps_screen
    )

    // Cache of installed launchable apps — package manager query is relatively
    // expensive, so we only rebuild it once per adapter lifetime instead of
    // every time the Apps page is bound (e.g. every onResume).
    private var cachedInstalledApps: List<Pair<String, String>>? = null

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

        // HOME SCREEN
        if (position == 0) {

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

                val blockedUntil = BlockManager.getBlockedUntil(context, packageName)

                if (blockedUntil != null && blockedUntil > System.currentTimeMillis()) {

                    val remainingMillis = blockedUntil - System.currentTimeMillis()
                    Toast.makeText(
                        context,
                        "${favoriteNames[pos]} is blocked for ${BlockManager.formatMinutes(remainingMillis / 60000L + 1)} more",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    val launchIntent =
                        context.packageManager
                            .getLaunchIntentForPackage(packageName)

                    if (launchIntent != null) {
                        MindfulLaunch.launch(context, launchIntent)
                    }
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

            // Cancel any previous clock loop before starting a new one, so
            // re-binding this page never results in multiple ticking timers.
            clockRunnable?.let { clockHandler.removeCallbacks(it) }

            clockRunnable = object : Runnable {

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

                    clockHandler.postDelayed(this, 1000)
                }
            }

            clockHandler.post(clockRunnable!!)
        }

        // APPS SCREEN
        if (position == 1) {

            val appsListView =
                holder.itemView.findViewById<ListView>(R.id.appsListView)

            val settingsGearBtn =
                holder.itemView.findViewById<android.widget.TextView>(R.id.settingsGearBtn)

            settingsGearBtn.setOnClickListener {
                context.startActivity(
                    Intent(context, SettingsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }

            val searchBar =
                holder.itemView.findViewById<EditText>(R.id.searchBar)

            val pm = context.packageManager

            // Only query the package manager once per adapter lifetime — this
            // list changes rarely, so there's no need to rebuild it on every bind.
            val uniqueApps = cachedInstalledApps ?: run {
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
                appList.distinctBy { it.second }
                    .sortedBy { it.first.lowercase() }
                    .also { cachedInstalledApps = it }
            }

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
                    "Block",
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
                                context.startActivity(
                                    Intent(context, BlockAppActivity::class.java)
                                        .putExtra(BlockAppActivity.EXTRA_PACKAGE_NAME, packageName)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }

                            3 -> {
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

                val blockedUntil = BlockManager.getBlockedUntil(context, packageName)

                if (blockedUntil != null && blockedUntil > System.currentTimeMillis()) {

                    val remainingMillis = blockedUntil - System.currentTimeMillis()
                    val remainingText = BlockManager.formatMinutes(remainingMillis / 60000L + 1)

                    Toast.makeText(
                        context,
                        "${filteredApps[i].first} is blocked for $remainingText more",
                        Toast.LENGTH_SHORT
                    ).show()

                } else if (launchIntent != null) {

                    MindfulLaunch.launch(context, launchIntent)
                }
            }
        }
    }
}
