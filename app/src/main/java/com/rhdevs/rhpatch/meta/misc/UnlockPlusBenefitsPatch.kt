package com.rhdevs.rhpatch.meta.misc

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

val UnlockPlusBenefitsPatch = patch(
    name = "Unlock IG Plus",
    description = "Unlocks Plus subscription benefits that are checked locally"
) {
    runCatching {
        if (!com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.init(appContext)) return@runCatching

        val targetStrings = listOf("is_benefit_active", "is_ig_creator_plus_unlocked")
        
        var methods = emptyList<java.lang.reflect.Method>()
        for (str in targetStrings) {
            val rawMethods = com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.findMethodUsingStrings(str)
            XposedBridge.log("Rhpatch: [UnlockPlus] '$str' -> ${rawMethods.size} valid methods before filter")
            
            methods = rawMethods.filter { it.returnType == Boolean::class.javaPrimitiveType || it.returnType == java.lang.Boolean::class.java }
            XposedBridge.log("Rhpatch: [UnlockPlus] '$str' -> ${methods.size} valid methods after boolean filter")
            
            if (methods.isNotEmpty()) break
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
            XposedBridge.log("Rhpatch: [UnlockPlus] Failed to find benefit checker method.")
        }
    }.onFailure { XposedBridge.log("Rhpatch: [UnlockPlus] Patch failed: $it") }
}

