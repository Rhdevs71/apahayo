package com.wmods.wppenhacer.xposed.features.general

import android.content.SharedPreferences
import android.media.MediaRecorder
import com.wmods.wppenhacer.xposed.core.Feature
import com.wmods.wppenhacer.xposed.utils.AudioOpusConverter
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.io.File
import java.io.FileDescriptor

class VoiceChangerHook(loader: ClassLoader, preferences: SharedPreferences) : Feature(loader, preferences) {

    override fun getPluginName(): String {
        return "VoiceChangerHook"
    }

    override fun doHook() {
        val enabled = prefs.getBoolean("voice_changer_enabled", false)
        if (!enabled) return

        XposedBridge.log("Rhpatch VoiceChangerHook: Initializing hooks")

        // 1. Hook standard MediaRecorder for newer WhatsApp versions
        try {
            XposedHelpers.findAndHookMethod(
                MediaRecorder::class.java,
                "setOutputFile",
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val path = param.args[0] as String
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "voice_file_path", path)
                    }
                }
            )

            XposedHelpers.findAndHookMethod(
                MediaRecorder::class.java,
                "setOutputFile",
                File::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val file = param.args[0] as File
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "voice_file_path", file.absolutePath)
                    }
                }
            )

            XposedHelpers.findAndHookMethod(
                MediaRecorder::class.java,
                "stop",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val path = XposedHelpers.getAdditionalInstanceField(param.thisObject, "voice_file_path") as? String
                        XposedBridge.log("Rhpatch VoiceChangerHook: MediaRecorder stop triggered for path: $path")
                        if (path != null) {
                            applyVoiceChanger(path)
                        }
                    }
                }
            )
            XposedBridge.log("Rhpatch VoiceChangerHook: Hooked android.media.MediaRecorder successfully")
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch VoiceChangerHook: Failed to hook MediaRecorder: ${e.message}")
        }

        // 2. Hook com.whatsapp.util.OpusRecorder (wrapped in Throwable catch to prevent ClassNotFound warning popup)
        try {
            val opusRecorderClass = XposedHelpers.findClass("com.whatsapp.util.OpusRecorder", classLoader)
            XposedBridge.hookAllConstructors(opusRecorderClass, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val fileObj = param.args[0]
                    val path = if (fileObj is File) fileObj.absolutePath else fileObj.toString()
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, "voice_file_path", path)
                }
            })

            XposedHelpers.findAndHookMethod(opusRecorderClass, "stop", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val path = XposedHelpers.getAdditionalInstanceField(param.thisObject, "voice_file_path") as? String
                    XposedBridge.log("Rhpatch VoiceChangerHook: OpusRecorder stop triggered for path: $path")
                    if (path != null) {
                        applyVoiceChanger(path)
                    }
                }
            })
            XposedBridge.log("Rhpatch VoiceChangerHook: Hooked com.whatsapp.util.OpusRecorder successfully")
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch VoiceChangerHook: OpusRecorder not found in this WhatsApp build (silent skip): ${e.message}")
        }
    }

    private fun applyVoiceChanger(path: String) {
        try {
            val voiceChangerType = prefs.getString("voice_changer_type", "NONE") ?: "NONE"
            if (voiceChangerType == "NONE") return

            val pitchFactor = when (voiceChangerType) {
                "CHIPMUNK" -> 1.4f
                "DEEP" -> 0.7f
                "SLOW" -> 0.5f
                "FAST" -> 1.5f
                else -> 1.0f
            }

            val originalFile = File(path)
            if (originalFile.exists()) {
                XposedBridge.log("Rhpatch VoiceChangerHook: Modifying audio pitch factor $pitchFactor for $path")
                val tempOut = AudioOpusConverter.convert(path, pitchFactor)
                if (tempOut != null && tempOut.exists()) {
                    originalFile.delete()
                    tempOut.renameTo(originalFile)
                    XposedBridge.log("Rhpatch VoiceChangerHook: Pitch modification successful")
                } else {
                    XposedBridge.log("Rhpatch VoiceChangerHook: Temp output file was not created")
                }
            }
        } catch (e: Exception) {
            XposedBridge.log("Rhpatch VoiceChangerHook Error: ${e.message}")
            e.printStackTrace()
        }
    }
}
