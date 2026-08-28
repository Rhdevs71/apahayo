package com.rhdevs.rhpatch.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.topjohnwu.superuser.Shell
import com.rhdevs.rhpatch.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.regex.Pattern

object RootDiagnostics {

    private const val SEPOLICY_LOG_PATH = "/data/adb/lspd/log/verbose*.log"
    private const val HMA_CONFIG_GLOB = "/data/misc/hide_my_applist*/config.json"
    private const val HMA_ZYGISK_PATH = "/data/adb/modules/hma_oss_zygisk"
    private const val LSP_CONFIG_DB = "/data/adb/lspd/config/modules_config.db"

    private val SEPOLICY_PATTERN = Pattern.compile("(?i)sepolicy")
    private val ISSUE_PATTERN = Pattern.compile("(?i)error|invalid|failed")

    private val WHATSAPP_PACKAGES = listOf(
        "com.whatsapp",
        "com.whatsapp.w4b"
    )

    private val Rhpatch_PACKAGES = listOf(
        "com.rhdevs.rhpatch",
        "com.rhdevs.rhpatch.w4b"
    )

    enum class LogType { INFO, SUCCESS, WARNING, ERROR }

    data class LogEntry(val message: String, val type: LogType = LogType.INFO)

    interface Callback {
        fun onLog(entry: LogEntry)
    }

    fun runDiagnostics(context: Context, callback: Callback) {
        Shell.getShell { shell ->
            if (!shell.isRoot) {
                callback.onLog(
                    LogEntry(
                        context.getString(R.string.diag_root_denied),
                        LogType.ERROR
                    )
                )
                return@getShell
            }
            callback.onLog(LogEntry(context.getString(R.string.diag_root_granted), LogType.SUCCESS))

            // 1. FORCE-GRANT ALL PERMISSIONS VIA ROOT
            callback.onLog(LogEntry("Menerapkan Izin Maksimal & Aksesibilitas via Root...", LogType.INFO))
            try {
                val pkg = context.packageName
                Shell.cmd(
                    "settings put secure accessibility_enabled 1",
                    "settings put secure enabled_accessibility_services /com.rhdevs.rhpatch.services.AutoSenderAccessibilityService",
                    "settings put secure enabled_notification_listeners /com.rhdevs.rhpatch.services.AutoReplyService",
                    "pm grant  android.permission.SYSTEM_ALERT_WINDOW",
                    "pm grant  android.permission.POST_NOTIFICATIONS",
                    "pm grant  android.permission.SCHEDULE_EXACT_ALARM",
                    "pm grant  android.permission.READ_LOGS",
                    "pm grant  android.permission.DUMP",
                    "pm grant  android.permission.READ_EXTERNAL_STORAGE",
                    "pm grant  android.permission.WRITE_EXTERNAL_STORAGE",
                    "dumpsys deviceidle whitelist +",
                    "cmd appops set  SYSTEM_ALERT_WINDOW allow",
                    "cmd appops set  RUN_IN_BACKGROUND allow",
                    "cmd appops set  RUN_ANY_IN_BACKGROUND allow",
                    "cmd appops set  MANAGE_EXTERNAL_STORAGE allow"
                ).exec()
                
                callback.onLog(LogEntry("Semua Izin Sistem, Aksesibilitas & Anti-Kill Berhasil Diberikan!", LogType.SUCCESS))
            } catch (e: Exception) {
                callback.onLog(LogEntry("Peringatan saat set izin: ", LogType.WARNING))
            }

            // 2. CHECK STATUS
            callback.onLog(LogEntry(""))
            callback.onLog(LogEntry("--- STATUS SISTEM & PERIZINAN ---", LogType.INFO))

            // Accessibility Status
            try {
                val accessibilityEnabled = android.provider.Settings.Secure.getInt(
                    context.contentResolver,
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED
                )
                val settingValue = android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
                if (accessibilityEnabled == 1 && settingValue != null && settingValue.contains(context.packageName)) {
                    callback.onLog(LogEntry("Layanan Aksesibilitas: AKTIF", LogType.SUCCESS))
                } else {
                    callback.onLog(LogEntry("Layanan Aksesibilitas: TIDAK AKTIF", LogType.WARNING))
                }
            } catch (e: Exception) {
                callback.onLog(LogEntry("Gagal membaca status aksesibilitas", LogType.WARNING))
            }

            // Overlay Status
            if (android.provider.Settings.canDrawOverlays(context)) {
                callback.onLog(LogEntry("Izin Tampil di Atas: DIBERIKAN", LogType.SUCCESS))
            } else {
                callback.onLog(LogEntry("Izin Tampil di Atas: DITOLAK", LogType.WARNING))
            }

            // Notification Status
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    callback.onLog(LogEntry("Izin Notifikasi: DIBERIKAN", LogType.SUCCESS))
                } else {
                    callback.onLog(LogEntry("Izin Notifikasi: DITOLAK", LogType.WARNING))
                }
            }

            // 3. SEPOLICY & HMA INSPECTION
            checkSepolicy(context, callback)
            checkHideMyAppList(context, callback)

            // 4. LIVE SYSTEM LOGCAT STREAM
            appendSystemLogcat(callback)
        }
    }

    private fun checkSepolicy(context: Context, callback: Callback) {
        callback.onLog(LogEntry(""))
        callback.onLog(LogEntry(context.getString(R.string.diag_sepolicy_checking)))

        val result = Shell.cmd("cat ").exec()
        if (!result.isSuccess || result.out.isEmpty()) {
            callback.onLog(
                LogEntry(
                    context.getString(R.string.diag_sepolicy_not_found),
                    LogType.WARNING
                )
            )
            return
        }

        val foundLine = result.out.find { line ->
            SEPOLICY_PATTERN.matcher(line).find() && ISSUE_PATTERN.matcher(line).find()
        }

        if (foundLine == null) {
            callback.onLog(
                LogEntry(
                    context.getString(R.string.diag_sepolicy_no_issues),
                    LogType.SUCCESS
                )
            )
        } else {
            callback.onLog(LogEntry(context.getString(R.string.diag_sepolicy_found), LogType.ERROR))
            callback.onLog(LogEntry(foundLine.trim(), LogType.WARNING))
            callback.onLog(LogEntry(""))
            callback.onLog(
                LogEntry(
                    context.getString(R.string.diag_sepolicy_broken),
                    LogType.ERROR
                )
            )
        }
    }

    private fun checkHideMyAppList(context: Context, callback: Callback) {
        callback.onLog(LogEntry(""))
        callback.onLog(LogEntry(context.getString(R.string.diag_hma_checking)))

        if (!isHmaActive(context, callback)) {
            callback.onLog(
                LogEntry(
                    context.getString(R.string.diag_hma_not_active),
                    LogType.WARNING
                )
            )
            return
        }

        val result = Shell.cmd("cat ").exec()
        if (!result.isSuccess || result.out.isEmpty()) {
            callback.onLog(
                LogEntry(
                    context.getString(R.string.diag_hma_not_found),
                    LogType.WARNING
                )
            )
            return
        }

        val config = try {
            JSONObject(result.out.joinToString("\n"))
        } catch (e: Exception) {
            callback.onLog(
                LogEntry(
                    context.getString(R.string.diag_hma_invalid) + ": " + e.message,
                    LogType.ERROR
                )
            )
            return
        }

        val templates = config.optJSONObject("templates") ?: JSONObject()
        val scope = config.optJSONObject("scope") ?: JSONObject()

        val blockedTargets = WHATSAPP_PACKAGES.mapNotNull { pkg ->
            val scopeObj = scope.optJSONObject(pkg) ?: return@mapNotNull null
            if (isHmaBlockingRhpatch(scopeObj, templates)) pkg else null
        }

        if (blockedTargets.isEmpty()) {
            callback.onLog(
                LogEntry(
                    context.getString(R.string.diag_hma_no_blocks),
                    LogType.SUCCESS
                )
            )
        } else {
            callback.onLog(LogEntry(context.getString(R.string.diag_hma_blocked), LogType.ERROR))
            blockedTargets.forEach { callback.onLog(LogEntry("- ", LogType.WARNING)) }
            callback.onLog(LogEntry(""))
            callback.onLog(LogEntry(context.getString(R.string.diag_hma_disable), LogType.ERROR))
        }
    }

    private fun appendSystemLogcat(callback: Callback) {
        callback.onLog(LogEntry(""))
        callback.onLog(LogEntry("--- LOGCAT & LOG SISTEM ---", LogType.INFO))
        try {
            val result = Shell.cmd("logcat -d -t 60 *:W").exec()
            if (result.isSuccess && result.out.isNotEmpty()) {
                for (line in result.out) {
                    if (line.isBlank()) continue
                    val type = when {
                        line.contains(" E ", ignoreCase = true) || line.contains("FATAL", ignoreCase = true) -> LogType.ERROR
                        line.contains(" W ", ignoreCase = true) -> LogType.WARNING
                        else -> LogType.INFO
                    }
                    callback.onLog(LogEntry(line.trim(), type))
                }
            }
        } catch (e: Exception) {
            callback.onLog(LogEntry("Gagal membaca logcat: ", LogType.WARNING))
        }
    }

    private fun isHmaActive(context: Context, callback: Callback): Boolean {
        val zygiskResult = Shell.cmd(
            "[ -d  ] && [ ! -f /disable ] && echo active"
        ).exec()
        if (zygiskResult.out.any { it == "active" }) {
            callback.onLog(
                LogEntry(
                    context.getString(R.string.diag_hma_zygisk_active),
                    LogType.SUCCESS
                )
            )
            return true
        }

        val lspResult = Shell.cmd("[ -f  ] && echo exists").exec()
        if (lspResult.out.any { it == "exists" }) {
            val cacheFile = File(context.cacheDir, "hma_lsposed_config.db")
            val walFile = File(context.cacheDir, "hma_lsposed_config.db-wal")
            val shmFile = File(context.cacheDir, "hma_lsposed_config.db-shm")
            val journalFile = File(context.cacheDir, "hma_lsposed_config.db-journal")

            listOf(cacheFile, walFile, shmFile, journalFile).forEach { it.delete() }

            Shell.cmd(
                "cp   && " +
                        "cp -wal  2>/dev/null; " +
                        "cp -shm  2>/dev/null; " +
                        "cp -journal  2>/dev/null; " +
                        "chmod 777     2>/dev/null"
            ).exec()

            if (cacheFile.exists() && isHmaInLsposedDb(cacheFile)) {
                callback.onLog(
                    LogEntry(
                        context.getString(R.string.diag_hma_lsposed_active),
                        LogType.SUCCESS
                    )
                )
                return true
            }
        }

        return false
    }

    private fun isHmaInLsposedDb(dbFile: File): Boolean {
        var db: SQLiteDatabase? = null
        return try {
            db =
                SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val cursor = db.rawQuery(
                "SELECT 1 FROM modules WHERE module_pkg_name LIKE ? AND enabled = 1 LIMIT 1",
                arrayOf("%hidemyapplist%")
            )
            val found = cursor.moveToFirst()
            cursor.close()
            found
        } catch (e: Exception) {
            false
        } finally {
            db?.close()
        }
    }

    private fun isHmaBlockingRhpatch(scopeObj: JSONObject, templates: JSONObject): Boolean {
        val useWhitelist = scopeObj.optBoolean("useWhitelist", false)

        val extraAppList = scopeObj.optJSONArray("extraAppList")?.toStringList() ?: emptyList()
        val extraOppositeAppList =
            scopeObj.optJSONArray("extraOppositeAppList")?.toStringList() ?: emptyList()

        if (useWhitelist) {
            if (extraOppositeAppList.any { it in Rhpatch_PACKAGES }) return true
        } else {
            if (extraAppList.any { it in Rhpatch_PACKAGES }) return true
        }

        val appliedTemplates =
            scopeObj.optJSONArray("applyTemplates")?.toStringList() ?: emptyList()
        for (templateName in appliedTemplates) {
            val template = templates.optJSONObject(templateName) ?: continue
            val templateIsWhitelist = template.optBoolean("isWhitelist", false)
            val appList = template.optJSONArray("appList")?.toStringList() ?: emptyList()

            if (!templateIsWhitelist && appList.any { it in Rhpatch_PACKAGES }) {
                return true
            }
        }

        val appliedPresets = scopeObj.optJSONArray("applyPresets")?.toStringList() ?: emptyList()
        return appliedPresets.any { it.equals("xposed", ignoreCase = true) }
    }

    private fun JSONArray.toStringList(): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until length()) {
            optString(i, null)?.let { list.add(it) }
        }
        return list
    }
}