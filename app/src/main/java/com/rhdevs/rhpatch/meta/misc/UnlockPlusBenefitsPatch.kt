package com.rhdevs.rhpatch.meta.misc

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val UnlockPlusBenefitsPatch = patch(
    name = "Unlock IG Plus",
    description = "Unlocks Plus subscription benefits that are checked locally"
) {
    runCatching {
        val processName = android.app.Application.getProcessName()
        if (processName != appContext.packageName) return@runCatching

        if (!com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.init(appContext)) return@runCatching

        // Fokus HANYA pada string yang dipakai Piko
        val targetStrings = listOf("is_benefit_active")
        
        var methods = emptyList<java.lang.reflect.Method>()
        for (str in targetStrings) {
            val found = com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.findMethodUsingStrings(str)
            
            // Cocokkan pola Piko: 1 parameter String, return Boolean
            methods = found.filter { method ->
                (method.returnType == Boolean::class.javaPrimitiveType || method.returnType == java.lang.Boolean::class.java) &&
                method.parameterTypes.size == 1 &&
                method.parameterTypes[0] == String::class.java
            }
            if (methods.isNotEmpty()) {
                break
            }
        }
        
        var hooked = false
        for (method in methods) {
            XposedBridge.hookMethod(method, object : de.robv.android.xposed.XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val prefs = de.robv.android.xposed.XSharedPreferences("com.rhdevs.rhpatch", "com.instagram.android")
                        prefs.makeWorldReadable()
                        if (prefs.getBoolean("pref_ig_plus", true)) {
                            param.result = true
                        }
                    } catch (e: Exception) {}
                }
            })
            hooked = true
            XposedBridge.log("Rhpatch: [UnlockPlus] Hooked benefit checker method: {method.declaringClass.name}.{method.name}")
        }
        
        if (!hooked) {
            XposedBridge.log("Rhpatch: [UnlockPlus] Failed to find benefit checker method.")
        }
    }.onFailure { XposedBridge.log("Rhpatch: [UnlockPlus] Patch failed: it") }
}
