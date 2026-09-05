package com.rhdevs.rhpatch.meta.misc

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Method

val UnlockPlusBenefitsPatch = patch(
    name = "Unlock Creator Plus",
    description = "Mengaktifkan fitur eksklusif berlangganan Creator Plus."
) {
    runCatching {
        if (!com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.init(appContext)) return@runCatching
        
        // Find methods containing the string "is_benefit_active"
        val methodsUsingString = com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.findMethodUsingStrings("is_benefit_active")
        
        val targetMethods = mutableListOf<Method>()
        
        // Piko's logic searches for the CLASS containing the string, then finds a public method returning Boolean with 1 String arg.
        for (method in methodsUsingString) {
            val declaringClass = method.declaringClass
            val classMethods = declaringClass.declaredMethods
            
            for (classMethod in classMethods) {
                val isPublic = java.lang.reflect.Modifier.isPublic(classMethod.modifiers)
                val returnsBoolean = classMethod.returnType == Boolean::class.javaPrimitiveType || classMethod.returnType == java.lang.Boolean::class.java
                val paramTypes = classMethod.parameterTypes
                
                if (isPublic && returnsBoolean && paramTypes.size == 1 && paramTypes[0] == String::class.java) {
                    if (!targetMethods.contains(classMethod)) {
                        targetMethods.add(classMethod)
                    }
                }
            }
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
            XposedBridge.log("Rhpatch: [UnlockPlus] Berhasil nge-hook ${targetMethods.size} method via class reference.")
        } else {
            XposedBridge.log("Rhpatch: [UnlockPlus] Method with signature (String)Z in class with 'is_benefit_active' not found.")
        }
    }.onFailure { XposedBridge.log("Rhpatch: [UnlockPlus] Patch failed: $it") }
}
