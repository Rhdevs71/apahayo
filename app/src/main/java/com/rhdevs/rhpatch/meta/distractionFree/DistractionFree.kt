package com.rhdevs.rhpatch.meta.distractionFree

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.Window
import android.view.WindowManager
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
        // Universal View Hook for Notes Tray (cf_hub_recycler_view)
        XposedBridge.hookAllMethods(View::class.java, "onAttachedToWindow", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val view = param.thisObject as? View ?: return
                    val context = view.context ?: return
                    val prefs = context.getSharedPreferences("rhpatch_settings", Context.MODE_PRIVATE)
                    if (!prefs.getBoolean("pref_hide_notes", true)) return

                    val res = view.resources ?: return
                    val id = view.id
                    if (id != View.NO_ID && runCatching { res.getResourceEntryName(id) }.getOrNull() == "cf_hub_recycler_view") {
                        view.visibility = View.GONE
                        val lp = view.layoutParams
                        if (lp != null) {
                            lp.height = 0
                            view.layoutParams = lp
                        }
                    }
                } catch (_: Throwable) {}
            }
        })
    }.onFailure { XposedBridge.log("Rhpatch: [HideNotesTray] Patch failed: $it") }
}

val DisableScreenshotDetection = patch(
    name = "Anti Deteksi Screenshot",
    description = "Mencegah Instagram memberi tahu jika Anda mengambil screenshot di DM & Buka blokir FLAG_SECURE."
) {
    runCatching {
        // 1. Strip FLAG_SECURE from Window.setFlags
        XposedBridge.hookAllMethods(Window::class.java, "setFlags", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                try {
                    val flags = param.args[0] as? Int ?: return
                    val mask = param.args[1] as? Int ?: return
                    if ((mask and WindowManager.LayoutParams.FLAG_SECURE) != 0) {
                        param.args[0] = flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                    }
                } catch (_: Throwable) {}
            }
        })

        // 2. Strip FLAG_SECURE from Window.addFlags
        XposedBridge.hookAllMethods(Window::class.java, "addFlags", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                try {
                    val flags = param.args[0] as? Int ?: return
                    if ((flags and WindowManager.LayoutParams.FLAG_SECURE) != 0) {
                        param.args[0] = flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                    }
                } catch (_: Throwable) {}
            }
        })

        // 3. Strip FLAG_SECURE from Window.setAttributes (for dialogs & view-once windows)
        XposedBridge.hookAllMethods(Window::class.java, "setAttributes", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                try {
                    val lp = param.args[0] as? WindowManager.LayoutParams ?: return
                    lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                } catch (_: Throwable) {}
            }
        })

        // 4. Force SurfaceView.setSecure to false (crucial for ephemeral media & video players)
        XposedBridge.hookAllMethods(android.view.SurfaceView::class.java, "setSecure", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                try {
                    param.args[0] = false
                } catch (_: Throwable) {}
            }
        })

        // 5. Clear FLAG_SECURE in Activity.onResume and onWindowFocusChanged
        XposedBridge.hookAllMethods(Activity::class.java, "onResume", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val activity = param.thisObject as? Activity ?: return
                    activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } catch (_: Throwable) {}
            }
        })
        XposedBridge.hookAllMethods(Activity::class.java, "onWindowFocusChanged", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val activity = param.thisObject as? Activity ?: return
                    activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } catch (_: Throwable) {}
            }
        })

        // 6. Neutralize Android 14/15 native ScreenCaptureCallback
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            runCatching {
                val m = Activity::class.java.getDeclaredMethod(
                    "registerScreenCaptureCallback",
                    java.util.concurrent.Executor::class.java,
                    Activity.ScreenCaptureCallback::class.java
                )
                XposedBridge.hookMethod(m, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = null // Block callback registration
                    }
                })
            }
        }

        // 7. Hook internal screenshot detection methods
        if (MetaUnobfuscator.init(appContext)) {
            val methods = MetaUnobfuscator.findMethodUsingStrings("is_screenshot_detected")
            methods.forEach { method ->
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = false
                    }
                })
            }
        }
    }.onFailure { XposedBridge.log("Rhpatch: [DisableScreenshotDetection] Patch failed: $it") }
}

val DisableSwipeToCreate = patch(
    name = "Matikan Geser untuk Kamera",
    description = "Menonaktifkan geser ke kanan untuk membuka kamera."
) {
    runCatching {
        if (!MetaUnobfuscator.init(appContext)) return@runCatching
        val methods = MetaUnobfuscator.findMethodUsingStrings("direct_swipe_to_camera_container")
        methods.forEach { method ->
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val context = android.app.AndroidAppHelper.currentApplication()
                        val prefs = context?.getSharedPreferences("rhpatch_settings", Context.MODE_PRIVATE)
                        if (prefs?.getBoolean("pref_disable_swipe_to_create", true) == true) {
                            param.result = null
                        }
                    } catch (e: Exception) {}
                }
            })
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
                            val prefs = context?.getSharedPreferences("rhpatch_settings", Context.MODE_PRIVATE)
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
                            val prefs = context?.getSharedPreferences("rhpatch_settings", Context.MODE_PRIVATE)
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
                    
                    if (text.equals("Suggested for you", ignoreCase = true) || 
                        text.equals("Saran untuk Anda", ignoreCase = true) ||
                        text.equals("Suggested users", ignoreCase = true)) {
                        
                        val view = param.thisObject as? View ?: return
                        val prefs = view.context.getSharedPreferences("rhpatch_settings", Context.MODE_PRIVATE)
                        if (!prefs.getBoolean("pref_hide_suggested_users", true)) return

                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            runCatching { 
                                var current: View? = view
                                var candidate: View? = null
                                repeat(20) {
                                    val parent = current?.parent ?: return@repeat
                                    if (parent.javaClass.name.contains("RecyclerView", ignoreCase = true)) {
                                        candidate?.let { item ->
                                            item.visibility = View.GONE
                                            item.layoutParams?.let { lp ->
                                                lp.height = 0
                                                item.layoutParams = lp
                                            }
                                        }
                                        return@post
                                    }
                                    candidate = current
                                    current = parent as? View
                                }
                            }
                        }
                    }
                }
            }
        )
    }.onFailure { XposedBridge.log("Rhpatch: [HideSuggestedUsers] Patch failed: $it") }
}

// Consolidated: RemoveEmptyBottomSpace is registered in RemoveEmptyBottomSpacePatch.kt
val DistractionFreePatches = arrayOf(
    HideNotesTray,
    DisableScreenshotDetection,
    DisableSwipeToCreate,
    DisableVideoAutoplayPatch,
    DisableStoriesAudioAutoplayPatch,
    DisableDoubleTapLikePatch,
    HideSuggestedUsersPatch
)
