package com.rhdevs.rhpatch.system

import android.content.Context
import android.net.Uri
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XSharedPreferences

object SystemAntiSpamHook {

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
            if (prefs != null && prefs.contains(key)) return prefs.getString(key, fallback) ?: fallback
        } catch (e: Throwable) {}
        val ctx = context ?: getAppContext() ?: return fallback
        try {
            val remotePrefs = com.crossbowffs.remotepreferences.RemotePreferences(
                ctx, "com.rhdevs.rhpatch.preferences", "prefs"
            )
            return remotePrefs.getString(key, fallback) ?: fallback
        } catch (e: Throwable) {}
        return fallback
    }

    private fun getConfigBoolean(prefs: XSharedPreferences?, context: Context?, key: String, fallback: Boolean = false): Boolean {
        try {
            prefs?.reload()
            if (prefs != null && prefs.contains(key)) return prefs.getBoolean(key, fallback)
        } catch (e: Throwable) {}
        val ctx = context ?: getAppContext() ?: return fallback
        try {
            val remotePrefs = com.crossbowffs.remotepreferences.RemotePreferences(
                ctx, "com.rhdevs.rhpatch.preferences", "prefs"
            )
            return remotePrefs.getBoolean(key, fallback)
        } catch (e: Throwable) {}
        return fallback
    }

    private fun checkSpamKeyword(message: String, context: Context?, prefs: XSharedPreferences?): Boolean {
        if (!getConfigBoolean(prefs, context, "antispam_sms_enabled", false)) return false
        val keywordsStr = getConfigString(prefs, context, "antispam_sms_keywords", "")
        if (keywordsStr.isEmpty()) return false
        val keywords = keywordsStr.split(Regex("[,\\n]+")).map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        val lowerMsg = message.lowercase()
        return keywords.any { lowerMsg.contains(it) }
    }

    
    fun hookSms(classLoader: ClassLoader, prefs: XSharedPreferences, context: Context? = null) {
        try {
            // LAYER 2: Notification Manager (Lebih stabil untuk Android 10+)
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
                                
                                val fullText = " "
                                if (checkSpamKeyword(fullText, context, prefs)) {
                                    param.result = null // Batalkan notifikasi
                                    XposedBridge.log("Rhpatch: Notifikasi SMS Spam berhasil dibungkam!")
                                }
                            } catch (e: Throwable) {}
                        }
                    }
                )
                
                // Also hook the 4-argument notify method just in case
                XposedHelpers.findAndHookMethod(
                    notifManagerClass,
                    "notify",
                    String::class.java,
                    String::class.java,
                    Int::class.java,
                    android.app.Notification::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            try {
                                val notification = param.args[3] as? android.app.Notification ?: return
                                val text = notification.extras?.getCharSequence("android.text")?.toString() ?: ""
                                val title = notification.extras?.getCharSequence("android.title")?.toString() ?: ""
                                
                                val fullText = " "
                                if (checkSpamKeyword(fullText, context, prefs)) {
                                    param.result = null // Batalkan notifikasi
                                }
                            } catch (e: Throwable) {}
                        }
                    }
                )
            }
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch Anti-Spam: Error hooking SMS Notif - ")
        }
    }


    fun hookCall(classLoader: ClassLoader, prefs: XSharedPreferences, context: Context? = null) {
        try {
            val callClass = XposedHelpers.findClassIfExists("com.android.server.telecom.Call", classLoader)
            if (callClass != null) {
                XposedBridge.hookAllConstructors(callClass, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            if (!getConfigBoolean(prefs, getAppContext(), "antispam_call_enabled", false)) return
                            
                            val handle = XposedHelpers.callMethod(param.thisObject, "getHandle") as? Uri ?: return
                            val phoneNumber = handle.schemeSpecificPart
                            
                            val keywordsStr = getConfigString(prefs, getAppContext(), "antispam_call_numbers", "")
                            if (keywordsStr.isEmpty()) return
                            val blockedNumbers = keywordsStr.split(Regex("[,\\n]+")).map { it.trim() }.filter { it.isNotEmpty() }
                            
                            if (blockedNumbers.any { phoneNumber.contains(it) }) {
                                XposedHelpers.callMethod(param.thisObject, "reject", false, null)
                                XposedBridge.log("Rhpatch Anti-Spam: Blocked Call: $phoneNumber")
                            }
                        } catch (e: Throwable) {}
                    }
                })
            }
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch Anti-Spam: Failed to hook Call - ${e.message}")
        }
    }
}
