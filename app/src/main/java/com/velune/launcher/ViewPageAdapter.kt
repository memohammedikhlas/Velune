package com.velune.launcher

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ViewPageAdapter(
    private val context: Context
) : RecyclerView.Adapter<ViewPageAdapter.ViewHolder>() {

    companion object {
        // Single shared clock ticker — reused across binds so re-binding the
        // Home page (e.g. on every onResume) never stacks up duplicate loops.
        private val clockHandler = Handler(Looper.getMainLooper())
        private var clockRunnable: Runnable? = null

        private const val FOLDER_PREFIX = "folder:"
    }

    private val layouts = listOf(R.layout.home_screen, R.layout.apps_screen)

    // Installed launchable apps, queried once per adapter lifetime — the
    // package manager query is relatively expensive and this list rarely
    // changes, so there's no need to rebuild it on every Apps-page bind.
    private var cachedInstalledApps: List<Pair<String, String>>? = null

    // Live reference to the Apps-screen list + its current search query, so
    // actions triggered from a long-press menu (favorite/hide/rename/folder)
    // can refresh just this list in place — preserving scroll position —
    // instead of forcing a full page rebind that jumps back to the top.
    private var appsListViewRef: ListView? = null
    private var appsSearchQuery: String = ""
    private var appsDisplayItems: List<Pair<String, String>> = emptyList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeText: TextView? = view.findViewById(R.id.timeText)
        val dateText: TextView? = view.findViewById(R.id.dateText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layouts[viewType], parent, false)
        return ViewHolder(view)
    }

    override fun getItemViewType(position: Int) = position

    override fun getItemCount() = layouts.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (position) {
            0 -> bindHomeScreen(holder)
            1 -> bindAppsScreen(holder)
        }
    }

    private fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    // ---------------------------------------------------------------------
    // HOME SCREEN
    // ---------------------------------------------------------------------

    private fun bindHomeScreen(holder: ViewHolder) {
        val favoritesList = holder.itemView.findViewById<ListView>(R.id.favoritesList)

        val favorites = Prefs.getFavorites(context)
        val favoriteNames = favorites.map { appLabel(context, it) }

        favoritesList.adapter = appNameAdapter(context, favoriteNames)

        holder.timeText?.typeface = android.graphics.Typeface.create(Prefs.getFontFamily(context), android.graphics.Typeface.NORMAL)
        holder.timeText?.setOnClickListener {
            try {
                context.startActivity(
                    Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
                toast("No clock app found")
            }
        }

        favoritesList.setOnItemClickListener { _, _, pos, _ ->
            launchOrShowBlocked(favorites[pos], favoriteNames[pos])
        }

        favoritesList.setOnItemLongClickListener { _, _, pos, _ ->
            val packageName = favorites[pos]
            showPremiumMenu(context, favoriteNames[pos], listOf(
                "Remove from Favorites" to {
                    Prefs.setAppFavorite(context, packageName, false)
                    toast("Removed from Favorites")
                    notifyItemChanged(0)
                },
                "App Info" to { openAppInfo(packageName) }
            ))
            true
        }

        startClockTicker(holder)
    }

    private fun startClockTicker(holder: ViewHolder) {
        // Cancel any previous clock loop before starting a new one, so
        // re-binding this page never results in multiple ticking timers.
        clockRunnable?.let { clockHandler.removeCallbacks(it) }

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, dd MMM", Locale.getDefault())

        clockRunnable = object : Runnable {
            override fun run() {
                val now = Calendar.getInstance().time
                holder.timeText?.text = timeFormat.format(now)
                holder.dateText?.text = dateFormat.format(now)
                clockHandler.postDelayed(this, 1000)
            }
        }

        clockHandler.post(clockRunnable!!)
    }

    // ---------------------------------------------------------------------
    // APPS SCREEN
    // ---------------------------------------------------------------------

    private fun bindAppsScreen(holder: ViewHolder) {
        val appsListView = holder.itemView.findViewById<ListView>(R.id.appsListView)
        val searchBar = holder.itemView.findViewById<EditText>(R.id.searchBar)
        val settingsGearBtn = holder.itemView.findViewById<TextView>(R.id.settingsGearBtn)

        appsListViewRef = appsListView
        appsSearchQuery = ""

        settingsGearBtn.setOnClickListener {
            openWithSlideUp(SettingsActivity::class.java)
        }

        refreshAppsList()

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                appsSearchQuery = s.toString()
                refreshAppsList()
            }
        })

        appsListView.setOnItemLongClickListener { _, _, i, _ ->
            val (label, key) = appsDisplayItems[i]
            if (key.startsWith(FOLDER_PREFIX)) {
                showFolderManageMenu(key.removePrefix(FOLDER_PREFIX))
            } else {
                showAppLongPressMenu(label, key)
            }
            true
        }

        appsListView.setOnItemClickListener { _, _, i, _ ->
            val (label, key) = appsDisplayItems[i]
            if (key.startsWith(FOLDER_PREFIX)) {
                openFolder(key.removePrefix(FOLDER_PREFIX))
            } else {
                launchOrShowBlocked(key, label)
            }
        }
    }

    /** Rebuilds the Apps-screen list from current data, keeping the user's scroll position. */
    private fun refreshAppsList() {
        val listView = appsListViewRef ?: return
        val firstVisible = listView.firstVisiblePosition

        appsDisplayItems = buildDisplayItems(appsSearchQuery)
        listView.adapter = appNameAdapter(context, appsDisplayItems.map { it.first })
        listView.setSelection(firstVisible)
    }

    /** Builds the Apps-screen row list: standalone apps + one row per non-empty folder, matching the query. */
    private fun buildDisplayItems(query: String): List<Pair<String, String>> {
        val hiddenApps = Prefs.getHiddenApps(context)
        val folders = Prefs.getFolders(context)
        val appsInFolders = folders.values.flatten().toSet()

        val standaloneApps = installedApps()
            .filter { it.second !in hiddenApps && it.second !in appsInFolders }
            .map { appLabel(context, it.second) to it.second }

        val folderRows = folders.mapNotNull { (name, members) ->
            val visibleCount = members.count { it !in hiddenApps }
            if (visibleCount == 0) null else "📁 $name ($visibleCount)" to (FOLDER_PREFIX + name)
        }

        return (standaloneApps + folderRows)
            .filter { it.first.contains(query, ignoreCase = true) }
            .sortedBy { it.first.lowercase() }
    }

    private fun installedApps(): List<Pair<String, String>> {
        cachedInstalledApps?.let { return it }

        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val apps = pm.queryIntentActivities(intent, 0)
            .map { it.loadLabel(pm).toString() to it.activityInfo.packageName }
            .distinctBy { it.second }
            .sortedBy { it.first.lowercase() }

        cachedInstalledApps = apps
        return apps
    }

    // ---- App long-press menu ----

    private fun showAppLongPressMenu(appName: String, packageName: String) {
        showPremiumMenu(context, appName, listOf(
            "Add to Favorites" to {
                Prefs.setAppFavorite(context, packageName, true)
                toast("$appName added to Favorites")
                notifyItemChanged(0)
            },
            "Block" to { openBlockScreen(packageName) },
            "Rename" to { promptRename(appName, packageName) },
            "Move to Folder" to { promptMoveToFolder(packageName) },
            "Hide App" to {
                Prefs.setAppHidden(context, packageName, true)
                toast("$appName hidden")
                refreshAppsList()
            },
            "App Info" to { openAppInfo(packageName) }
        ))
    }

    private fun promptRename(currentName: String, packageName: String) {
        val input = EditText(context).apply { setText(currentName) }

        AlertDialog.Builder(context)
            .setTitle("Rename App")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                Prefs.setCustomName(context, packageName, input.text.toString())
                refreshAppsList()
                notifyItemChanged(0)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ---- Folders ----

    private fun promptMoveToFolder(packageName: String) {
        val folders = Prefs.getFolders(context).keys.sorted()

        val options = folders.map { name ->
            name to {
                Prefs.addAppToFolder(context, name, packageName)
                refreshAppsList()
            }
        } + ("+ New Folder" to { promptNewFolder(packageName) })

        showPremiumMenu(context, "Move to Folder", options)
    }

    private fun promptNewFolder(packageName: String) {
        val input = EditText(context)

        AlertDialog.Builder(context)
            .setTitle("New Folder Name")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    Prefs.addAppToFolder(context, name, packageName)
                    refreshAppsList()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openFolder(folderName: String) {
        val hiddenApps = Prefs.getHiddenApps(context)
        val members = (Prefs.getFolders(context)[folderName] ?: emptySet()).filter { it !in hiddenApps }
        if (members.isEmpty()) return

        val options = members.map { pkg ->
            val name = appLabel(context, pkg)
            name to { launchOrShowBlocked(pkg, name) }
        } + ("⚙ Manage Folder" to { showFolderManageMenu(folderName) })

        showPremiumMenu(context, folderName, options)
    }

    private fun showFolderManageMenu(folderName: String) {
        val hiddenApps = Prefs.getHiddenApps(context)
        val members = (Prefs.getFolders(context)[folderName] ?: emptySet()).filter { it !in hiddenApps }

        val removeOptions = members.map { pkg ->
            val name = appLabel(context, pkg)
            "Remove: $name" to {
                Prefs.removeAppFromFolder(context, pkg)
                refreshAppsList()
            }
        }

        val deleteOption = "Delete Folder" to {
            Prefs.deleteFolder(context, folderName)
            toast("\"$folderName\" deleted")
            refreshAppsList()
        }

        showPremiumMenu(context, "Manage \"$folderName\"", removeOptions + deleteOption)
    }

    // ---------------------------------------------------------------------
    // SHARED HELPERS
    // ---------------------------------------------------------------------

    /** Launches [packageName] unless it's currently blocked (explicit or scheduled), in which case a toast explains why not. */
    private fun launchOrShowBlocked(packageName: String, appName: String) {
        BlockManager.blockedMessage(context, packageName, appName)?.let {
            toast(it)
            return
        }

        context.packageManager.getLaunchIntentForPackage(packageName)?.let {
            MindfulLaunch.launch(context, it)
        }
    }

    private fun openAppInfo(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun openBlockScreen(packageName: String) {
        val intent = Intent(context, BlockAppActivity::class.java)
            .putExtra(BlockAppActivity.EXTRA_PACKAGE_NAME, packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent, slideUpOptions())
    }

    private fun openWithSlideUp(activity: Class<*>) {
        val intent = Intent(context, activity).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent, slideUpOptions())
    }

    private fun slideUpOptions() =
        ActivityOptions.makeCustomAnimation(context, R.anim.slide_up_in, R.anim.stay).toBundle()
}
