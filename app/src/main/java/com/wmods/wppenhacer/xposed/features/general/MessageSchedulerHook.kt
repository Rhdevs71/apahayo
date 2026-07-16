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
import com.wmods.wppenhacer.xposed.core.devkit.Unobfuscator
import com.wmods.wppenhacer.xposed.core.Feature
import com.wmods.wppenhacer.xposed.core.WppCore
import com.wmods.wppenhacer.xposed.utils.ReflectionUtils
import com.wmods.wppenhacer.xposed.utils.Utils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.io.File
import java.util.Collections

class MessageSchedulerHook(loader: ClassLoader, preferences: SharedPreferences) : Feature(loader, preferences) {

    override fun getPluginName(): String {
        return "MessageSchedulerHook"
    }

    override fun doHook() {
        XposedBridge.log("WaEnhancer MessageSchedulerHook: Hooking scheduler receiver in WhatsApp")
        registerSchedulerReceiver()
        hookWhatsAppSync()
    }

    private fun hookWhatsAppSync() {
        try {
            val waJobManagerMethod = Unobfuscator.loadBlueOnReplayWaJobManagerMethod(classLoader)
            XposedBridge.hookMethod(waJobManagerMethod, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    // Send a broadcast to trigger our SchedulerReceiver and wake up the service
                    val intent = Intent("com.wmods.wppenhacer.TRIGGER_ALARM").apply {
                        `package` = BuildConfig.APPLICATION_ID
                    }
                    Utils.application.sendBroadcast(intent)
                }
            })
            XposedBridge.log("WaEnhancer MessageSchedulerHook: Hooked WaJobManager for periodic background triggers")
        } catch (e: Exception) {
            XposedBridge.log("WaEnhancer MessageSchedulerHook Error: Failed to hook WaJobManager: ${e.message}")
        }
    }

    @SuppressLint("WrongConstant")
    private fun registerSchedulerReceiver() {
        val filter = IntentFilter("com.wmods.wppenhacer.SCHEDULED_SEND")
        // Remove the signature broadcast permission filter so that the broadcast is successfully received across sandboxes
        Utils.application.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val id = intent.getIntExtra("id", -1)
                    val jid = intent.getStringExtra("jid") ?: return
                    val messageText = intent.getStringExtra("messageText") ?: ""
                    val mediaPath = intent.getStringExtra("mediaPath")
                    val mediaType = intent.getStringExtra("mediaType")

                    XposedBridge.log("WaEnhancer MessageSchedulerHook onReceive: Message trigger received for id $id, JID: $jid")

                    Handler(Looper.getMainLooper()).post {
                        try {
                            if (mediaPath != null && mediaPath.isNotEmpty()) {
                                XposedBridge.log("WaEnhancer MessageSchedulerHook onReceive: Processing media message for id $id")
                                sendMediaMessage(jid, messageText, mediaPath, mediaType, id)
                            } else {
                                XposedBridge.log("WaEnhancer MessageSchedulerHook onReceive: Processing text message for id $id")
                                sendTextMessage(jid, messageText, id)
                            }
                        } catch (e: Exception) {
                            XposedBridge.log("WaEnhancer MessageSchedulerHook Error: Failed to process message $id: ${e.message}")
                            e.printStackTrace()
                            sendStatusBroadcast(id, false)
                        }
                    }
                }
            },
            filter,
            null, // Clear permission string
            null,
            if (android.os.Build.VERSION.SDK_INT >= 33) 2 else 0 // 2 is Context.RECEIVER_EXPORTED
        )
        XposedBridge.log("WaEnhancer MessageSchedulerHook: Receiver registered successfully")
    }

    private fun sendTextMessage(jid: String, text: String, id: Int) {
        try {
            val userJid = WppCore.createUserJid(jid)
            if (userJid == null) {
                XposedBridge.log("WaEnhancer MessageSchedulerHook: UserJid is null for $jid")
                sendStatusBroadcast(id, false)
                return
            }

            XposedBridge.log("WaEnhancer MessageSchedulerHook: Calling WppCore.sendMessageToJid")
            WppCore.sendMessageToJid(userJid, text)
            sendStatusBroadcast(id, true)
        } catch (e: Exception) {
            XposedBridge.log("WaEnhancer MessageSchedulerHook Error: sending text failed: ${e.message}")
            e.printStackTrace()
            sendStatusBroadcast(id, false)
        }
    }

    private fun sendMediaMessage(jid: String, caption: String, mediaPath: String, mediaType: String?, id: Int) {
        try {
            val file = File(mediaPath)
            if (!file.exists()) {
                XposedBridge.log("WaEnhancer MessageSchedulerHook: Media file does not exist on disk: $mediaPath")
                sendStatusBroadcast(id, false)
                return
            }

            val userJid = WppCore.createUserJid(jid)
            if (userJid == null) {
                XposedBridge.log("WaEnhancer MessageSchedulerHook: UserJid is null for $jid")
                sendStatusBroadcast(id, false)
                return
            }

            val actionUser = WppCore.getActionUser()
            val actionUserClass = WppCore.getActionUserClass()
            if (actionUser == null || actionUserClass == null) {
                XposedBridge.log("WaEnhancer MessageSchedulerHook: ActionUser instance or class is null")
                sendStatusBroadcast(id, false)
                return
            }

            val oldPolicy = StrictMode.getVmPolicy()
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().build())
            val fileUri = Uri.fromFile(file)
            StrictMode.setVmPolicy(oldPolicy)

            // Dynamically find media sending method in UserAction class
            val mediaMethod = actionUserClass.declaredMethods.find { method ->
                val params = method.parameterTypes
                params.size >= 3 &&
                        List::class.java.isAssignableFrom(params[0]) &&
                        (List::class.java.isAssignableFrom(params[1]) || Uri::class.java.isAssignableFrom(params[1]))
            }

            if (mediaMethod == null) {
                XposedBridge.log("WaEnhancer MessageSchedulerHook: Media sending method not found in ActionUser")
                sendStatusBroadcast(id, false)
                return
            }

            mediaMethod.isAccessible = true
            val args = arrayOfNulls<Any>(mediaMethod.parameterCount)
            for (i in args.indices) {
                val param = mediaMethod.parameterTypes[i]
                args[i] = ReflectionUtils.getDefaultValue(param)
            }

            args[0] = Collections.singletonList(userJid)

            val uriParamType = mediaMethod.parameterTypes[1]
            if (List::class.java.isAssignableFrom(uriParamType)) {
                args[1] = Collections.singletonList(fileUri)
            } else {
                args[1] = fileUri
            }

            val captionIndex = mediaMethod.parameterTypes.indices.find { i ->
                i >= 2 && mediaMethod.parameterTypes[i] == String::class.java
            }
            if (captionIndex != null) {
                args[captionIndex] = caption
            }

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
            XposedBridge.log("WaEnhancer MessageSchedulerHook: Media message id $id sent to $jid successfully")
            sendStatusBroadcast(id, true)
        } catch (e: Exception) {
            XposedBridge.log("WaEnhancer MessageSchedulerHook Error: sending media failed: ${e.message}")
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
