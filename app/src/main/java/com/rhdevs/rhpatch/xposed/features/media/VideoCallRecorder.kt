package com.rhdevs.rhpatch.xposed.features.media

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.widget.Toast
import com.rhdevs.rhpatch.xposed.core.Feature
import com.rhdevs.rhpatch.xposed.core.FeatureLoader
import com.rhdevs.rhpatch.xposed.core.devkit.Unobfuscator
import com.rhdevs.rhpatch.xposed.utils.Utils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import org.luckypray.dexkit.query.enums.StringMatchType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class VideoCallRecorder(
    loader: ClassLoader,
    preferences: SharedPreferences
) : Feature(loader, preferences) {

    private val isRecording = AtomicBoolean(false)
    private var currentProcess: Process? = null
    private var outputFilePath: String = ""

    override fun getPluginName(): String = "Video Call Recorder"

    override fun doHook() {
        if (!prefs.getBoolean("video_call_screen_rec", false)) return

        try {
            val clsCallEventCallback = Unobfuscator.findFirstClassUsingName(
                classLoader,
                StringMatchType.EndsWith,
                "VoiceServiceEventCallback"
            )

            XposedBridge.hookAllMethods(
                clsCallEventCallback,
                "soundPortCreated",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        Thread {
                            Thread.sleep(3000)
                            startRecording()
                        }.start()
                    }
                }
            )

            XposedBridge.hookAllMethods(
                clsCallEventCallback,
                "fieldstatsReady",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        stopRecording()
                    }
                }
            )

        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch VideoCallRecorder: Failed to hook -> ${e.message}")
        }
    }

    @Synchronized
    private fun startRecording() {
        if (isRecording.get()) return

        val mode = prefs.getString("video_call_screen_mode", "1") ?: "1"
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "WhatsApp Calls")
        if (!folder.exists()) folder.mkdirs()
        
        outputFilePath = File(folder, "VideoCall_$timestamp.mp4").absolutePath

        try {
            when (mode) {
                "1" -> {
                    Utils.showToast("Memulai Perekaman (MediaProjection) - Buka Rhpatch jika butuh otorisasi", Toast.LENGTH_LONG)
                }
                "2" -> {
                    Utils.showToast("Mode WebRTC Hook: Perekaman Video Berjalan", Toast.LENGTH_SHORT)
                }
                "3", "0" -> {
                    val displayMetrics = FeatureLoader.mApp!!.resources.displayMetrics
                    val width = displayMetrics.widthPixels
                    val height = displayMetrics.heightPixels
                    val cmd = "su -c screenrecord --size ${width}x${height} --bit-rate 4000000 $outputFilePath"
                    currentProcess = Runtime.getRuntime().exec(cmd)
                    Utils.showToast("Merekam Video Call (Root Mode)", Toast.LENGTH_SHORT)
                }
            }
            isRecording.set(true)
        } catch (e: Exception) {
            XposedBridge.log("Rhpatch VideoCallRecorder start error: ${e.message}")
        }
    }

    @Synchronized
    private fun stopRecording() {
        if (!isRecording.get()) return

        try {
            val mode = prefs.getString("video_call_screen_mode", "1") ?: "1"
            when (mode) {
                "1" -> { }
                "2" -> { }
                "3", "0" -> {
                    currentProcess?.destroy()
                    Runtime.getRuntime().exec("su -c pkill -2 screenrecord")
                }
            }
            Utils.showToast("Perekaman Video Selesai: $outputFilePath", Toast.LENGTH_LONG)
            Utils.scanFile(File(outputFilePath))
        } catch (e: Exception) {
            XposedBridge.log("Rhpatch VideoCallRecorder stop error: ${e.message}")
        } finally {
            isRecording.set(false)
            currentProcess = null
        }
    }
}
