package com.rhdevs.rhpatch.xposed.features.media

import android.content.SharedPreferences
import android.view.MotionEvent
import android.view.View
import com.rhdevs.rhpatch.xposed.core.Feature
import com.rhdevs.rhpatch.xposed.core.devkit.Unobfuscator
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.WeakHashMap

class VideoPlaybackTrimmerFix(loader: ClassLoader, preferences: SharedPreferences) :
    Feature(loader, preferences) {

    private class VideoTrimState {
        var startTime: Long = 0L
        var endTime: Long = Long.MAX_VALUE
        var wasTrimmedWhilePaused: Boolean = false
    }

    private val trimStates = WeakHashMap<Any, VideoTrimState>()
    private var cachedPositionField: Field? = null

    override fun doHook() {
        try {
            val fragmentClass = Unobfuscator.loadVideoComposerFragmentClass(classLoader) ?: return
            cachedPositionField = findCachedPositionField(fragmentClass)

            // 1. Hook trim range change method: A2i(long startTime, long endTime)
            val trimMethod = findTrimMethod(fragmentClass)
            if (trimMethod != null) {
                XposedBridge.hookMethod(trimMethod, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val fragment = param.thisObject ?: return
                            val startTime = (param.args.getOrNull(0) as? Long) ?: 0L
                            val endTime = (param.args.getOrNull(1) as? Long) ?: Long.MAX_VALUE

                            val state = trimStates.getOrPut(fragment) { VideoTrimState() }
                            val changed = (state.startTime != startTime || state.endTime != endTime)
                            state.startTime = startTime
                            state.endTime = endTime

                            val player = getPlayer(fragment)
                            val isPlaying = if (player != null) {
                                (XposedHelpers.callMethod(player, "isPlaying") as? Boolean) ?: false
                            } else false

                            if (changed && !isPlaying) {
                                state.wasTrimmedWhilePaused = true

                                // Seek player to startTime and sync cached position so the preview frame
                                // and timeline progress indicator immediately jump to the new cut start
                                if (player != null) {
                                    XposedHelpers.callMethod(player, "seekTo", startTime.toInt())
                                    cachedPositionField?.setLong(fragment, startTime)
                                    getTimelineView(fragment)?.postInvalidate()
                                }
                            }
                        } catch (t: Throwable) {
                            logDebug("trimMethod error", t)
                        }
                    }
                })
            }

            // 2. Hook Play button click method: A2e()
            val playMethod = Unobfuscator.loadVideoComposerPlayMethod(fragmentClass)
                ?: findPlayButtonMethodFallback(fragmentClass)
            if (playMethod != null) {
                XposedBridge.hookMethod(playMethod, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val fragment = param.thisObject ?: return
                            val player = getPlayer(fragment) ?: return
                            val isPlaying = (XposedHelpers.callMethod(player, "isPlaying") as? Boolean) ?: false

                            // Only act if currently paused and about to play
                            if (!isPlaying) {
                                val state = trimStates.getOrPut(fragment) { VideoTrimState() }
                                val currentPosition = (XposedHelpers.callMethod(player, "getCurrentPosition") as? Number)?.toLong() ?: 0L
                                val startTime = state.startTime
                                val endTime = state.endTime

                                val isOutsideBounds = (startTime > 0 && currentPosition < startTime) ||
                                        (endTime > startTime && currentPosition >= (endTime - 500L))

                                if (state.wasTrimmedWhilePaused || isOutsideBounds) {
                                    XposedHelpers.callMethod(player, "seekTo", startTime.toInt())
                                    cachedPositionField?.setLong(fragment, startTime)
                                    getTimelineView(fragment)?.postInvalidate()
                                    state.wasTrimmedWhilePaused = false
                                }
                            }
                        } catch (t: Throwable) {
                            logDebug("playMethod beforeHookedMethod error", t)
                        }
                    }

                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val fragment = param.thisObject ?: return
                            val state = trimStates[fragment]
                            if (state != null) {
                                state.wasTrimmedWhilePaused = false
                            }
                        } catch (t: Throwable) {
                            logDebug("playMethod afterHookedMethod error", t)
                        }
                    }
                })
            }

            // 3. Hook VideoTimelineView.onTouchEvent to sync when touch handle is released
            val timelineClass = Unobfuscator.loadVideoTimelineViewClass(classLoader)
            if (timelineClass != null) {
                val onTouchEventMethod = XposedHelpers.findMethodExactIfExists(
                    timelineClass,
                    "onTouchEvent",
                    MotionEvent::class.java
                )
                if (onTouchEventMethod != null) {
                    XposedBridge.hookMethod(onTouchEventMethod, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            try {
                                val event = param.args[0] as? MotionEvent ?: return
                                val action = event.actionMasked
                                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                                    val timelineView = param.thisObject as? View ?: return
                                    val fragment = getFragmentForTimeline(timelineView) ?: return
                                    val state = trimStates[fragment] ?: return

                                    if (state.wasTrimmedWhilePaused) {
                                        val player = getPlayer(fragment)
                                        if (player != null) {
                                            val isPlaying = (XposedHelpers.callMethod(player, "isPlaying") as? Boolean) ?: false
                                            if (!isPlaying) {
                                                XposedHelpers.callMethod(player, "seekTo", state.startTime.toInt())
                                                cachedPositionField?.setLong(fragment, state.startTime)
                                                timelineView.postInvalidate()
                                            }
                                        }
                                    }
                                }
                            } catch (t: Throwable) {
                                logDebug("timeline onTouchEvent error", t)
                            }
                        }
                    })
                }
            }
        } catch (t: Throwable) {
            log(t)
        }
    }

    private fun findTrimMethod(fragmentClass: Class<*>): Method? {
        try {
            val direct = XposedHelpers.findMethodExactIfExists(
                fragmentClass,
                "A2i",
                java.lang.Long.TYPE,
                java.lang.Long.TYPE
            )
            if (direct != null) return direct
        } catch (_: Throwable) {}

        for (m in fragmentClass.declaredMethods) {
            if (m.parameterTypes.contentEquals(arrayOf(java.lang.Long.TYPE, java.lang.Long.TYPE)) &&
                m.returnType == java.lang.Void.TYPE
            ) {
                return m
            }
        }
        return null
    }

    private fun findPlayButtonMethodFallback(fragmentClass: Class<*>): Method? {
        // Try method with 0 params, void return, whose name starts with 'A2'
        for (m in fragmentClass.declaredMethods) {
            if (m.parameterTypes.isEmpty() && m.returnType == java.lang.Void.TYPE && m.name == "A2e") {
                return m
            }
        }
        return null
    }

    private fun findCachedPositionField(fragmentClass: Class<*>): Field? {
        try {
            val f = XposedHelpers.findFieldIfExists(fragmentClass, "A05")
            if (f != null && f.type == java.lang.Long.TYPE && !Modifier.isStatic(f.modifiers)) {
                f.isAccessible = true
                return f
            }
        } catch (_: Throwable) {}

        for (f in fragmentClass.declaredFields) {
            if (f.type == java.lang.Long.TYPE && !Modifier.isStatic(f.modifiers)) {
                f.isAccessible = true
                return f
            }
        }
        return null
    }

    private fun getPlayer(fragment: Any): Any? {
        try {
            val f = XposedHelpers.findFieldIfExists(fragment.javaClass, "A0U")
            if (f != null) {
                val player = f.get(fragment)
                if (player != null && hasPlayerMethods(player.javaClass)) return player
            }
        } catch (_: Throwable) {}

        for (field in fragment.javaClass.declaredFields) {
            try {
                field.isAccessible = true
                val obj = field.get(fragment) ?: continue
                if (hasPlayerMethods(obj.javaClass)) return obj
            } catch (_: Throwable) {}
        }
        return null
    }

    private fun hasPlayerMethods(clazz: Class<*>): Boolean {
        val methods = clazz.methods
        val hasSeek = methods.any { it.name == "seekTo" && it.parameterTypes.contentEquals(arrayOf(Int::class.javaPrimitiveType)) }
        val hasPos = methods.any { it.name == "getCurrentPosition" && it.returnType == Int::class.javaPrimitiveType }
        val hasPlaying = methods.any { it.name == "isPlaying" && it.returnType == Boolean::class.javaPrimitiveType }
        return hasSeek && hasPos && hasPlaying
    }

    private fun getTimelineView(fragment: Any): View? {
        try {
            val f = XposedHelpers.findFieldIfExists(fragment.javaClass, "A0P")
            if (f != null) {
                val v = f.get(fragment)
                if (v is View) return v
            }
        } catch (_: Throwable) {}

        for (field in fragment.javaClass.declaredFields) {
            try {
                field.isAccessible = true
                val obj = field.get(fragment)
                if (obj is View && obj.javaClass.name.contains("VideoTimelineView")) {
                    return obj
                }
            } catch (_: Throwable) {}
        }
        return null
    }

    private fun getFragmentForTimeline(timelineView: View): Any? {
        return trimStates.keys.firstOrNull { fragment ->
            getTimelineView(fragment) === timelineView
        }
    }

    override fun getPluginName(): String {
        return "Video Playback Trimmer Fix"
    }
}
