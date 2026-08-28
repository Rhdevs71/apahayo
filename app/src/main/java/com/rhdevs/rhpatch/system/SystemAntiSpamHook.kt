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
            // LAYER 1: Telephony SmsProvider SQLite Database Interception
            val smsProviderClass = XposedHelpers.findClassIfExists("com.android.providers.telephony.SmsProvider", classLoader)
            if (smsProviderClass != null) {
                for (m in smsProviderClass.declaredMethods) {
                    if (m.name == "insert" && m.parameterTypes.size == 2) {
                        XposedBridge.hookMethod(m, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                val values = param.args[1] as? android.content.ContentValues ?: return
                                val body = values.getAsString("body") ?: ""
                                val address = values.getAsString("address") ?: ""
                                
                                if (checkSpamKeyword(body, getAppContext(), prefs) || checkSpamKeyword(address, getAppContext(), prefs)) {
                                    XposedBridge.log("Rhpatch Anti-Spam: Dropped SMS Provider write: $address -> $body")
                                    param.result = Uri.parse("content://sms/blocked/0")
                                }
                            }
                        })
                    }
                }
            }

            // LAYER 2: InboundSmsHandler & Message Tracker
            val inboundSmsHandlerClass = XposedHelpers.findClassIfExists("com.android.internal.telephony.InboundSmsHandler", classLoader)
            if (inboundSmsHandlerClass != null) {
                val dispatchMethods = inboundSmsHandlerClass.declaredMethods.filter { 
                    it.name == "dispatchIntent" || it.name == "dispatchSmsDeliveryIntent" || it.name == "processMessagePart" 
                }
                for (method in dispatchMethods) {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val intent = param.args.firstOrNull { it is android.content.Intent } as? android.content.Intent
                            if (intent != null) {
                                try {
                                    val pdus = intent.extras?.get("pdus") as? Array<*>
                                    val format = intent.extras?.getString("format")
                                    if (pdus != null) {
                                        val smsMessageClass = XposedHelpers.findClass("android.telephony.SmsMessage", classLoader)
                                        val fullMessage = java.lang.StringBuilder()
                                        for (pdu in pdus) {
                                            val sms = XposedHelpers.callStaticMethod(smsMessageClass, "createFromPdu", pdu, format)
                                            val body = XposedHelpers.callMethod(sms, "getDisplayMessageBody") as? String
                                            if (body != null) fullMessage.append(body)
                                        }
                                        
                                        if (checkSpamKeyword(fullMessage.toString(), getAppContext(), prefs)) {
                                            if (method.returnType == Boolean::class.javaPrimitiveType) {
                                                param.result = true
                                            } else if (method.returnType == Int::class.javaPrimitiveType) {
                                                param.result = 1
                                            } else {
                                                param.result = null
                                            }
                                            XposedBridge.log("Rhpatch Anti-Spam: Blocked InboundSmsHandler: $fullMessage")
                                            return
                                        }
                                    }
                                } catch (e: Throwable) {}
                            }

                            // Check tracker object if present
                            val tracker = param.args.firstOrNull { it != null && it.javaClass.simpleName.contains("InboundSmsTracker") }
                            if (tracker != null) {
                                val messageBody = XposedHelpers.callMethod(tracker, "getMessageBody") as? String ?: ""
                                if (checkSpamKeyword(messageBody, getAppContext(), prefs)) {
                                    if (method.returnType == Boolean::class.javaPrimitiveType) {
                                        param.result = true
                                    } else if (method.returnType == Int::class.javaPrimitiveType) {
                                        param.result = 1
                                    } else {
                                        param.result = null
                                    }
                                    XposedBridge.log("Rhpatch Anti-Spam: Blocked SMS Tracker: $messageBody")
                                }
                            }
                        }
                    })
                }
            }

            // LAYER 3: Intents & Messaging Apps
            XposedHelpers.findAndHookMethod(
                "android.provider.Telephony.Sms.Intents", classLoader, "getMessagesFromIntent",
                android.content.Intent::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val messages = param.result as? Array<*> ?: return
                        if (messages.isEmpty()) return
                        
                        val fullMessage = java.lang.StringBuilder()
                        for (msg in messages) {
                            val body = XposedHelpers.callMethod(msg, "getDisplayMessageBody") as? String
                            if (body != null) fullMessage.append(body)
                        }
                        
                        if (checkSpamKeyword(fullMessage.toString(), getAppContext(), prefs)) {
                            val emptyArray = java.lang.reflect.Array.newInstance(messages.javaClass.componentType, 0)
                            param.result = emptyArray
                            XposedBridge.log("Rhpatch Anti-Spam: Hiding SMS via Intents: $fullMessage")
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch Anti-Spam: Failed to hook SMS - ${e.message}")
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
