package com.rhdevs.rhpatch.system

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XSharedPreferences
import org.json.JSONArray
import org.json.JSONObject

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
        } catch (_: Throwable) {}
        val ctx = context ?: getAppContext() ?: return fallback
        try {
            val remotePrefs = com.crossbowffs.remotepreferences.RemotePreferences(
                ctx, "com.rhdevs.rhpatch.preferences", "prefs"
            )
            return remotePrefs.getString(key, fallback) ?: fallback
        } catch (_: Throwable) {}
        return fallback
    }

    private fun getConfigBoolean(prefs: XSharedPreferences?, context: Context?, key: String, fallback: Boolean = false): Boolean {
        try {
            prefs?.reload()
            if (prefs != null && prefs.contains(key)) return prefs.getBoolean(key, fallback)
        } catch (_: Throwable) {}
        val ctx = context ?: getAppContext() ?: return fallback
        try {
            val remotePrefs = com.crossbowffs.remotepreferences.RemotePreferences(
                ctx, "com.rhdevs.rhpatch.preferences", "prefs"
            )
            return remotePrefs.getBoolean(key, fallback)
        } catch (_: Throwable) {}
        return fallback
    }

    fun checkSpamKeyword(message: String, context: Context?, prefs: XSharedPreferences?): String? {
        if (!getConfigBoolean(prefs, context, "antispam_sms_enabled", false)) return null
        val keywordsStr = getConfigString(prefs, context, "antispam_sms_keywords", "")
        if (keywordsStr.isEmpty()) return null
        val keywords = keywordsStr.split(Regex("[,\\n]+")).map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        
        val lowerMsg = message.lowercase()
        val deSpaced = lowerMsg.replace(Regex("[.\\-_\\s]+"), "")
        
        for (kw in keywords) {
            val cleanKw = kw.replace(Regex("[.\\-_\\s]+"), "")
            if (lowerMsg.contains(kw) || (cleanKw.length >= 3 && deSpaced.contains(cleanKw))) {
                return kw
            }
        }
        return null
    }

    private fun saveSpamLog(message: String, keyword: String, context: Context?) {
        try {
            val ctx = context ?: getAppContext() ?: return
            val prefs = ctx.getSharedPreferences("antispam_logs", Context.MODE_PRIVATE)
            val existing = prefs.getString("logs", "[]") ?: "[]"
            val array = JSONArray(existing)
            
            val item = JSONObject().apply {
                put("time", System.currentTimeMillis())
                put("type", "SMS")
                put("message", message.take(150))
                put("keyword", keyword)
            }
            array.put(item)
            
            val trimmedArray = JSONArray()
            val start = (array.length() - 50).coerceAtLeast(0)
            for (i in start until array.length()) {
                trimmedArray.put(array.get(i))
            }
            
            prefs.edit().putString("logs", trimmedArray.toString()).apply()
        } catch (_: Throwable) {}
    }

    // Hook di system_server (package 'android') untuk membungkam notifikasi SMS spam dari SEMUA aplikasi SMS di Android 15
    fun hookSystemServer(classLoader: ClassLoader, prefs: XSharedPreferences) {
        try {
            val nmsClass = XposedHelpers.findClassIfExists("com.android.server.notification.NotificationManagerService", classLoader)
            if (nmsClass != null) {
                val notifHook = object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val pkg = param.args.firstOrNull { it is String } as? String ?: ""
                            val lowerPkg = pkg.lowercase()
                            if (!lowerPkg.contains("messaging") && !lowerPkg.contains("sms") && !lowerPkg.contains("mms") && !lowerPkg.contains("telephony")) {
                                return
                            }

                            val notification = param.args.firstOrNull { it is Notification } as? Notification ?: return
                            val extras = notification.extras ?: return
                            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
                            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

                            val fullText = "$title $text $bigText $subText".trim()
                            if (fullText.isEmpty()) return

                            val matched = checkSpamKeyword(fullText, null, prefs)
                            if (matched != null) {
                                param.result = null
                                XposedBridge.log("Rhpatch: [SystemAntiSpam] Berhasil memblokir notifikasi SMS Spam di system_server dari $pkg! Kata kunci: \"$matched\"")
                                saveSpamLog(fullText, matched, null)
                            }
                        } catch (_: Throwable) {}
                    }
                }

                for (m in nmsClass.declaredMethods) {
                    if (m.name == "enqueueNotificationWithTag") {
                        XposedBridge.hookMethod(m, notifHook)
                        XposedBridge.log("Rhpatch: [SystemAntiSpam] Berhasil memasang hook di NotificationManagerService.enqueueNotificationWithTag")
                    }
                }
            }
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch: [SystemAntiSpam] Error hooking system_server: ${e.message}")
        }
    }

    // Hook client-side di aplikasi SMS
    fun hookSms(classLoader: ClassLoader, prefs: XSharedPreferences, context: Context? = null) {
        hookSmsNotification(classLoader, prefs, context)
    }

    fun hookSmsNotification(classLoader: ClassLoader, prefs: XSharedPreferences, context: Context? = null) {
        try {
            val notifManagerClass = XposedHelpers.findClassIfExists("android.app.NotificationManager", classLoader)
            if (notifManagerClass != null) {
                val notifHook = object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val notification = param.args.firstOrNull { it is Notification } as? Notification ?: return
                            val extras = notification.extras ?: return
                            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
                            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

                            val fullText = "$title $text $bigText $subText".trim()
                            if (fullText.isEmpty()) return

                            val matchedKeyword = checkSpamKeyword(fullText, context, prefs)
                            if (matchedKeyword != null) {
                                param.result = null
                                XposedBridge.log("Rhpatch: Notifikasi SMS Spam dibatalkan di client! Kata kunci: $matchedKeyword")
                                saveSpamLog(fullText, matchedKeyword, context)
                            }
                        } catch (_: Throwable) {}
                    }
                }

                for (m in notifManagerClass.declaredMethods) {
                    if (m.name == "notify") {
                        XposedBridge.hookMethod(m, notifHook)
                    }
                }
            }
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch Anti-Spam: Error hooking SMS Notif - " + e.message)
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

                            val handle = XposedHelpers.callMethod(param.thisObject, "getHandle") as? Uri
                            val phoneNumber = handle?.schemeSpecificPart ?: ""

                            val blockHidden = getConfigBoolean(prefs, getAppContext(), "antispam_call_hidden", false)
                            if (blockHidden && phoneNumber.isEmpty()) {
                                XposedHelpers.callMethod(param.thisObject, "reject", false, null)
                                XposedBridge.log("Rhpatch Anti-Spam: Panggilan nomor privat/tersembunyi berhasil ditolak!")
                                return
                            }

                            val keywordsStr = getConfigString(prefs, getAppContext(), "antispam_call_numbers", "")
                            if (keywordsStr.isNotEmpty() && phoneNumber.isNotEmpty()) {
                                val blockedNumbers = keywordsStr.split(Regex("[,\\n]+")).map { it.trim() }.filter { it.isNotEmpty() }
                                if (blockedNumbers.any { phoneNumber.contains(it) }) {
                                    XposedHelpers.callMethod(param.thisObject, "reject", false, null)
                                    XposedBridge.log("Rhpatch Anti-Spam: Panggilan spam diblokir: " + phoneNumber)
                                    return
                                }
                            }
                        } catch (_: Throwable) {}
                    }
                })
            }
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch Anti-Spam: Failed to hook Call - " + e.message)
        }
    }
}

