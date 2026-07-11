package com.wmods.wppenhacer.xposed.features.general

import android.content.SharedPreferences
import com.wmods.wppenhacer.xposed.core.Feature
import com.wmods.wppenhacer.xposed.utils.AudioOpusConverter
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.io.File

class VoiceChangerHook(loader: ClassLoader, preferences: SharedPreferences) : Feature(loader, preferences) {

    override fun getPluginName(): String {
        return "VoiceChangerHook"
    }

    override fun doHook() {
        val enabled = prefs.getBoolean("voice_changer_enabled", false)
        if (!enabled) return

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
                    val path = XposedHelpers.getAdditionalInstanceField(param.thisObject, "voice_file_path") as? String ?: return
                    val voiceChangerType = prefs.getString("voice_changer_type", "NONE") ?: "NONE"
                    if (voiceChangerType == "NONE") return

                    XposedBridge.log("VoiceChangerHook stop: modifying voice note at $path with effect $voiceChangerType")

                    val pitchFactor = when (voiceChangerType) {
                        "CHIPMUNK" -> 1.4f
                        "DEEP" -> 0.7f
                        "SLOW" -> 0.5f
                        "FAST" -> 1.5f
                        else -> 1.0f
                    }

                    val originalFile = File(path)
                    if (originalFile.exists()) {
                        val tempOut = AudioOpusConverter.convert(path, pitchFactor)
                        if (tempOut != null && tempOut.exists()) {
                            originalFile.delete()
                            tempOut.renameTo(originalFile)
                            XposedBridge.log("VoiceChangerHook stop: voice note modified successfully")
                        } else {
                            XposedBridge.log("VoiceChangerHook stop: conversion returned null or missing file")
                        }
                    }
                }
            })
        } catch (e: Exception) {
            XposedBridge.log("VoiceChangerHook Error: ${e.message}")
        }
    }
}
