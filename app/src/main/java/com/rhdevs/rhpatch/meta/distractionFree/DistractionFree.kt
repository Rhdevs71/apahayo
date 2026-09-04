package com.rhdevs.rhpatch.meta.distractionFree

import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator
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
        val methods = MetaUnobfuscator.findMethodUsingStrings("autoplay_disabled", "is_autoplay_enabled", "video_autoplay", "autoplay", "ig_olympus_disable_video_autoplay")
        methods.forEach { method ->
            if (method.returnType == Boolean::class.javaPrimitiveType || method.returnType == java.lang.Boolean::class.java) {
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val context = android.app.AndroidAppHelper.currentApplication()
                            val prefs = context?.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
                            if (prefs?.getBoolean("pref_disable_video_autoplay", false) == true) {
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
                            param.result = true // Consume the event
                        } catch (e: Exception) {}
                    }
                })
            }
        }
    }.onFailure { XposedBridge.log("Rhpatch: [DoubleTapLike] Patch failed: $it") }
}

val HideSuggestedUsersPatch = patch(
    name = "Hide Suggested Users",
    description = "Menyembunyikan saran pengguna / profil orang di Feed."
) {
    runCatching {
        XposedHelpers.findAndHookMethod(
            android.widget.TextView::class.java,
            "setText",
            CharSequence::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val text = (param.args[0] as? CharSequence)?.toString()?.trim() ?: return
                    
                    // Deteksi teks "Suggested for you" atau "Saran untuk Anda"
                    if (text.equals("Suggested for you", ignoreCase = true) || 
                        text.equals("Saran untuk Anda", ignoreCase = true) ||
                        text.equals("Suggested users", ignoreCase = true)) {
                        
                        val view = param.thisObject as? android.view.View ?: return
                        val prefs = view.context.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
                        if (!prefs.getBoolean("pref_hide_suggested_users", true)) return

                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            runCatching { 
                                var current: android.view.View? = view
                                var candidate: android.view.View? = null
                                repeat(20) {
                                    val parent = current?.parent ?: return@repeat
                                    if (parent.javaClass.name.contains("RecyclerView", ignoreCase = true)) {
                                        candidate?.let { item ->
                                            item.visibility = android.view.View.GONE
                                            item.layoutParams?.let { lp ->
                                                lp.height = 0
                                                item.layoutParams = lp
                                            }
                                        }
                                        return@post
                                    }
                                    candidate = current
                                    current = parent as? android.view.View
                                }
                            }
                        }
                    }
                }
            }
        )
    }.onFailure { XposedBridge.log("Rhpatch: [HideSuggestedUsers] Patch failed: $it") }
}


val RemoveEmptyBottomSpace = patch(
    name = "Hapus ruang kosong di bagian bawah",
    description = "Removes empty space below bottom navigation bar."
) {
    runCatching {
        if (!MetaUnobfuscator.init(appContext)) return@runCatching
        val methods = MetaUnobfuscator.findMethodUsingStrings("config_showNavigationBar", "_hasNavigationBar_notFound")
        
        for (method in methods) {
            if (method.returnType == Boolean::class.javaPrimitiveType || method.returnType == java.lang.Boolean::class.java) {
                XposedBridge.hookMethod(method, object : de.robv.android.xposed.XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val context = android.app.AndroidAppHelper.currentApplication()
                            val prefs = context?.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
                            if (prefs?.getBoolean("pref_remove_bottom_space", true) == true) {
                                param.result = false
                            }
                        } catch (e: Exception) {}
                    }
                })
            }
        }
    }.onFailure { de.robv.android.xposed.XposedBridge.log("Rhpatch: [RemoveEmptyBottomSpace] Patch failed: it") }
}

val DistractionFreePatches = arrayOf(RemoveEmptyBottomSpace, HideNotesTray, DisableScreenshotDetection, DisableSwipeToCreate, DisableVideoAutoplayPatch, DisableStoriesAudioAutoplayPatch, DisableDoubleTapLikePatch, HideSuggestedUsersPatch)




