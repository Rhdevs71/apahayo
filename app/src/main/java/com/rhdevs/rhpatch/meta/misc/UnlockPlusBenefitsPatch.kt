package com.rhdevs.rhpatch.meta.misc

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

val UnlockPlusBenefitsPatch = patch(
    name = "Unlock Creator Plus",
    description = "Mengaktifkan fitur eksklusif berlangganan Creator Plus."
) {
    runCatching {
        if (!com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.init(appContext)) return@runCatching
        
        // Find method using the exact fingerprint from piko:
        // class contains string "is_benefit_active"
        // method parameters: Ljava/lang/String;
        // return type: Z (Boolean)
        // Access flag: PUBLIC
        
        val methods = com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.findMethodUsingStrings("is_benefit_active")
        val targetMethods = methods.filter { method ->
            val isPublic = java.lang.reflect.Modifier.isPublic(method.modifiers)
            val returnsBoolean = method.returnType == Boolean::class.javaPrimitiveType || method.returnType == java.lang.Boolean::class.java
            val paramTypes = method.parameterTypes
            
            isPublic && returnsBoolean && paramTypes.size == 1 && paramTypes[0] == String::class.java
        }

        if (targetMethods.isNotEmpty()) {
            for (method in targetMethods) {
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
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
            }
        } else {
            XposedBridge.log("Rhpatch: [UnlockPlus] Method with signature (String)Z in class with 'is_benefit_active' not found.")
        }
    }.onFailure { XposedBridge.log("Rhpatch: [UnlockPlus] Patch failed: $it") }
}
