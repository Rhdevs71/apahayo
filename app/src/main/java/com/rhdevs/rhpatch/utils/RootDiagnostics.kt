package com.rhdevs.rhpatch.utils

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.topjohnwu.superuser.Shell
import com.rhdevs.rhpatch.R
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern

object RootDiagnostics {

    private const val SEPOLICY_LOG_PATH = "/data/adb/lspd/log/verbose*.log"
    private const val HMA_CONFIG_GLOB = "/data/misc/hide_my_applist*/config.json"
    private const val HMA_ZYGISK_PATH = "/data/adb/modules/hma_oss_zygisk"

    private val SEPOLICY_PATTERN = Pattern.compile("(?i)sepolicy")
    private val ISSUE_PATTERN = Pattern.compile("(?i)error|invalid|failed")

    private val WHATSAPP_PACKAGES = listOf(
        "com.whatsapp",
        "com.whatsapp.w4b"
    )

    private val RHPATCH_PACKAGES = listOf(
        "com.rhdevs.rhpatch",
        "com.rhdevs.rhpatch.w4b"
    )

    enum class LogType { INFO, SUCCESS, WARNING, ERROR }

    data class LogEntry(val message: String, val type: LogType = LogType.INFO)

    interface Callback {
        fun onLog(entry: LogEntry)
    }

    fun runDiagnostics(context: Context, callback: Callback) {
        callback.onLog(LogEntry("=== DIAGNOSTIK RHPATCH ==="))
        callback.onLog(LogEntry(""))

        Shell.getShell { shell ->
            if (!shell.isRoot) {
                callback.onLog(LogEntry(context.getString(R.string.diag_root_denied), LogType.ERROR))
                return@getShell
            }
            callback.onLog(LogEntry("Root access granted.", LogType.SUCCESS))

            // 1. Grant maximum permissions & anti-hibernation via SU
            callback.onLog(LogEntry("Menerapkan Izin Maksimal via Root..."))
            val pkg = context.packageName
            Shell.cmd(
                "pm grant  android.permission.SYSTEM_ALERT_WINDOW",
                "pm grant  android.permission.POST_NOTIFICATIONS",
                "pm grant  android.permission.SCHEDULE_EXACT_ALARM",
                "pm grant  android.permission.DUMP",
                "pm grant  android.permission.READ_LOGS",
                "dumpsys deviceidle whitelist +",
                "cmd appops set  SYSTEM_ALERT_WINDOW allow",
                "cmd appops set  RUN_IN_BACKGROUND allow",
                "cmd appops set  RUN_ANY_IN_BACKGROUND allow"
            ).exec()
            callback.onLog(LogEntry("Izin Maksimal & Anti-Hibernasi berhasil diterapkan!", LogType.SUCCESS))

            // 2. Check Accessibility Service
            callback.onLog(LogEntry("Memeriksa Layanan Aksesibilitas..."))
            val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
            val isAccessOk = enabledServices.contains("com.rhdevs.rhpatch.services.AutoSenderAccessibilityService")
            if (isAccessOk) {
                callback.onLog(LogEntry("Layanan Aksesibilitas: AKTIF", LogType.SUCCESS))
            } else {
                callback.onLog(LogEntry("Layanan Aksesibilitas: TIDAK AKTIF", LogType.WARNING))
            }

            // 3. Check Overlay (Draw over apps)
            callback.onLog(LogEntry("Memeriksa Izin Overlay (Tampil di atas)..."))
            val isOverlayOk = Settings.canDrawOverlays(context)
            if (isOverlayOk) {
                callback.onLog(LogEntry("Izin Tampil di Atas: DIBERIKAN", LogType.SUCCESS))
            } else {
                callback.onLog(LogEntry("Izin Tampil di Atas: DITOLAK", LogType.WARNING))
            }

            // 4. Check Notifications
            callback.onLog(LogEntry("Memeriksa Izin Notifikasi..."))
            val isNotifOk = NotificationManagerCompat.from(context).areNotificationsEnabled()
            if (isNotifOk) {
                callback.onLog(LogEntry("Izin Notifikasi: DIBERIKAN", LogType.SUCCESS))
            } else {
                callback.onLog(LogEntry("Izin Notifikasi: DITOLAK", LogType.WARNING))
            }

            // 5. Check Exact Alarm
            callback.onLog(LogEntry("Memeriksa Izin Alarm Akurat..."))
            var isAlarmOk = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                isAlarmOk = alarmManager.canScheduleExactAlarms()
            }
            if (isAlarmOk) {
                callback.onLog(LogEntry("Izin Alarm Akurat: DIBERIKAN", LogType.SUCCESS))
            } else {
                callback.onLog(LogEntry("Izin Alarm Akurat: DITOLAK", LogType.WARNING))
            }

            // 6. Check Sepolicy
            checkSepolicy(context, callback)

            // 7. Check Hide My App List
            checkHideMyAppList(context, callback)

            // 8. Capture Logcat
            callback.onLog(LogEntry(""))
            callback.onLog(LogEntry("--- BEGIN LOGCAT ---"))
            val logcatResult = Shell.cmd("logcat -d -t 150").exec()
            if (logcatResult.isSuccess && logcatResult.out.isNotEmpty()) {
                logcatResult.out.takeLast(100).forEach { line ->
                    val type = when {
                        line.contains(" E ") || line.contains("FATAL") || line.contains("Exception") -> LogType.ERROR
                        line.contains(" W ") || line.contains("Warning") -> LogType.WARNING
                        else -> LogType.INFO
                    }
                    callback.onLog(LogEntry(line, type))
                }
            } else {
                callback.onLog(LogEntry("Logcat kosong atau tidak dapat diakses.", LogType.WARNING))
            }
        }
    }

    private fun checkSepolicy(context: Context, callback: Callback) {
        callback.onLog(LogEntry(""))
        callback.onLog(LogEntry("Checking sepolicy log..."))

        val result = Shell.cmd("cat ").exec()
        if (!result.isSuccess || result.out.isEmpty()) {
            callback.onLog(LogEntry("No sepolicy log found.", LogType.WARNING))
            return
        }

        val foundLine = result.out.find { line ->
            SEPOLICY_PATTERN.matcher(line).find() && ISSUE_PATTERN.matcher(line).find()
        }

        if (foundLine == null) {
            callback.onLog(LogEntry("No sepolicy issues found in the log.", LogType.SUCCESS))
        } else {
            callback.onLog(LogEntry("Sepolicy issue detected:", LogType.ERROR))
            callback.onLog(LogEntry(foundLine.trim(), LogType.WARNING))
        }
    }

    private fun checkHideMyAppList(context: Context, callback: Callback) {
        callback.onLog(LogEntry(""))
        callback.onLog(LogEntry("Checking Hide My App List status..."))

        if (!isHmaActive(context, callback)) {
            callback.onLog(LogEntry("Hide My App List not active or not installed.", LogType.INFO))
            return
        }

        val result = Shell.cmd("cat ").exec()
        if (!result.isSuccess || result.out.isEmpty()) {
            callback.onLog(LogEntry("Hide My App List (Zygisk) is active.", LogType.SUCCESS))
            callback.onLog(LogEntry("No config found or default template active.", LogType.INFO))
            return
        }

        val config = try {
            JSONObject(result.out.joinToString("\n"))
        } catch (e: Exception) {
            callback.onLog(LogEntry("Error reading HMA config: ", LogType.ERROR))
            return
        }

        val templates = config.optJSONObject("templates") ?: JSONObject()
        val scope = config.optJSONObject("scope") ?: JSONObject()

        val blockedTargets = WHATSAPP_PACKAGES.mapNotNull { pkg ->
            val scopeObj = scope.optJSONObject(pkg) ?: return@mapNotNull null
            if (isHmaBlockingRhpatch(scopeObj, templates)) pkg else null
        }

        if (blockedTargets.isEmpty()) {
            callback.onLog(LogEntry("Hide My App List (Zygisk) is active.", LogType.SUCCESS))
            callback.onLog(LogEntry("No Hide My App List blocks detected for WhatsApp.", LogType.SUCCESS))
        } else {
            callback.onLog(LogEntry("Warning: HMA is hiding Rhpatch from WhatsApp!", LogType.ERROR))
            blockedTargets.forEach { callback.onLog(LogEntry("- ", LogType.WARNING)) }
        }
    }

    private fun isHmaActive(context: Context, callback: Callback): Boolean {
        val zygiskResult = Shell.cmd("[ -d  ] && [ ! -f /disable ] && echo active").exec()
        return zygiskResult.out.any { it.contains("active") }
    }

    private fun isHmaBlockingRhpatch(scopeObj: JSONObject, templates: JSONObject): Boolean {
        val mode = scopeObj.optInt("mode", -1)
        val templateList = scopeObj.optJSONArray("templates") ?: JSONArray()
        val appList = scopeObj.optJSONArray("apps") ?: JSONArray()

        val blockedPackages = mutableSetOf<String>()
        for (i in 0 until appList.length()) {
            blockedPackages.add(appList.optString(i))
        }

        for (i in 0 until templateList.length()) {
            val templateName = templateList.optString(i)
            val templateObj = templates.optJSONObject(templateName) ?: continue
            val templateApps = templateObj.optJSONArray("apps") ?: continue
            for (j in 0 until templateApps.length()) {
                blockedPackages.add(templateApps.optString(j))
            }
        }

        return when (mode) {
            1 -> RHPATCH_PACKAGES.any { it in blockedPackages }
            2, 3 -> RHPATCH_PACKAGES.none { it in blockedPackages }
            else -> false
        }
    }
}