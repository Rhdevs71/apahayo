package com.rhdevs.rhpatch.xposed.features.others

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.view.Menu
import android.widget.Toast
import com.rhdevs.rhpatch.xposed.core.Feature
import com.rhdevs.rhpatch.xposed.utils.DesignUtils
import com.rhdevs.rhpatch.xposed.utils.Utils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

class DebugFeature(classLoader: ClassLoader, preferences: SharedPreferences) :
    Feature(classLoader, preferences) {

    override fun doHook() {
        val devModeEnabled = prefs.getBoolean("pref_wa_developer_mode", true)
        if (!devModeEnabled) return

        // 1. Hook Internal Build / Dogfood check agar WhatsApp mengizinkan akses ke layar diagnostik dan debug Meta
        runCatching {
            val buildStatusClasses = listOf(
                "com.whatsapp.buildstatus.BuildStatus",
                "com.whatsapp.util.BuildStatus"
            )
            for (clsName in buildStatusClasses) {
                val cls = XposedHelpers.findClassIfExists(clsName, classLoader) ?: continue
                for (methodName in listOf("isDogfood", "isInternalBuild", "isDebugBuild")) {
                    runCatching {
                        XposedHelpers.findMethodExactIfExists(cls, methodName)?.let { m ->
                            XposedBridge.hookMethod(m, object : XC_MethodHook() {
                                override fun beforeHookedMethod(param: MethodHookParam) {
                                    param.result = true
                                }
                            })
                        }
                    }
                }
            }
        }

        // 2. Tambahkan opsi Developer / Dogfood Diagnostics ke Menu Utama WhatsApp
        MenuHome.addMenuItem { menu, activity ->
            insertDevMenuOption(menu, activity)
        }
    }

    private fun insertDevMenuOption(menu: Menu, activity: Activity) {
        val devItem = menu.add(0, 0, 9998, "🛠️ WhatsApp Dev Diagnostics")
        val icon = DesignUtils.getDrawableByName("ic_settings")
        if (icon != null) {
            icon.setTint(Color.parseColor("#10B981"))
            devItem.icon = icon
        }
        devItem.setOnMenuItemClickListener {
            showDevOptionsDialog(activity)
            true
        }
    }

    private fun showDevOptionsDialog(activity: Activity) {
        val options = arrayOf(
            "Dogfooder Diagnostics (Meta Internal)",
            "Backup dan Restore Debug Screen",
            "Force Dogfood ABProps Reload",
            "Buka Info Build dan Internal Flags"
        )
        AlertDialog.Builder(activity)
            .setTitle("🛠️ Mode Pengembang WhatsApp (Rhpatch)")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchActivitySafe(activity, "com.whatsapp.dogfood.DogfooderDiagnosticsActivity")
                    1 -> launchActivitySafe(activity, "com.whatsapp.backup.debugscreen.BackupDebugActivity")
                    2 -> {
                        Toast.makeText(activity, "Reloading ABProps configuration...", Toast.LENGTH_SHORT).show()
                        Utils.doRestart(activity)
                    }
                    3 -> {
                        val buildInfo = "Package: ${activity.packageName}\nTarget SDK: ${activity.applicationInfo.targetSdkVersion}\nDogfood Mode: Active"
                        AlertDialog.Builder(activity)
                            .setTitle("Internal Build Info")
                            .setMessage(buildInfo)
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun launchActivitySafe(activity: Activity, activityClassName: String) {
        try {
            val intent = Intent()
            intent.setClassName(activity.packageName, activityClassName)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(activity, "Gagal membuka activity: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun getPluginName(): String {
        return "Developer Mode"
    }
}
