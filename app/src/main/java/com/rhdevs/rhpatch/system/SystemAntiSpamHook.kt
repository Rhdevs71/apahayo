package com.rhdevs.rhpatch.system

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import de.robv.android.xposed.XSharedPreferences

object SystemAntiSpamHook {
    fun hookSms(classLoader: ClassLoader, prefs: XSharedPreferences) {
        prefs.reload()
        if (!prefs.getBoolean("antispam_sms_enabled", false)) return
        
        try {
            val inboundSmsHandlerClass = XposedHelpers.findClassIfExists("com.android.internal.telephony.InboundSmsHandler", classLoader)
            if (inboundSmsHandlerClass != null) {
                // Hook the method that processes incoming SMS PDUs
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
                                        if (body != null) {
                                            fullMessage += body
                                        }
                                    }
                                    
                                    val keywordsRaw = prefs.getString("antispam_sms_keywords", "") ?: ""
                                    if (keywordsRaw.isNotEmpty()) {
                                        val keywords = keywordsRaw.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
                                        val messageLower = fullMessage.lowercase()
                                        
                                        for (keyword in keywords) {
                                            if (messageLower.contains(keyword)) {
                                                XposedBridge.log("Rhpatch Anti-Spam: Blocked SMS containing keyword '\$keyword'.")
                                                // Cancel the intent delivery by returning EARLY
                                                val returnType = (param.method as? java.lang.reflect.Method)?.returnType
                                                if (returnType == Boolean::class.javaPrimitiveType) {
                                                    param.result = true
                                                } else if (returnType == Int::class.javaPrimitiveType) {
                                                    param.result = 1 // Activity.RESULT_OK
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
                            XposedBridge.log("Rhpatch Anti-Spam SMS error: \${e.message}")
                        }
                    }
                })
                XposedBridge.log("Rhpatch Anti-Spam: SMS Hook initialized successfully.")
            }
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch Anti-Spam SMS init error: \${e.message}")
        }
    }

    fun hookCall(classLoader: ClassLoader, prefs: XSharedPreferences, context: Context?) {
        prefs.reload()
        val blockHidden = prefs.getBoolean("antispam_call_hidden", false)
        val blockNonContacts = prefs.getBoolean("antispam_call_non_contacts", false)
        
        if (!blockHidden && !blockNonContacts) return
        
        try {
            val callsManagerClass = XposedHelpers.findClassIfExists("com.android.server.telecom.CallsManager", classLoader)
            if (callsManagerClass != null) {
                // Hook the method that actually processes the new Call object
                // onSuccessfulIncomingCall(Call call)
                XposedBridge.hookAllMethods(callsManagerClass, "onSuccessfulIncomingCall", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            prefs.reload()
                            val isBlockHidden = prefs.getBoolean("antispam_call_hidden", false)
                            val isBlockNonContacts = prefs.getBoolean("antispam_call_non_contacts", false)
                            
                            if (!isBlockHidden && !isBlockNonContacts) return
                            
                            var sysContext = context
                            if (sysContext == null) {
                                val activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", null)
                                val currentActivityThread = XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread")
                                sysContext = XposedHelpers.callMethod(currentActivityThread, "getSystemContext") as? Context
                            }
                            
                            val call = param.args.firstOrNull { it?.javaClass?.name?.contains("Call") == true } ?: return
                            
                            val handle = XposedHelpers.callMethod(call, "getHandle") as? Uri
                            val callerNumber = handle?.schemeSpecificPart
                            
                            if (callerNumber.isNullOrEmpty()) {
                                if (isBlockHidden) {
                                    XposedBridge.log("Rhpatch Anti-Spam: Blocked hidden/private call.")
                                    rejectCall(call)
                                    param.result = null
                                }
                                return
                            }
                            
                            if (isBlockNonContacts && sysContext != null) {
                                if (!isContact(sysContext, callerNumber)) {
                                    XposedBridge.log("Rhpatch Anti-Spam: Blocked unknown caller \$callerNumber.")
                                    rejectCall(call)
                                    param.result = null
                                }
                            }
                            
                        } catch (e: Throwable) {
                            XposedBridge.log("Rhpatch Anti-Spam Call verification error: \${e.message}")
                        }
                    }
                })
                XposedBridge.log("Rhpatch Anti-Spam: Call Hook initialized successfully.")
            }
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch Anti-Spam Call init error: \${e.message}")
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
