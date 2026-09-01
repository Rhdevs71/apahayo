package com.rhdevs.rhpatch.revanced.meta.distractionFree

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

val DisableVideoAutoplayPatch = patch(
    name = "Disable Video Autoplay",
    description = "Mencegah video berputar otomatis di beranda (Piko Parity)"
) {
    runCatching {
        if (!com.rhdevs.rhpatch.revanced.meta.devkit.MetaUnobfuscator.init(appContext)) return@runCatching
        val targetStrings = listOf("ig_olympus_disable_video_autoplay", "ig_disable_video_autoplay")
        var methods = emptyList<java.lang.reflect.Method>()
        for (str in targetStrings) {
            methods = com.rhdevs.rhpatch.revanced.meta.devkit.MetaUnobfuscator.findMethodUsingStrings(str)
            methods = methods.filter { it.returnType == Boolean::class.javaPrimitiveType || it.returnType == java.lang.Boolean::class.java }
            if (methods.isNotEmpty()) break
        }
        var hooked = false
        methods.forEach { method ->
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
            hooked = true
        }
        if (hooked) {
            XposedBridge.log("Rhpatch: [DisableVideoAutoplay] Hooked successfully.")
        } else {
            XposedBridge.log("Rhpatch: [DisableVideoAutoplay] Failed to find target boolean method.")
        }
    }.onFailure { XposedBridge.log("Rhpatch: [DisableVideoAutoplay] Patch failed: $it") }
}
