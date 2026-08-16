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

    private fun getConfigBoolean(prefs: XSharedPreferences?, context: Context?, key: String, fallback: Boolean = false): Boolean {
        try {
            prefs?.reload()
            if (prefs != null && prefs.contains(key)) return prefs.getBoolean(key, fallback)
        } catch (e: Throwable) {}

        val ctx = context ?: getAppContext() ?: return fallback
        try {
            val remotePrefs = com.crossbowffs.remotepreferences.RemotePreferences(
                ctx, "com.wmods.wppenhacer.preferences", "prefs"
            )
            return remotePrefs.getBoolean(key, fallback)
        } catch (e: Throwable) {}
        return fallback
    }

    private fun getConfigString(prefs: XSharedPreferences?, context: Context?, key: String, fallback: String = ""): String {
        try {
            prefs?.reload()
            if (prefs != null && prefs.contains(key)) return prefs.getString(key, fallback) ?: fallback
        } catch (e: Throwable) {}

        val ctx = context ?: getAppContext() ?: return fallback
        try {
            val remotePrefs = com.crossbowffs.remotepreferences.RemotePreferences(
                ctx, "com.wmods.wppenhacer.preferences", "prefs"
            )
            return remotePrefs.getString(key, fallback) ?: fallback
        } catch (e: Throwable) {}
        return fallback
    }

    private fun saveSpamLog(message: String, type: String, context: Context?) {
        try {
            val appCtx = context ?: getAppContext() ?: return
            val intent = android.content.Intent("com.rhdevs.rhpatch.LOG_SPAM")
            intent.putExtra("message", message)
            intent.putExtra("type", type)
            intent.setPackage("com.rhdevs.rhpatch")
            appCtx.sendBroadcast(intent)
        } catch (e: Exception) {}
    }

    private fun isSpam(prefs: XSharedPreferences?, message: String, context: Context?): Boolean {
        if (message.isEmpty()) return false
        val keywordsRaw = getConfigString(prefs, context, "antispam_sms_keywords", "")
        if (keywordsRaw.isEmpty()) return false
        
        val keywords = keywordsRaw.split(Regex("[,\\n]+")).map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        for (keyword in keywords) {
            val regex = Regex("(?i)" + java.util.regex.Pattern.quote(keyword))
            if (regex.containsMatchIn(message)) {
                saveSpamLog("SMS Spam Terdeteksi: Kata kunci '$keyword' -> $message", "SMS", context)
                return true
            }
        }
        return false
    }

    fun hookSms(classLoader: ClassLoader, prefs: XSharedPreferences, context: Context? = null) {
        var isSmsProviderHooked = false
        var isInboundSmsHooked = false
        var isIntentSmsHooked = false

        // DOKTRIN 1 - Lapis 0: Pintu Gerbang Database (The Absolute Blocker) - UNIVERSAL
        try {
            val contentProviderClass = XposedHelpers.findClassIfExists("android.content.ContentProvider", classLoader)
            if (contentProviderClass != null) {
                XposedBridge.hookAllMethods(contentProviderClass, "insert", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val uri = param.args[0] as? Uri ?: return
                            if (uri.toString().startsWith("content://sms")) {
                                XposedBridge.log("Rhpatch Anti-Spam: Hooked ContentProvider.insert for SMS")
                                val provider = param.thisObject as? android.content.ContentProvider
                                val ctx = provider?.context ?: getAppContext()
                                
                                val isEnabled = getConfigBoolean(prefs, ctx, "antispam_sms_enabled", false)
                                if (!isEnabled) return
                                
                                val values = param.args[1] as? android.content.ContentValues ?: return
                                val body = values.getAsString("body") ?: return
                                
                                val spam = isSpam(prefs, body, ctx)
                                if (spam) {
                                    XposedBridge.log("Rhpatch Anti-Spam: [Lapis 0 Universal] Memblokir SMS di hulu provider: $body")
                                    param.result = null // Blokir total dari database
                                }
                            }
                        } catch (e: Throwable) {
                            XposedBridge.log("Rhpatch Anti-Spam: Error in Universal ContentProvider hook: ${e.message}")
                        }
                    }
                })
                isSmsProviderHooked = true
                XposedBridge.log("Rhpatch Anti-Spam: Universal SMS Hook (Lapis 0) berhasil terpasang.")
            }
        } catch (e: Throwable) {}

        // DOKTRIN 1 - Lapis 1: Mesin Pemroses PDU (The Network Interceptor)
        if (!isSmsProviderHooked) {
            try {
                val inboundSmsHandlerClass = XposedHelpers.findClassIfExists("com.android.internal.telephony.InboundSmsHandler", classLoader)
                if (inboundSmsHandlerClass != null) {
                    XposedBridge.hookAllMethods(inboundSmsHandlerClass, "dispatchIntent", object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            try {
                                val ctx = getAppContext()
                                if (!getConfigBoolean(prefs, ctx, "antispam_sms_enabled", false)) return
                                // PDU extraction logic would go here if needed.
                            } catch (e: Throwable) {}
                        }
                    })
                    isInboundSmsHooked = true
                    XposedBridge.log("Rhpatch Anti-Spam: SMS Hook (Lapis 1: InboundSmsHandler) fallback terpasang.")
                }
            } catch (e: Throwable) {}
        }

        // DOKTRIN 1 - Lapis 2: Kurir Broadcast (The App Interceptor)
        try {
            val intentsClass = XposedHelpers.findClassIfExists("android.provider.Telephony.Sms.Intents", classLoader)
            if (intentsClass != null) {
                XposedBridge.hookAllMethods(intentsClass, "getMessagesFromIntent", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val ctx = getAppContext()
                            if (!getConfigBoolean(prefs, ctx, "antispam_sms_enabled", false)) return
                            
                            val messages = param.result as? Array<*> ?: return
                            if (messages.isEmpty()) return
                            
                            var fullMessage = ""
                            for (msg in messages) {
                                if (msg == null) continue
                                val getMessageBodyMethod = msg.javaClass.getMethod("getMessageBody")
                                val body = getMessageBodyMethod.invoke(msg) as? String
                                if (body != null) fullMessage += body
                            }
                            
                            if (isSpam(prefs, fullMessage, ctx)) {
                                XposedBridge.log("Rhpatch Anti-Spam: [Lapis 2] Memanipulasi Intent Broadcast untuk SMS: $fullMessage")
                                val smsMessageClass = XposedHelpers.findClass("android.telephony.SmsMessage", classLoader)
                                param.result = java.lang.reflect.Array.newInstance(smsMessageClass, 0) // Hide message from apps
                            }
                        } catch (e: Throwable) {}
                    }
                })
                isIntentSmsHooked = true
                XposedBridge.log("Rhpatch Anti-Spam: SMS Hook (Lapis 2: Intents) berhasil terpasang.")
            }
        } catch (e: Throwable) {}

        // DOKTRIN 1 - Lapis 3: Pembersih Jejak Kosmetik (The Sweeper)
        try {
            val notificationManagerClass = XposedHelpers.findClassIfExists("android.app.NotificationManager", classLoader)
            if (notificationManagerClass != null) {
                XposedBridge.hookAllMethods(notificationManagerClass, "notify", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val ctx = getAppContext()
                            if (!getConfigBoolean(prefs, ctx, "antispam_sms_enabled", false)) return
                            
                            val notification = param.args.firstOrNull { it is android.app.Notification } as? android.app.Notification ?: return
                            val title = notification.extras.getCharSequence("android.title")?.toString() ?: ""
                            val text = notification.extras.getCharSequence("android.text")?.toString() ?: ""
                            val fullText = "$title $text"
                            
                            if (isSpam(prefs, fullText, ctx)) {
                                XposedBridge.log("Rhpatch Anti-Spam: [Lapis 3] Menghapus notifikasi SMS Spam: $fullText")
                                param.result = null // Batalkan notifikasi
                                
                                // Silent Sweeper (Hapus dari DB)
                                if (ctx != null) {
                                    Thread {
                                        try {
                                            Thread.sleep(2000)
                                            val uri = Uri.parse("content://sms/inbox")
                                            val deletedRows = ctx.contentResolver.delete(uri, "body LIKE ?", arrayOf("%${text.take(15)}%"))
                                            if (deletedRows > 0) XposedBridge.log("Rhpatch Anti-Spam: [Lapis 3] Sweeper berhasil menghapus jejak SMS.")
                                        } catch (e: Exception) {}
                                    }.start()
                                }
                            }
                        } catch (e: Throwable) {}
                    }
                })
                XposedBridge.log("Rhpatch Anti-Spam: SMS Hook (Lapis 3: NotificationManager) berhasil terpasang.")
            }
        } catch (e: Throwable) {}
    }

    fun hookCall(classLoader: ClassLoader, prefs: XSharedPreferences, context: Context?) {
        var isCallsManagerHooked = false
        
        // Cek secara dinamis kapan ada panggilan masuk
        fun checkShouldBlockCall(phoneNumber: String?, ctx: Context?): Boolean {
            if (phoneNumber.isNullOrEmpty()) return false
            val blockHidden = getConfigBoolean(prefs, ctx, "antispam_call_hidden", false)
            val blockNonContacts = getConfigBoolean(prefs, ctx, "antispam_call_non_contacts", false)
            
            if (!blockHidden && !blockNonContacts) return false
            
            if (blockHidden && (phoneNumber.contains("unknown", ignoreCase = true) || phoneNumber.contains("private", ignoreCase = true) || phoneNumber.length <= 4)) {
                saveSpamLog("Panggilan Privat/Tersembunyi ditolak", "Call", ctx)
                return true
            }
            if (blockNonContacts && ctx != null) {
                try {
                    val uri = Uri.withAppendedPath(android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
                    ctx.contentResolver.query(uri, arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use {
                        if (it.count == 0) {
                            saveSpamLog("Panggilan dari nomor tak dikenal ditolak: $phoneNumber", "Call", ctx)
                            return true
                        }
                    }
                } catch (e: Exception) {
                    return true // Jika gagal cek kontak, anggap non-kontak
                }
            }
            return false
        }

        // DOKTRIN 2 - Lapis 2: Jantung Server (CallsManager) - Paling kuat!
        try {
            val callsManagerClass = XposedHelpers.findClassIfExists("com.android.server.telecom.CallsManager", classLoader)
            if (callsManagerClass != null) {
                XposedBridge.hookAllMethods(callsManagerClass, "onSuccessfulIncomingCall", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val callObj = param.args[0] ?: return
                            val handleUri = XposedHelpers.callMethod(callObj, "getHandle") as? Uri ?: return
                            val phoneNumber = handleUri.schemeSpecificPart
                            
                            if (checkShouldBlockCall(phoneNumber, getAppContext())) {
                                XposedBridge.log("Rhpatch Anti-Spam: [Lapis 2 Call] Memblokir panggilan: $phoneNumber")
                                XposedHelpers.callMethod(callObj, "reject", false, null)
                                param.result = null
                            }
                        } catch (e: Throwable) {}
                    }
                })
                isCallsManagerHooked = true
                XposedBridge.log("Rhpatch Anti-Spam: Call Hook (Lapis 2: CallsManager) berhasil terpasang.")
            }
        } catch (e: Throwable) {}

        // DOKTRIN 2 - Lapis 3: Layanan Koneksi (ConnectionService)
        if (!isCallsManagerHooked) {
            try {
                val connectionServiceClass = XposedHelpers.findClassIfExists("android.telecom.ConnectionService", classLoader)
                if (connectionServiceClass != null) {
                    XposedBridge.hookAllMethods(connectionServiceClass, "createIncomingConnection", object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            try {
                                val request = param.args[1] as? android.telecom.ConnectionRequest ?: return
                                val phoneNumber = request.address?.schemeSpecificPart
                                
                                if (checkShouldBlockCall(phoneNumber, getAppContext())) {
                                    XposedBridge.log("Rhpatch Anti-Spam: [Lapis 3 Call] Memutus koneksi: $phoneNumber")
                                    val connection = param.result as? android.telecom.Connection ?: return
                                    connection.setDisconnected(android.telecom.DisconnectCause(android.telecom.DisconnectCause.REJECTED))
                                }
                            } catch (e: Throwable) {}
                        }
                    })
                    XposedBridge.log("Rhpatch Anti-Spam: Call Hook (Lapis 3: ConnectionService) fallback terpasang.")
                }
            } catch (e: Throwable) {}
        }
    }
}
