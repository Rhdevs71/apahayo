package com.rhdevs.rhpatch.system

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.telecom.PhoneAccountHandle
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

object SystemAntiSpamHook {

    // Helper untuk mendapatkan Context di level System Server
    private fun getSystemContext(context: Context?): Context? {
        if (context != null) return context
        return try {
            val activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", null)
            val currentActivityThread = XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread")
            XposedHelpers.callMethod(currentActivityThread, "getSystemContext") as? Context
        } catch (e: Throwable) {
            null
        }
    }

    fun hookSms(classLoader: ClassLoader, prefs: XSharedPreferences) {
        prefs.reload()
        if (!prefs.getBoolean("antispam_sms_enabled", false)) return

        var isPrimarySmsHooked = false

        // ==========================================
        // 1. PRIMARY HOOK: ContentResolver (Universal API)
        // Mencegat semua SMS sebelum ditulis ke Database HP
        // ==========================================
        try {
            XposedHelpers.findAndHookMethod(
                "android.content.ContentResolver", classLoader,
                "insert", Uri::class.java, ContentValues::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            prefs.reload()
                            if (!prefs.getBoolean("antispam_sms_enabled", false)) return

                            val uri = param.args[0] as? Uri ?: return
                            // Pastikan operasi insert adalah untuk SMS
                            if (uri.toString().contains("content://sms")) {
                                val values = param.args[1] as? ContentValues ?: return
                                val body = values.getAsString("body") ?: return

                                val keywordsRaw = prefs.getString("antispam_sms_keywords", "") ?: ""
                                if (keywordsRaw.isNotEmpty()) {
                                    val keywords = keywordsRaw.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
                                    
                                    for (keyword in keywords) {
                                        val regex = Regex("(?i)" + java.util.regex.Pattern.quote(keyword))
                                        if (regex.containsMatchIn(body)) {
                                            XposedBridge.log("Rhpatch Anti-Spam [Primary SMS]: Diblokir! Mengandung '$keyword'.")
                                            param.result = null // Batalkan operasi insert ke database
                                            return
                                        }
                                    }
                                }
                            }
                        } catch (e: Throwable) {
                            XposedBridge.log("Rhpatch Anti-Spam [Primary SMS] Error: ${e.message}")
                        }
                    }
                }
            )
            isPrimarySmsHooked = true
            XposedBridge.log("Rhpatch Anti-Spam: Universal SMS Hook (Layer 1) berhasil dipasang.")
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch Anti-Spam: Universal SMS Hook gagal. Beralih ke Fallback... (${e.message})")
        }

        // ==========================================
        // 2. FALLBACK HOOK: InboundSmsHandler (Kode Lama Kamu)
        // Mengeksekusi AOSP Hook hanya jika Primary Hook tidak berjalan
        // ==========================================
        if (!isPrimarySmsHooked) {
            try {
                val inboundSmsHandlerClass = XposedHelpers.findClassIfExists("com.android.internal.telephony.InboundSmsHandler", classLoader)
                if (inboundSmsHandlerClass != null) {
                    XposedBridge.hookAllMethods(inboundSmsHandlerClass, "dispatchIntent", object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            try {
                                prefs.reload()
                                if (!prefs.getBoolean("antispam_sms_enabled", false)) return
                                
                                val intent = param.args[0] as? android.content.Intent ?: return
                                if (intent.action == "android.provider.Telephony.SMS_DELIVER") {
                                    val pdus = intent.extras?.get("pdus") as? Array<*>
                                    val format = intent.extras?.getString("format")
                                    
                                    if (pdus != null && pdus.isNotEmpty()) {
                                        val smsMessageClass = XposedHelpers.findClass("android.telephony.SmsMessage", classLoader)
                                        val createFromPduMethod = XposedHelpers.findMethodBestMatch(smsMessageClass, "createFromPdu", ByteArray::class.java, String::class.java)
                                        
                                        var fullMessage = ""
                                        for (pdu in pdus) {
                                            val pduByteArray = pdu as? ByteArray ?: continue
                                            val message = createFromPduMethod.invoke(null, pduByteArray, format)
                                            val body = XposedHelpers.callMethod(message, "getMessageBody") as? String
                                            if (body != null) fullMessage += body
                                        }
                                        
                                        val keywordsRaw = prefs.getString("antispam_sms_keywords", "") ?: ""
                                        if (keywordsRaw.isNotEmpty()) {
                                            val keywords = keywordsRaw.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
                                            
                                            for (keyword in keywords) {
                                                val regex = Regex("(?i)" + java.util.regex.Pattern.quote(keyword))
                                                if (regex.containsMatchIn(fullMessage)) {
                                                    XposedBridge.log("Rhpatch Anti-Spam [Fallback SMS]: Diblokir! Mengandung '$keyword'.")
                                                    intent.action = "" 
                                                    intent.removeExtra("pdus")
                                                    param.args[0] = intent
                                                    
                                                    val returnType = (param.method as? java.lang.reflect.Method)?.returnType
                                                    if (returnType == Boolean::class.javaPrimitiveType) {
                                                        param.result = false
                                                    } else if (returnType == Int::class.javaPrimitiveType) {
                                                        param.result = 0 
                                                    } else {
                                                        param.result = null
                                                    }
                                                    return
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (e: Throwable) {
                                XposedBridge.log("Rhpatch Anti-Spam [Fallback SMS] Runtime Error: ${e.message}")
                            }
                        }
                    })
                    XposedBridge.log("Rhpatch Anti-Spam: Fallback SMS Hook (Layer 2) berhasil dipasang.")
                }
            } catch (e: Throwable) {
                XposedBridge.log("Rhpatch Anti-Spam [Fallback SMS] Init Error: ${e.message}")
            }
        }
    }

    fun hookCall(classLoader: ClassLoader, prefs: XSharedPreferences, context: Context?) {
        prefs.reload()
        val blockHidden = prefs.getBoolean("antispam_call_hidden", false)
        val blockNonContacts = prefs.getBoolean("antispam_call_non_contacts", false)
        
        if (!blockHidden && !blockNonContacts) return

        var isPrimaryCallHooked = false

        // ==========================================
        // 1. PRIMARY HOOK: TelecomManager (Universal API)
        // Mencegat sinyal panggilan sebelum UI Calling muncul
        // ==========================================
        try {
            XposedHelpers.findAndHookMethod(
                "android.telecom.TelecomManager", classLoader,
                "addNewIncomingCall",
                PhoneAccountHandle::class.java, Bundle::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            prefs.reload()
                            val isBlockHidden = prefs.getBoolean("antispam_call_hidden", false)
                            val isBlockNonContacts = prefs.getBoolean("antispam_call_non_contacts", false)

                            if (!isBlockHidden && !isBlockNonContacts) return

                            val extras = param.args[1] as? Bundle ?: return
                            val handle = extras.getParcelable("android.telecom.extra.INCOMING_CALL_ADDRESS") as? Uri
                            val callerNumber = handle?.schemeSpecificPart

                            val sysContext = getSystemContext(context)

                            if (callerNumber.isNullOrEmpty()) {
                                if (isBlockHidden) {
                                    XposedBridge.log("Rhpatch Anti-Spam [Primary Call]: Telepon Private/Hidden diblokir.")
                                    param.result = null // Membatalkan panggilan
                                }
                                return
                            }

                            if (isBlockNonContacts && sysContext != null) {
                                if (!isContact(sysContext, callerNumber)) {
                                    XposedBridge.log("Rhpatch Anti-Spam [Primary Call]: Telepon dari non-kontak ($callerNumber) diblokir.")
                                    param.result = null
                                }
                            }
                        } catch (e: Throwable) {
                            XposedBridge.log("Rhpatch Anti-Spam [Primary Call] Error: ${e.message}")
                        }
                    }
                }
            )
            isPrimaryCallHooked = true
            XposedBridge.log("Rhpatch Anti-Spam: Universal Call Hook (Layer 1) berhasil dipasang.")
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch Anti-Spam: Universal Call Hook gagal. Beralih ke Fallback... (${e.message})")
        }

        // ==========================================
        // 2. FALLBACK HOOK: CallsManager (Kode Lama Kamu)
        // ==========================================
        if (!isPrimaryCallHooked) {
            try {
                val callsManagerClass = XposedHelpers.findClassIfExists("com.android.server.telecom.CallsManager", classLoader)
                if (callsManagerClass != null) {
                    XposedBridge.hookAllMethods(callsManagerClass, "onSuccessfulIncomingCall", object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            try {
                                prefs.reload()
                                val isBlockHidden = prefs.getBoolean("antispam_call_hidden", false)
                                val isBlockNonContacts = prefs.getBoolean("antispam_call_non_contacts", false)
                                
                                if (!isBlockHidden && !isBlockNonContacts) return
                                
                                val sysContext = getSystemContext(context)
                                
                                val call = param.args.firstOrNull { it?.javaClass?.name?.contains("Call") == true } ?: return
                                
                                val handle = XposedHelpers.callMethod(call, "getHandle") as? Uri
                                val callerNumber = handle?.schemeSpecificPart
                                
                                if (callerNumber.isNullOrEmpty()) {
                                    if (isBlockHidden) {
                                        XposedBridge.log("Rhpatch Anti-Spam [Fallback Call]: Telepon Private/Hidden diblokir.")
                                        rejectCall(call)
                                        param.result = null
                                    }
                                    return
                                }
                                
                                if (isBlockNonContacts && sysContext != null) {
                                    if (!isContact(sysContext, callerNumber)) {
                                        XposedBridge.log("Rhpatch Anti-Spam [Fallback Call]: Telepon dari non-kontak ($callerNumber) diblokir.")
                                        rejectCall(call)
                                        param.result = null
                                    }
                                }
                                
                            } catch (e: Throwable) {
                                XposedBridge.log("Rhpatch Anti-Spam [Fallback Call] Verification Error: ${e.message}")
                            }
                        }
                    })
                    XposedBridge.log("Rhpatch Anti-Spam: Fallback Call Hook (Layer 2) berhasil dipasang.")
                }
            } catch (e: Throwable) {
                XposedBridge.log("Rhpatch Anti-Spam [Fallback Call] Init Error: ${e.message}")
            }
        }
    }
    
    private fun rejectCall(callObj: Any) {
        try {
            XposedHelpers.callMethod(callObj, "reject", false, null)
        } catch (e: Throwable) {
            try {
                XposedHelpers.callMethod(callObj, "reject", false, null, null)
            } catch (e2: Throwable) {
                // Ignore
            }
        }
    }
    
    private fun isContact(context: Context, number: String): Boolean {
        if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return false
        }
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        val projection = arrayOf(ContactsContract.PhoneLookup._ID)
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return false
    }
}
