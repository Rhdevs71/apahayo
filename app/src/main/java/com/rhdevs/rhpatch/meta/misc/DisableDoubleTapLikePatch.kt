package com.rhdevs.rhpatch.meta.misc

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

val DisableDoubleTapLikePatch = patch(
    name = "Disable Double Tap Like",
    description = "Matikan fungsi 2 kali ketuk untuk like"
) {
    runCatching {
        val processName = android.app.Application.getProcessName()
        if (processName != appContext.packageName) return@runCatching

        if (!com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.init(appContext)) return@runCatching

        // Piko hooks onDoubleTap inside classes matching certain strings (e.g. open_cmon_interstitial for post)
        val postMethods = com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.findMethodUsingStrings(
            "open_cmon_interstitial"
        )
        
        val postClasses = postMethods.map { it.declaringClass }.distinct()
        for (clazz in postClasses) {
            val onDoubleTap = clazz.methods.firstOrNull { it.name == "onDoubleTap" && it.parameterTypes.size == 1 && it.parameterTypes[0] == android.view.MotionEvent::class.java }
            if (onDoubleTap != null) {
                XposedBridge.hookMethod(onDoubleTap, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val prefs = de.robv.android.xposed.XSharedPreferences("com.rhdevs.rhpatch", "com.instagram.android")
                            prefs.makeWorldReadable()
                            if (prefs.getBoolean("pref_disable_double_tap_like", false)) {
                                param.result = true // Consumed, ignore tap
                            }
                        } catch (e: Exception) {}
                    }
                })
            }
        }
    }.onFailure { XposedBridge.log("Rhpatch: [DisableDoubleTapLike] Patch failed: it") }
}
