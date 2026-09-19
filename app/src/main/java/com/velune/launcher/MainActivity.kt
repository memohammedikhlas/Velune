package com.velune.launcher

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: ViewPageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.viewPager)

        // PERMISSIONS — each one checks and requests only what it's named for
        requestOverlayPermissionIfNeeded()
        requestUsageAccessPermissionIfNeeded()

        updateLauncherIconVisibility()

        // VIEWPAGER
        adapter = ViewPageAdapter(this)
        viewPager.adapter = adapter

        viewPager.setCurrentItem(0, false)
        viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        viewPager.offscreenPageLimit = 1

        viewPager.setPageTransformer { page, position ->
            val scale = 0.92f + (1 - kotlin.math.abs(position)) * 0.08f
            page.scaleY = scale
            page.alpha = 0.7f + (1 - kotlin.math.abs(position)) * 0.3f
        }

        // FULLSCREEN
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    /** Checks the "Display over other apps" permission needed for the session overlay. */
    private fun requestOverlayPermissionIfNeeded() {
        if (Settings.canDrawOverlays(this)) return

        AlertDialog.Builder(this)
            .setTitle("Overlay Permission Required")
            .setMessage("Velune needs overlay permission to show focus reminders over apps.")
            .setPositiveButton("Allow") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
            .setNegativeButton("Not Now", null)
            .show()
    }

    /** Checks the "Usage Access" permission needed to track which app is in the foreground. */
    private fun requestUsageAccessPermissionIfNeeded() {
        if (hasUsageAccessPermission()) return

        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Velune needs Usage Access permission to track mindful sessions.")
            .setPositiveButton("Allow") { _, _ ->
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
            .setNegativeButton("Not Now", null)
            .show()
    }

    private fun hasUsageAccessPermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
        updateLauncherIconVisibility()
        stopService(Intent(this, MonochromeOverlayService::class.java))
    }

    /**
     * Shows the "Velune" icon in the current launcher's app drawer normally.
     * Once the user actually sets Velune as their default Home app, this
     * disables that icon so Velune doesn't show up as just another app in
     * its own drawer / other launchers while it IS the home screen.
     */
    private fun updateLauncherIconVisibility() {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        val isDefaultHome = resolved?.activityInfo?.packageName == packageName

        val alias = ComponentName(this, "com.velune.launcher.LauncherIcon")

        val desiredState = if (isDefaultHome)
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        else
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED

        if (packageManager.getComponentEnabledSetting(alias) != desiredState) {
            packageManager.setComponentEnabledSetting(
                alias,
                desiredState,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    override fun onBackPressed() {
        if (viewPager.currentItem != 0) {
            viewPager.currentItem = 0
        } else {
            moveTaskToBack(true)
        }
    }
}
