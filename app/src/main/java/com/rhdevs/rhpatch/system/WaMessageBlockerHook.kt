package com.rhdevs.rhpatch.system

import android.content.Context
import android.net.Uri
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XSharedPreferences

object WaMessageBlockerHook {

    private fun getAppContext(): Context? {
        return try {
            val activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", null)
            val currentActivityThread = XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread")
            XposedHelpers.callMethod(currentActivityThread, "getApplication") as? Context
        } catch (e: Exception) {
            null
        }
    }

    private fun getConfigString(prefs: XSharedPreferences?, context: Context?, key: String, fallback: String = ""): String {
        try {
            prefs?.reload()
            if (prefs != null && prefs.contains(key)) {
                return prefs.getString(key, fallback) ?: fallback
            }
        } catch (e: Throwable) {}

        val ctx = context ?: getAppContext() ?: return fallback
        try {
            val remotePrefs = com.crossbowffs.remotepreferences.RemotePreferences(
                ctx, "com.rhdevs.rhpatch.preferences", "prefs"
            )
            return remotePrefs.getString(key, fallback) ?: fallback
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch Anti-Spam: RemotePreferences error in WA: ${e.message}")
        }
        return fallback
    }
    
    private fun saveSpamLog(message: String, context: Context?) {
        try {
            val appCtx = context ?: getAppContext() ?: return
            val intent = android.content.Intent("com.rhdevs.rhpatch.LOG_SPAM")
            intent.putExtra("message", message)
            intent.putExtra("type", "WhatsApp")
            intent.setPackage("com.rhdevs.rhpatch")
            appCtx.sendBroadcast(intent)
        } catch (e: Exception) {}
    }

    fun hook(classLoader: ClassLoader, prefs: XSharedPreferences) {
        // Kita akan menggunakan hook SQLiteDatabase.insert pada tabel 'messages' WhatsApp
        // Ini adalah cara paling universal untuk mencegat pesan WA yang masuk sebelum muncul di UI.
        try {
            val sqliteDbClass = XposedHelpers.findClassIfExists("android.database.sqlite.SQLiteDatabase", classLoader)
            if (sqliteDbClass != null) {
                // Hook insert(String table, String nullColumnHack, ContentValues values)
                val insertMethods = sqliteDbClass.declaredMethods.filter { it.name == "insert" || it.name == "insertWithOnConflict" }
                for (method in insertMethods) {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            try {
                                val table = param.args[0] as? String ?: return
                                if (table == "message" || table == "messages") {
                                    val values = param.args[2] as? android.content.ContentValues ?: return
                                    // text_data is used in newer WA, data in older WA
                                    val textData = values.getAsString("text_data") ?: values.getAsString("data") ?: return
                                    
                                    val ctx = getAppContext()
                                    // User wants WA and SMS to share the same keyword config!
                                    val keywordsRaw = getConfigString(prefs, ctx, "antispam_sms_keywords", "")
                                    if (keywordsRaw.isNotEmpty()) {
                                        val keywords = keywordsRaw.split(Regex("[,\\n]+")).map { it.trim().lowercase() }.filter { it.isNotEmpty() }
                                        for (keyword in keywords) {
                                            val regex = Regex("(?i)" + java.util.regex.Pattern.quote(keyword))
                                            if (regex.containsMatchIn(textData)) {
                                                XposedBridge.log("Rhpatch Anti-Spam: Memblokir pesan WA Spam -> $textData")
                                                saveSpamLog("WA Spam Terdeteksi: '$keyword' -> $textData", ctx)
                                                param.result = -1L // Return gagal insert SQLite
                                                return
                                            }
                                        }
                                    }
                                }
                            } catch (e: Throwable) {}
                        }
                    })
                }
                XposedBridge.log("Rhpatch Anti-Spam: WA Message Blocker (SQLite Lapis 0) berhasil terpasang.")
            }
        } catch (e: Throwable) {}
    }
}
