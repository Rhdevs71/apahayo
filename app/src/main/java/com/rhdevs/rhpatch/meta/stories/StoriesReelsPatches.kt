package com.rhdevs.rhpatch.meta.stories

import android.view.MotionEvent
import androidx.viewpager2.widget.ViewPager2
import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val DisableReelsScrollingPatch = patch(
    name = "Disable Reels Scrolling",
    description = "Mencegah scroll/swipe secara paksa pada Reels (Rhpatch Parity)"
) {
    runCatching {
        if (!MetaUnobfuscator.init(appContext)) return@runCatching

        // Fingerprint 1: Disable user input on ViewPager2
        val clipsMethods = MetaUnobfuscator.findMethodUsingStrings("ClipsViewPagerImpl_getViewAtIndex")
        if (clipsMethods.isNotEmpty()) {
            val clipsClass = clipsMethods[0].declaringClass
            
            XposedBridge.hookAllConstructors(clipsClass, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val obj = param.thisObject
                        val fields = obj.javaClass.declaredFields
                        for (field in fields) {
                            if (field.type.name == "androidx.viewpager2.widget.ViewPager2") {
                                field.isAccessible = true
                                val viewPager = field.get(obj) as? ViewPager2
                                viewPager?.isUserInputEnabled = false
                                XposedBridge.log("Rhpatch: [Reels] Scrolling disabled on ViewPager2")
                            }
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            })
        }

        // Fingerprint 2: Disable pull-to-refresh on ClipsSwipeRefreshLayout
        val swipeRefreshClass = XposedHelpers.findClassIfExists("instagram.features.clips.viewer.ui.ClipsSwipeRefreshLayout", classLoader)
        if (swipeRefreshClass != null) {
            val onInterceptMethod = swipeRefreshClass.methods.find { it.name == "onInterceptTouchEvent" && it.parameterCount == 1 }
            if (onInterceptMethod != null) {
                XposedBridge.hookMethod(onInterceptMethod, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = false // Disable pull to refresh
                        XposedBridge.log("Rhpatch: [Reels] Pull-to-refresh disabled")
                    }
                })
            }
        }
    }.onFailure { XposedBridge.log("Rhpatch: [Reels] Disable Scrolling Hook failed: $it") }
}

val DisableStoryFlippingPatch = patch(
    name = "Disable Story Flipping",
    description = "Mencegah pindah story otomatis (Rhpatch Parity)"
) {
    runCatching {
        if (!MetaUnobfuscator.init(appContext)) return@runCatching

        val methods = MetaUnobfuscator.findMethodUsingStrings("userSession").filter {
            it.declaringClass.name == "instagram.features.stories.fragment.ReelViewerFragment" &&
            it.parameterTypes.size == 1 &&
            it.returnType == Void.TYPE
        }

        methods.forEach { method ->
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = null // Batalkan pindah story
                    XposedBridge.log("Rhpatch: [Stories] Automatic flipping disabled")
                }
            })
        }
    }.onFailure { XposedBridge.log("Rhpatch: [Stories] Disable Flipping Hook failed: $it") }
}

val StoriesReelsPatches = arrayOf(DisableReelsScrollingPatch, DisableStoryFlippingPatch)
