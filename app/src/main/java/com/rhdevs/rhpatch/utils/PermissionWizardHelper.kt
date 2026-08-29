package com.rhdevs.rhpatch.utils

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.topjohnwu.superuser.Shell
import com.rhdevs.rhpatch.R

object PermissionWizardHelper {

    fun checkAndRequestPermissions(activity: Activity) {
        val missingPermissions = mutableListOf<String>()

        // 1. Overlay (Tampil di Atas)
        if (!Settings.canDrawOverlays(activity)) {
            missingPermissions.add(activity.getString(R.string.perm_overlay_title) + "\n" + activity.getString(R.string.perm_overlay_desc))
        }

        // 2. Notifikasi (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(activity.getString(R.string.perm_notif_title) + "\n" + activity.getString(R.string.perm_notif_desc))
            }
        }

        // 3. Exact Alarm (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                missingPermissions.add(activity.getString(R.string.perm_alarm_title) + "\n" + activity.getString(R.string.perm_alarm_desc))
            }
        }


        // 4. Location
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(activity.getString(R.string.perm_loc_title) + "\n" + activity.getString(R.string.perm_loc_desc))
        }

        if (missingPermissions.isNotEmpty()) {
            showPermissionDialog(activity, missingPermissions)
        }
    }

    private fun showPermissionDialog(activity: Activity, missingPermissions: List<String>) {
        val message = activity.getString(R.string.perm_dialog_intro) + "\n\n" + missingPermissions.joinToString("\n\n")

        val builder = AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.perm_dialog_title))
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(activity.getString(R.string.perm_btn_manual)) { _, _ ->
                openSettings(activity)
            }

        if (Shell.getShell().isRoot) {
            builder.setNeutralButton(activity.getString(R.string.perm_btn_auto_root)) { _, _ ->
                grantAutomaticallyViaRoot(activity)
            }
        }

        builder.show()
    }

    private fun grantAutomaticallyViaRoot(activity: Activity) {
        Shell.getShell { shell ->
            // Grant Overlay
            Shell.cmd("appops set " + activity.packageName + " SYSTEM_ALERT_WINDOW allow").exec()
            
            // Grant Notifications
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Shell.cmd("pm grant " + activity.packageName + " android.permission.POST_NOTIFICATIONS").exec()
            }
            
            // Grant Exact Alarm
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Shell.cmd("appops set " + activity.packageName + " SCHEDULE_EXACT_ALARM allow").exec()
            }

            
            // Grant Location
            Shell.cmd("pm grant " + activity.packageName + " android.permission.ACCESS_FINE_LOCATION").exec()
            Shell.cmd("pm grant " + activity.packageName + " android.permission.ACCESS_COARSE_LOCATION").exec()

            activity.runOnUiThread {
                checkAndRequestPermissions(activity)
            }
        }
    }

    private fun openSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:" + activity.packageName)
        activity.startActivity(intent)
        // Dialog will show up again on resume if permissions are still missing.
    }
}