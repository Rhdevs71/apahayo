package com.rhdevs.rhpatch.revanced.meta.distractionFree

import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.revanced.meta.devkit.MetaUnobfuscator
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers


val HideNotesTray = patch(
    name = "Sembunyikan Notes",
    description = "Menyembunyikan deretan Notes di pesan masuk (Inbox)."
) {
    runCatching {
        if (!MetaUnobfuscator.init(appContext)) return@runCatching
        val methods = MetaUnobfuscator.findMethodUsingStrings("MainFeedInboxNotesTrayBinderGroup", "direct_inbox_notes_tray")
        methods.forEach { method ->
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = null
                }
            })
        }
    }
}

val DisableScreenshotDetection = patch(
    name = "Anti Deteksi Screenshot",
    description = "Mencegah Instagram memberi tahu jika Anda mengambil screenshot di DM."
) {
    runCatching {
        if (!MetaUnobfuscator.init(appContext)) return@runCatching
        val methods = MetaUnobfuscator.findMethodUsingStrings("is_screenshot_detected")
        methods.forEach { method ->
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = false
                }
            })
        }
    }
}

val DisableSwipeToCreate = patch(
    name = "Matikan Geser untuk Kamera",
    description = "Menonaktifkan geser ke kanan untuk membuka kamera."
) {
    runCatching {
        if (!MetaUnobfuscator.init(appContext)) return@runCatching
        // Disable swipe to create container
        val methods = MetaUnobfuscator.findMethodUsingStrings("direct_swipe_to_camera_container")
        methods.forEach { method ->
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val context = android.app.AndroidAppHelper.currentApplication()
                        val prefs = context?.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
                        if (prefs?.getBoolean("pref_disable_swipe", true) == true) {
                            param.result = true // Consumes the touch event so it doesn't swipe
                        }
                    } catch (e: Exception) {}
                }
            })
        }
        
        // Hide Camera Button View (Rhpatch approach)
        val cameraButtonClass = XposedHelpers.findClassIfExists("com.instagram.mainactivity.camerabutton.CameraButtonView", classLoader)
        if (cameraButtonClass != null) {
            for (method in cameraButtonClass.declaredMethods) {
                if (method.name == "setVisibility") {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            try {
                                val context = android.app.AndroidAppHelper.currentApplication()
                                val prefs = context?.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
                                if (prefs?.getBoolean("pref_disable_swipe", true) == true) {
                                    param.args[0] = android.view.View.GONE
                                }
                            } catch (e: Exception) {}
                        }
                    })
                }
            }
        }
    }
}

val DisableVideoAutoplayPatch = patch(
    name = "Matikan Autoplay Video",
    description = "Mencegah video putar otomatis di Feed"
) {
    runCatching {
        if (!MetaUnobfuscator.init(appContext)) return@runCatching
        val methods = MetaUnobfuscator.findMethodUsingStrings("ig_olympus_disable_video_autoplay", "ig_disable_video_autoplay", "ig_video_setting")
        methods.forEach { method ->
            if (method.returnType == Boolean::class.javaPrimitiveType || method.returnType == java.lang.Boolean::class.java) {
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val context = android.app.AndroidAppHelper.currentApplication()
                            val prefs = context?.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
                            if (prefs?.getBoolean("pref_disable_video_autoplay", true) == true) {
                                param.result = true
                            }
                        } catch (e: Exception) {}
                    }
                })
            }
        }
    }.onFailure { XposedBridge.log("Rhpatch: [VideoAutoplay] Patch failed: $it") }
}

val DisableStoriesAudioAutoplayPatch = patch(
    name = "Matikan Autoplay Audio Stories",
    description = "Mencegah audio story putar otomatis"
) {
    runCatching {
        if (!MetaUnobfuscator.init(appContext)) return@runCatching
        val methods = MetaUnobfuscator.findMethodUsingStrings("is_audio_muted_by_user")
        methods.forEach { method ->
            if (method.returnType == Boolean::class.javaPrimitiveType) {
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val context = android.app.AndroidAppHelper.currentApplication()
                            val prefs = context?.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
                            if (prefs?.getBoolean("pref_disable_stories_audio", true) == true) {
                                param.result = true
                            }
                        } catch (e: Exception) {}
                    }
                })
            }
        }
    }.onFailure { XposedBridge.log("Rhpatch: [StoriesAudio] Patch failed: $it") }
}

val DisableDoubleTapLikePatch = patch(
    name = "Matikan 2 Kali untuk Like",
    description = "Mencegah memberikan like dengan ketuk dua kali pada postingan."
) {
    runCatching {
        if (!MetaUnobfuscator.init(appContext)) return@runCatching
        val methods = MetaUnobfuscator.findMethodUsingStrings("open_cmon_interstitial")
        methods.forEach { method ->
            val gestureListenerClass = method.declaringClass
            val onDoubleTapMethods = gestureListenerClass.declaredMethods.filter { it.name == "onDoubleTap" }
            onDoubleTapMethods.forEach { doubleTapMethod ->
                XposedBridge.hookMethod(doubleTapMethod, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val context = android.app.AndroidAppHelper.currentApplication()
                            val prefs = context?.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
                            if (prefs?.getBoolean("pref_disable_double_tap_like", true) == true) {
                                param.result = true // Consume the event
                            }
                        } catch (e: Exception) {}
                    }
                })
            }
        }
    }.onFailure { XposedBridge.log("Rhpatch: [DoubleTapLike] Patch failed: $it") }
}

val DistractionFreePatches = arrayOf(HideNotesTray, DisableScreenshotDetection, DisableSwipeToCreate, DisableVideoAutoplayPatch, DisableStoriesAudioAutoplayPatch, DisableDoubleTapLikePatch)
