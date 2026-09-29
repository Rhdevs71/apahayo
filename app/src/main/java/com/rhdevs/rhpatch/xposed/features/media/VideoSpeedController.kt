package com.rhdevs.rhpatch.xposed.features.media

import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.PlaybackParams
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.rhdevs.rhpatch.xposed.core.Feature
import com.rhdevs.rhpatch.xposed.core.devkit.Unobfuscator
import com.rhdevs.rhpatch.xposed.utils.Utils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.util.WeakHashMap

class VideoSpeedController(loader: ClassLoader, preferences: SharedPreferences) : Feature(loader, preferences) {

    companion object {
        private const val SPEED_BTN_TAG = "rhpatch_video_speed_btn"
        private val SPEEDS = floatArrayOf(1.0f, 1.5f, 2.0f, 0.5f)
    }

    private val fragmentSpeedMap = WeakHashMap<Any, Float>()

    override fun getPluginName(): String {
        return "VideoSpeedController"
    }

    override fun doHook() {
        if (!prefs.getBoolean("video_speed_controller_enabled", true)) return

        try {
            val fragmentClass = Unobfuscator.loadVideoComposerFragmentClass(classLoader) ?: return

            // Hook onViewCreated to inject Speed Button into the video composer screen
            XposedHelpers.findAndHookMethod(
                fragmentClass,
                "onViewCreated",
                View::class.java,
                Bundle::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val fragment = param.thisObject ?: return
                            val rootView = param.args[0] as? ViewGroup ?: return
                            injectSpeedButton(fragment, rootView)
                        } catch (t: Throwable) {
                            logDebug("onViewCreated hook error", t)
                        }
                    }
                }
            )

            // Hook play method to ensure selected playback speed is preserved when played
            val playMethod = Unobfuscator.loadVideoComposerPlayMethod(fragmentClass)
            if (playMethod != null) {
                XposedBridge.hookMethod(playMethod, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val fragment = param.thisObject ?: return
                            val speed = fragmentSpeedMap[fragment] ?: 1.0f
                            if (speed != 1.0f) {
                                applySpeedToPlayer(fragment, speed)
                            }
                        } catch (t: Throwable) {
                            logDebug("playMethod afterHookedMethod error", t)
                        }
                    }
                })
            }

        } catch (t: Throwable) {
            logDebug("doHook error in VideoSpeedController", t)
        }
    }

    private fun injectSpeedButton(fragment: Any, root: ViewGroup) {
        if (root.findViewWithTag<View>(SPEED_BTN_TAG) != null) return

        val context = root.context
        val speedBtn = TextView(context).apply {
            tag = SPEED_BTN_TAG
            val currentSpeed = fragmentSpeedMap[fragment] ?: 1.0f
            text = "${currentSpeed}x"
            textSize = 13f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER

            val padH = Utils.dipToPixels(12)
            val padV = Utils.dipToPixels(6)
            setPadding(padH, padV, padH, padV)

            background = GradientDrawable().apply {
                setColor(Color.parseColor("#80000000"))
                cornerRadius = Utils.dipToPixels(16).toFloat()
                setStroke(Utils.dipToPixels(1), Color.parseColor("#55FFFFFF"))
            }

            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                topMargin = Utils.dipToPixels(64)
                leftMargin = Utils.dipToPixels(16)
            }

            setOnClickListener {
                val activeSpeed = fragmentSpeedMap[fragment] ?: 1.0f
                var nextIndex = 0
                for (i in SPEEDS.indices) {
                    if (SPEEDS[i] == activeSpeed) {
                        nextIndex = (i + 1) % SPEEDS.size
                        break
                    }
                }
                val newSpeed = SPEEDS[nextIndex]
                fragmentSpeedMap[fragment] = newSpeed
                text = "${newSpeed}x"
                applySpeedToPlayer(fragment, newSpeed)
            }
        }

        root.addView(speedBtn)
    }

    private fun applySpeedToPlayer(fragment: Any, speed: Float) {
        try {
            val player = getPlayer(fragment) ?: return

            // 1. Android MediaPlayer (API 23+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val getParamsMethod = player.javaClass.methods.firstOrNull { it.name == "getPlaybackParams" }
                    if (getParamsMethod != null) {
                        val params = getParamsMethod.invoke(player) as? PlaybackParams ?: PlaybackParams()
                        params.speed = speed
                        val setParamsMethod = player.javaClass.methods.firstOrNull {
                            it.name == "setPlaybackParams" && it.parameterTypes.contentEquals(arrayOf(PlaybackParams::class.java))
                        }
                        setParamsMethod?.invoke(player, params)
                        return
                    }
                } catch (_: Throwable) {}
            }

            // 2. ExoPlayer PlaybackParameters
            try {
                val playbackParamsClass = XposedHelpers.findClassIfExists("com.google.android.exoplayer2.PlaybackParameters", fragment.javaClass.classLoader)
                    ?: XposedHelpers.findClassIfExists("androidx.media3.common.PlaybackParameters", fragment.javaClass.classLoader)
                if (playbackParamsClass != null) {
                    val constructor = playbackParamsClass.getConstructor(java.lang.Float.TYPE)
                    val paramsInstance = constructor.newInstance(speed)
                    val setMethod = player.javaClass.methods.firstOrNull {
                        it.parameterCount == 1 && it.parameterTypes[0] == playbackParamsClass
                    }
                    setMethod?.invoke(player, paramsInstance)
                    return
                }
            } catch (_: Throwable) {}

            // 3. Direct setSpeed / setPlaybackSpeed
            try {
                XposedHelpers.callMethod(player, "setPlaybackSpeed", speed)
            } catch (_: Throwable) {
                try {
                    XposedHelpers.callMethod(player, "setSpeed", speed)
                } catch (_: Throwable) {}
            }

        } catch (t: Throwable) {
            logDebug("applySpeedToPlayer error", t)
        }
    }

    private fun getPlayer(fragment: Any): Any? {
        return try {
            XposedHelpers.getObjectField(fragment, "A0N")
        } catch (_: Throwable) {
            try {
                fragment.javaClass.declaredFields.firstOrNull { field ->
                    field.type.name.contains("player", ignoreCase = true) ||
                            field.type.name.contains("video", ignoreCase = true)
                }?.let {
                    it.isAccessible = true
                    it.get(fragment)
                }
            } catch (_: Throwable) {
                null
            }
        }
    }
}
