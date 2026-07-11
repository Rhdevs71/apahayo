package com.wmods.wppenhacer.xposed.features.general

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import com.wmods.wppenhacer.BuildConfig
import com.wmods.wppenhacer.xposed.core.Feature
import com.wmods.wppenhacer.xposed.core.WppCore
import com.wmods.wppenhacer.xposed.core.components.FMessageWpp
import com.wmods.wppenhacer.xposed.utils.ReflectionUtils
import com.wmods.wppenhacer.xposed.utils.Utils
import de.robv.android.xposed.XposedBridge
import java.io.File
import java.util.Collections

class MessageSchedulerHook(loader: ClassLoader, preferences: SharedPreferences) : Feature(loader, preferences) {

    override fun getPluginName(): String {
        return "MessageSchedulerHook"
    }

    override fun doHook() {
        registerSchedulerReceiver()
    }

    @SuppressLint("WrongConstant")
    private fun registerSchedulerReceiver() {
        val filter = IntentFilter("com.wmods.wppenhacer.SCHEDULED_SEND")
        Utils.application.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val id = intent.getIntExtra("id", -1)
                    val jid = intent.getStringExtra("jid") ?: return
                    val messageText = intent.getStringExtra("messageText") ?: ""
                    val mediaPath = intent.getStringExtra("mediaPath")
                    val mediaType = intent.getStringExtra("mediaType")

                    XposedBridge.log("Scheduler received trigger for message id: $id, JID: $jid")

                    // Run on main thread because WhatsApp APIs often require it
                    Handler(Looper.getMainLooper()).post {
                        try {
                            if (mediaPath != null && mediaPath.isNotEmpty()) {
                                sendMediaMessage(jid, messageText, mediaPath, mediaType, id)
                            } else {
                                sendTextMessage(jid, messageText, id)
                            }
                        } catch (e: Exception) {
                            XposedBridge.log("Failed to process scheduled message: ${e.message}")
                            e.printStackTrace()
                            sendStatusBroadcast(id, false)
                        }
                    }
                }
            },
            filter,
            "com.wmods.wppenhacer.permission.SEND_RECEIVE_WPP", // Add a custom permission if needed, or null
            null,
            Context.RECEIVER_EXPORTED
        )
    }

    private fun sendTextMessage(jid: String, text: String, id: Int) {
        try {
            val userJid = WppCore.createUserJid(jid)
            if (userJid == null) {
                XposedBridge.log("Scheduler Error: UserJid is null for $jid")
                sendStatusBroadcast(id, false)
                return
            }

            val actionUser = WppCore.getActionUser()
            if (actionUser == null) {
                XposedBridge.log("Scheduler Error: ActionUser is null")
                sendStatusBroadcast(id, false)
                return
            }

            val senderMethod = ReflectionUtils.findMethodUsingFilterIfExists(actionUser.javaClass) { method ->
                List::class.java.isAssignableFrom(method.returnType) &&
                        ReflectionUtils.findIndexOfType(method.parameterTypes, String::class.java) != -1 &&
                        ReflectionUtils.findIndexOfType(method.parameterTypes, List::class.java) != -1
            }

            if (senderMethod == null) {
                XposedBridge.log("Scheduler Error: senderMethod not found")
                sendStatusBroadcast(id, false)
                return
            }

            val newObject = arrayOfNulls<Any>(senderMethod.parameterCount)
            for (i in newObject.indices) {
                val param = senderMethod.parameterTypes[i]
                newObject[i] = ReflectionUtils.getDefaultValue(param)
            }

            val textIndex = ReflectionUtils.findIndexOfType(senderMethod.parameterTypes, String::class.java)
            newObject[textIndex] = text

            val jidIndex = ReflectionUtils.findIndexOfType(senderMethod.parameterTypes, List::class.java)
            newObject[jidIndex] = Collections.singletonList(userJid)

            senderMethod.invoke(actionUser, *newObject)
            XposedBridge.log("Scheduled text message sent successfully to $jid")
            sendStatusBroadcast(id, true)
        } catch (e: Exception) {
            XposedBridge.log("Error sending text: ${e.message}")
            e.printStackTrace()
            sendStatusBroadcast(id, false)
        }
    }

    private fun sendMediaMessage(jid: String, caption: String, mediaPath: String, mediaType: String?, id: Int) {
        try {
            val file = File(mediaPath)
            if (!file.exists()) {
                XposedBridge.log("Scheduler Error: Media file does not exist: $mediaPath")
                sendStatusBroadcast(id, false)
                return
            }

            val userJid = WppCore.createUserJid(jid)
            if (userJid == null) {
                XposedBridge.log("Scheduler Error: UserJid is null for $jid")
                sendStatusBroadcast(id, false)
                return
            }

            val actionUser = WppCore.getActionUser()
            if (actionUser == null) {
                XposedBridge.log("Scheduler Error: ActionUser is null")
                sendStatusBroadcast(id, false)
                return
            }

            // Disable strict mode checking for file URI
            val oldPolicy = StrictMode.getVmPolicy()
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().build())
            val fileUri = Uri.fromFile(file)
            StrictMode.setVmPolicy(oldPolicy)

            // Dynamically find media sending method in UserAction
            // It typically takes a list of JIDs, a list of URIs, a caption (String), and other optional params
            val mediaMethod = actionUser.javaClass.declaredMethods.find { method ->
                val params = method.parameterTypes
                params.size >= 3 &&
                        List::class.java.isAssignableFrom(params[0]) && // Recipient list (JIDs)
                        (List::class.java.isAssignableFrom(params[1]) || Uri::class.java.isAssignableFrom(params[1])) // URI list or single URI
            }

            if (mediaMethod == null) {
                XposedBridge.log("Scheduler Error: Media sending method not found in ActionUser")
                sendStatusBroadcast(id, false)
                return
            }

            mediaMethod.isAccessible = true
            val args = arrayOfNulls<Any>(mediaMethod.parameterCount)
            for (i in args.indices) {
                val param = mediaMethod.parameterTypes[i]
                args[i] = ReflectionUtils.getDefaultValue(param)
            }

            // Bind JIDs list
            args[0] = Collections.singletonList(userJid)

            // Bind URI(s)
            val uriParamType = mediaMethod.parameterTypes[1]
            if (List::class.java.isAssignableFrom(uriParamType)) {
                args[1] = Collections.singletonList(fileUri)
            } else {
                args[1] = fileUri
            }

            // Bind caption String if present in parameter types
            val captionIndex = mediaMethod.parameterTypes.indices.find { i ->
                i >= 2 && mediaMethod.parameterTypes[i] == String::class.java
            }
            if (captionIndex != null) {
                args[captionIndex] = caption
            }

            // Bind media type if needed (often a byte or int param for IMAGE/VIDEO/DOC)
            // WhatsApp internal methods usually auto-detect from URI, so leaving default is fine,
            // but we can try to bind it if we find an Int parameter
            val intParamIndex = mediaMethod.parameterTypes.indices.find { i ->
                i >= 2 && (mediaMethod.parameterTypes[i] == Int::class.javaPrimitiveType || mediaMethod.parameterTypes[i] == Byte::class.javaPrimitiveType)
            }
            if (intParamIndex != null) {
                val typeVal = when (mediaType) {
                    "VIDEO" -> 2
                    "AUDIO" -> 3
                    "DOCUMENT" -> 9
                    else -> 1 // IMAGE
                }
                if (mediaMethod.parameterTypes[intParamIndex] == Byte::class.javaPrimitiveType) {
                    args[intParamIndex] = typeVal.toByte()
                } else {
                    args[intParamIndex] = typeVal
                }
            }

            mediaMethod.invoke(actionUser, *args)
            XposedBridge.log("Scheduled media message sent successfully to $jid")
            sendStatusBroadcast(id, true)
        } catch (e: Exception) {
            XposedBridge.log("Error sending media: ${e.message}")
            e.printStackTrace()
            sendStatusBroadcast(id, false)
        }
    }

    private fun sendStatusBroadcast(id: Int, success: Boolean) {
        val intent = Intent("com.wmods.wppenhacer.SCHEDULED_STATUS").apply {
            putExtra("id", id)
            putExtra("success", success)
        }
        Utils.application.sendBroadcast(intent)
    }
}
