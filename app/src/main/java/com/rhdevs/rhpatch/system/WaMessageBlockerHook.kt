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
        // [REVISI KEAMANAN]: Hook SQLite Database dihapus karena menyebabkan crash dan "Muat ulang cache" WA.
        // Kita beralih ke Hook NotificationManager (Lapis 2) seperti SMS.
        try {
            val notifManagerClass = XposedHelpers.findClassIfExists("android.app.NotificationManager", classLoader)
            if (notifManagerClass != null) {
                XposedHelpers.findAndHookMethod(
                    notifManagerClass,
                    "notify",
                    String::class.java,
                    Int::class.java,
                    android.app.Notification::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            try {
                                val notification = param.args[2] as? android.app.Notification ?: return
                                val text = notification.extras?.getCharSequence("android.text")?.toString() ?: ""
                                val title = notification.extras?.getCharSequence("android.title")?.toString() ?: ""
                                
                                val fullText = "$title $text"
                                val ctx = getAppContext()
                                val keywordsRaw = getConfigString(prefs, ctx, "antispam_sms_keywords", "")
                                
                                if (keywordsRaw.isNotEmpty()) {
                                    val keywords = keywordsRaw.split(Regex("[,\n]+")).map { it.trim().lowercase() }.filter { it.isNotEmpty() }
                                    for (keyword in keywords) {
                                        val regex = Regex("(?i)" + java.util.regex.Pattern.quote(keyword))
                                        if (regex.containsMatchIn(fullText)) {
                                            XposedBridge.log("Rhpatch Anti-Spam: Memblokir Notifikasi WA Spam -> $fullText")
                                            saveSpamLog("WA Spam Terdeteksi: '$keyword' -> $fullText", ctx)
                                            param.result = null // Batalkan notifikasi
                                            return
                                        }
                                    }
                                }
                            } catch (e: Throwable) {}
                        }
                    }
                )
                
                // Juga hook versi 3 parameter (int, Notification)
                XposedHelpers.findAndHookMethod(
                    notifManagerClass,
                    "notify",
                    Int::class.java,
                    android.app.Notification::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            try {
                                val notification = param.args[1] as? android.app.Notification ?: return
                                val text = notification.extras?.getCharSequence("android.text")?.toString() ?: ""
                                val title = notification.extras?.getCharSequence("android.title")?.toString() ?: ""
                                
                                val fullText = "$title $text"
                                val ctx = getAppContext()
                                val keywordsRaw = getConfigString(prefs, ctx, "antispam_sms_keywords", "")
                                
                                if (keywordsRaw.isNotEmpty()) {
                                    val keywords = keywordsRaw.split(Regex("[,\n]+")).map { it.trim().lowercase() }.filter { it.isNotEmpty() }
                                    for (keyword in keywords) {
                                        val regex = Regex("(?i)" + java.util.regex.Pattern.quote(keyword))
                                        if (regex.containsMatchIn(fullText)) {
                                            XposedBridge.log("Rhpatch Anti-Spam: Memblokir Notifikasi WA Spam (Lapis 2) -> $fullText")
                                            saveSpamLog("WA Spam Terdeteksi (ID): '$keyword' -> $fullText", ctx)
                                            param.result = null // Batalkan notifikasi
                                            return
                                        }
                                    }
                                }
                            } catch (e: Throwable) {}
                        }
                    }
                )
                XposedBridge.log("Rhpatch Anti-Spam: WA Message Blocker (Notification Lapis 2) berhasil terpasang.")
            }
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch Anti-Spam: Gagal memuat WA Notification Blocker - ${e.message}")
        }
    }
}
