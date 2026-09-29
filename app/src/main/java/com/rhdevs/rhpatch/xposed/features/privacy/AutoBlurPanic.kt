package com.rhdevs.rhpatch.xposed.features.privacy

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.rhdevs.rhpatch.R
import com.rhdevs.rhpatch.xposed.core.Feature
import com.rhdevs.rhpatch.xposed.core.WppCore
import com.rhdevs.rhpatch.xposed.core.devkit.Unobfuscator
import com.rhdevs.rhpatch.xposed.utils.Utils
import org.luckypray.dexkit.query.enums.StringMatchType
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import kotlin.math.sqrt

class AutoBlurPanic(loader: ClassLoader, preferences: SharedPreferences) : Feature(loader, preferences) {

    companion object {
        private const val OVERLAY_TAG = "rhpatch_panic_overlay"
        private const val SHAKE_THRESHOLD = 16.0f // m/s^2
    }

    private var sensorManager: SensorManager? = null
    private var accelSensor: Sensor? = null
    private var proximitySensor: Sensor? = null
    private var sensorListener: SensorEventListener? = null
    private var isBlurred = false
    private var lastShakeTime = 0L

    override fun getPluginName(): String {
        return "AutoBlurPanic"
    }

    override fun doHook() {
        if (!prefs.getBoolean("auto_blur_panic_enabled", true)) return

        try {
            val conversationClass = try {
                XposedHelpers.findClass("com.whatsapp.Conversation", classLoader)
            } catch (_: Throwable) {
                Unobfuscator.findFirstClassUsingName(
                    classLoader,
                    StringMatchType.EndsWith,
                    "Conversation"
                )
            }

            if (conversationClass == null) return

            // Hook onResume to register sensors
            XposedHelpers.findAndHookMethod(
                conversationClass,
                "onResume",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as? Activity ?: return
                        setupSensorListener(activity)
                    }
                }
            )

            // Hook onPause to clean up sensors
            XposedHelpers.findAndHookMethod(
                conversationClass,
                "onPause",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as? Activity ?: return
                        unregisterSensors()
                    }
                }
            )

        } catch (e: Throwable) {
            logDebug("Error hooking Conversation for AutoBlurPanic", e)
        }
    }

    private fun setupSensorListener(activity: Activity) {
        try {
            if (sensorManager == null) {
                sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            }
            val sm = sensorManager ?: return

            accelSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            proximitySensor = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)

            sensorListener = object : SensorEventListener {
                private var lastX = 0f
                private var lastY = 0f
                private var lastZ = 0f

                override fun onSensorChanged(event: SensorEvent?) {
                    if (event == null) return

                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]

                        val deltaX = x - lastX
                        val deltaY = y - lastY
                        val deltaZ = z - lastZ

                        lastX = x
                        lastY = y
                        lastZ = z

                        val speed = sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble()).toFloat()
                        val now = System.currentTimeMillis()

                        if (speed > SHAKE_THRESHOLD && now - lastShakeTime > 1500L) {
                            lastShakeTime = now
                            activity.runOnUiThread {
                                togglePanicBlur(activity)
                            }
                        }
                    } else if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
                        val distance = event.values[0]
                        val maxRange = event.sensor.maximumRange
                        // When proximity sensor is fully covered (close to 0)
                        if (distance < maxRange && distance <= 1.5f && !isBlurred) {
                            activity.runOnUiThread {
                                applyBlur(activity)
                            }
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            val trigger = prefs.getString("auto_blur_panic_trigger", "both") ?: "both"
            val enableShake = trigger == "both" || trigger == "shake"
            val enableProximity = trigger == "both" || trigger == "proximity"

            if (enableShake) {
                accelSensor?.let { sm.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI) }
            }
            if (enableProximity) {
                proximitySensor?.let { sm.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_NORMAL) }
            }

        } catch (e: Throwable) {
            logDebug("setupSensorListener error", e)
        }
    }

    private fun unregisterSensors() {
        try {
            sensorListener?.let { sensorManager?.unregisterListener(it) }
            sensorListener = null
        } catch (_: Throwable) {}
    }

    private fun togglePanicBlur(activity: Activity) {
        if (isBlurred) {
            removeBlur(activity)
        } else {
            applyBlur(activity)
        }
    }

    private fun applyBlur(activity: Activity) {
        if (isBlurred || activity.isFinishing || activity.isDestroyed) return
        val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return

        // 1. Android 12+ RenderEffect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                root.setRenderEffect(RenderEffect.createBlurEffect(35f, 35f, Shader.TileMode.CLAMP))
            } catch (_: Throwable) {}
        }

        // 2. Privacy frosted overlay for all Android versions
        if (root.findViewWithTag<View>(OVERLAY_TAG) == null) {
            val overlay = FrameLayout(activity).apply {
                tag = OVERLAY_TAG
                setBackgroundColor(Color.parseColor("#E60B141B")) // WhatsApp dark theme background with opacity
                isClickable = true
                isFocusable = true
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                val contentBox = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                    )

                    val tvIcon = TextView(activity).apply {
                        text = "🔒"
                        textSize = 48f
                        gravity = Gravity.CENTER
                    }
                    addView(tvIcon)

                    val tvTitle = TextView(activity).apply {
                        text = try { Utils.getString(R.string.panic_blur_title) } catch (_: Throwable) { "Mode Panik Aktif" }
                        textSize = 20f
                        setTextColor(Color.WHITE)
                        gravity = Gravity.CENTER
                        setPadding(0, Utils.dipToPixels(12), 0, Utils.dipToPixels(6))
                    }
                    addView(tvTitle)

                    val tvDesc = TextView(activity).apply {
                        text = try { Utils.getString(R.string.panic_blur_desc) } catch (_: Throwable) { "Layar obrolan disamarkan demi privasi.\nKetuk untuk membuka kembali." }
                        textSize = 14f
                        setTextColor(Color.parseColor("#8696A0"))
                        gravity = Gravity.CENTER
                    }
                    addView(tvDesc)
                }

                addView(contentBox)

                setOnClickListener {
                    removeBlur(activity)
                }
            }

            root.addView(overlay)
        }

        isBlurred = true
    }

    private fun removeBlur(activity: Activity) {
        if (!isBlurred) return
        val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                root.setRenderEffect(null)
            } catch (_: Throwable) {}
        }

        val overlay = root.findViewWithTag<View>(OVERLAY_TAG)
        if (overlay != null) {
            root.removeView(overlay)
        }

        isBlurred = false
    }
}
