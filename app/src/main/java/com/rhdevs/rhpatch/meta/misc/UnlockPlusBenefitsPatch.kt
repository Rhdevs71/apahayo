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

        // Coba cari metode dengan berbagai kemungkinan string terkait IG Plus / Verified
        val targetStrings = listOf("is_benefit_active", "is_ig_creator_plus_unlocked")
        
        var methods = emptyList<java.lang.reflect.Method>()
        for (str in targetStrings) {
            // Hilangkan filter returnType di DexKit untuk memperlebar pencarian
            methods = com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.findMethodUsingStrings(str)
            
            // Filter manual untuk mencocokkan persis metode return Boolean dengan 1 parameter String
            methods = methods.filter { method ->
                (method.returnType == Boolean::class.javaPrimitiveType || method.returnType == java.lang.Boolean::class.java) &&
                method.parameterTypes.size == 1 &&
                method.parameterTypes[0] == String::class.java
            }
            if (methods.isNotEmpty()) break
        }

        var hooked = false
        for (method in methods) {
            // Pastikan method memiliki parameter minimal 0 atau 1 (biasanya boolean atau String)
            XposedBridge.hookMethod(method, object : de.robv.android.xposed.XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val prefs = de.robv.android.xposed.XSharedPreferences("com.rhdevs.rhpatch", "com.instagram.android")
                        prefs.makeWorldReadable()
                        if (prefs.getBoolean("Unlock IG Plus", true)) {
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
