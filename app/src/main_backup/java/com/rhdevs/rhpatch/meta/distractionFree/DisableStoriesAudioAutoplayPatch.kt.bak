package com.rhdevs.rhpatch.revanced.meta.distractionFree

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

val DisableStoriesAudioAutoplayPatch = patch(
    name = "Disable Stories Audio Autoplay",
    description = "Mematikan audio otomatis di Story"
) {
    runCatching {
        if (!com.rhdevs.rhpatch.revanced.meta.devkit.MetaUnobfuscator.init(appContext)) return@runCatching

        // Cari class yang memiliki string "audio state did not match"
        val methodsWithStr = com.rhdevs.rhpatch.revanced.meta.devkit.MetaUnobfuscator.findMethodUsingStrings("audio state did not match")
        
        if (methodsWithStr.isNotEmpty()) {
            val targetClass = methodsWithStr.first().declaringClass
            
            val desiredMethods = targetClass.declaredMethods.filter { 
                (it.returnType == Boolean::class.javaPrimitiveType || it.returnType == java.lang.Boolean::class.java) &&
                it.parameterTypes.isEmpty() &&
                !java.lang.reflect.Modifier.isAbstract(it.modifiers)
            }
            
            val targetMethod = if (desiredMethods.size >= 3) desiredMethods[2] else desiredMethods.firstOrNull()
            
            if (targetMethod != null) {
                XposedBridge.hookMethod(targetMethod, object : XC_MethodHook() {
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
                XposedBridge.log("Rhpatch: [DisableStoriesAudioAutoplay] Hook installed successfully!")
            } else {
                XposedBridge.log("Rhpatch: [DisableStoriesAudioAutoplay] Failed to find target boolean method.")
            }
        } else {
            XposedBridge.log("Rhpatch: [DisableStoriesAudioAutoplay] Failed to find string fingerprint.")
        }
    }.onFailure { XposedBridge.log("Rhpatch: [DisableStoriesAudioAutoplay] Patch failed: $it") }
}
