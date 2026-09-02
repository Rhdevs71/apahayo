package com.rhdevs.rhpatch.meta.privacy

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val GhostModePatch = patch(
    name = "Instagram Ghost Mode",
    description = "Sembunyikan status dilihat pada DM dan Story (Metode Rhpatch)"
) {
    runCatching {
        if (!com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.init(appContext)) return@runCatching

        // Cari metode DMSeenFingerprint milik Rhpatch:
        // Metode public static final void yang memiliki string "mark_thread_seen-"
        val markThreadSeenMethods = com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.findMethodUsingStrings("mark_thread_seen-")

        if (markThreadSeenMethods.isNotEmpty()) {
            for (method in markThreadSeenMethods) {
                val isStatic = java.lang.reflect.Modifier.isStatic(method.modifiers)
                val isFinal = java.lang.reflect.Modifier.isFinal(method.modifiers)
                val isPublic = java.lang.reflect.Modifier.isPublic(method.modifiers)
                // Filter out methods without arguments to avoid breaking internal channel initialization
                if (method.returnType == Void.TYPE && isStatic && isFinal && isPublic && method.parameterTypes.isNotEmpty()) {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            try {
                                val context = android.app.AndroidAppHelper.currentApplication() ?: return
                                val prefs = context.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
                                if (prefs.getBoolean("pref_ghost_mode", true)) {
                                    param.result = null
                                    
                                    if (prefs.getBoolean("pref_hook_tracker", false)) {
                                        android.widget.Toast.makeText(context, "Rhpatch: Ghost Mode DM Aktif!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {}
                        }
                    })
                    break // HANYA hook metode pertama yang cocok (seperti perilaku Piko) untuk mencegah bug Saluran!
                }
            }
        }
    }.onFailure { XposedBridge.log("Rhpatch: [GhostMode] DMSeen hook failed: $it") }

    // Story Seen Hook (Layer 1: Tigon Network Intercept - Piko Style)
    // Memblokir langsung di layer jaringan agar UI tidak macet
    runCatching {
        val tigonClass = XposedHelpers.findClassIfExists("com.instagram.api.tigon.TigonServiceLayer", classLoader)
        if (tigonClass != null) {
            val startRequestMethods = tigonClass.declaredMethods.filter { it.name == "startRequest" }
            for (method in startRequestMethods) {
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val context = android.app.AndroidAppHelper.currentApplication()
                            val prefs = context?.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
                            if (prefs?.getBoolean("pref_ghost_mode", true) == true) {
                                // Ekstrak URL dari argumen (TigonRequest)
                                val urlStr = extractUrlFromTigon(param.args)
                                if (urlStr != null) {
                                    if (urlStr.contains("/api/v2/media/seen/") || urlStr.contains("/api/v1/media/seen/")) {
                                        param.throwable = java.io.IOException("Rhpatch: Blocked Story Seen request")
                                        XposedBridge.log("Rhpatch: [GhostMode] Intercepted and blocked Story Seen on Tigon Layer")
                                    } else if (urlStr.contains("typing_status") || urlStr.contains("send_direct_typing")) {
                                        if (prefs.getBoolean("pref_disable_typing", true)) {
                                            param.throwable = java.io.IOException("Rhpatch: Blocked Typing Status request")
                                            XposedBridge.log("Rhpatch: [GhostMode] Intercepted and blocked Typing Status")
                                        }
                                    } else if (urlStr.contains("mark_thread_seen")) {
                                        // "Tandai sudah dibaca" logic intercept for manual trigger
                                    }
                                }
                            }
                        } catch (e: Exception) {}
                    }
                    
                    private fun extractUrlFromTigon(args: Array<Any?>): String? {
                        for (arg in args) {
                            if (arg == null) continue
                            val str = arg.toString()
                            if (str.contains("media/seen") || str.contains("typing_status") || str.contains("unsend")) return str
                            try {
                                val fields = arg.javaClass.declaredFields
                                for (field in fields) {
                                    field.isAccessible = true
                                    val value = field.get(arg)
                                    if (value != null && value is java.net.URI) {
                                        return value.toString()
                                    }
                                    if (value != null && value is String && value.contains("media/seen")) {
                                        return value
                                    }
                                }
                            } catch (e: Exception) {}
                        }
                        return null
                    }
                })
            }
        }
    }.onFailure { XposedBridge.log("Rhpatch: [GhostMode] Tigon hook failed: $it") }

    // Story Seen Hook (Layer 2: Fallback Rhpatch Method)
    runCatching {
        val methods = com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.findMethodUsingStrings("media/seen/?reel=%s&live_vod=0")
        val validMethods = methods.filter { it.returnType == Boolean::class.javaPrimitiveType || it.returnType == java.lang.Boolean::class.java }
        
        if (validMethods.isNotEmpty()) {
            val targetMethod = validMethods.last() // Menggunakan metode terakhir (Paling akurat)
            if (!java.lang.reflect.Modifier.isAbstract(targetMethod.modifiers)) {
                XposedBridge.hookMethod(targetMethod, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val context = android.app.AndroidAppHelper.currentApplication()
                            val prefs = context?.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
                            if (prefs?.getBoolean("pref_ghost_mode", true) == true) {
                                // Piko mereturn true, sedangkan Rhpatch mereturn false.
                                // Keduanya bisa membatalkan request, tapi jika terjadi bug UI, Layer 1 (Tigon) akan menangani.
                                param.result = false 
                            }
                        } catch (e: Exception) {}
                    }
                })
            }
        }
    }.onFailure { XposedBridge.log("Rhpatch: [GhostMode] Story Seen hook failed: $it") }

}
