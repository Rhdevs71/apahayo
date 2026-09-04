package com.rhdevs.rhpatch.meta.misc

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XposedBridge

val UnlockPlusBenefitsPatch = patch(
    name = "Unlock Creator Plus",
    description = "Mengaktifkan fitur eksklusif berlangganan Creator Plus."
) {
    runCatching {
        if (!com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.init(appContext)) return@runCatching

        val stringsToFind = listOf("is_creator_plus", "has_creator_plus", "is_subscriber", "is_ig_creator_plus_unlocked", "is_benefit_active")
        var methods = emptyList<java.lang.reflect.Method>()
        
        for (str in stringsToFind) {
            val rawMethods = com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.findMethodUsingStrings(str)
            
            methods = rawMethods.filter { 
                it.returnType == Boolean::class.javaPrimitiveType || it.returnType == java.lang.Boolean::class.java 
            }
            
            if (methods.isNotEmpty()) {
                XposedBridge.log("Rhpatch: [UnlockPlus] '$str' -> ${methods.size} valid boolean methods found")
                break
            }
        }
        
        var hooked = false
        for (method in methods) {
            XposedBridge.hookMethod(method, object : de.robv.android.xposed.XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val context = android.app.AndroidAppHelper.currentApplication()
                        val prefs = context?.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
                        if (prefs?.getBoolean("pref_ig_plus", true) == true) {
                            param.result = true
                        }
                    } catch (e: Exception) {}
                }
            })
            hooked = true
            XposedBridge.log("Rhpatch: [UnlockPlus] Hooked benefit checker method: ${method.declaringClass.name}.${method.name}")
        }
        
        if (!hooked) {
            XposedBridge.log("Rhpatch: [UnlockPlus] Failed to find boolean benefit checker method.")
        }
    }.onFailure { XposedBridge.log("Rhpatch: [UnlockPlus] Patch failed: $it") }
}
