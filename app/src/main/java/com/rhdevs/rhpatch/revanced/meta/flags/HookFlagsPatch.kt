package com.rhdevs.rhpatch.revanced.meta.flags

import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.revanced.meta.devkit.MetaUnobfuscator
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

val HookFlagsPatch = patch(
    name = "Override Experiment Flags (Instagram)",
    description = "Hooks Instagram's MobileConfig flag check to force enable/disable features (like Piko)"
) {

    runCatching {
        if (!MetaUnobfuscator.init(appContext)) {
            XposedBridge.log("Rhpatch: [Flags] Failed to initialize MetaUnobfuscator")
            return@runCatching
        }

        // Piko Fingerprint for String Flag Check: "__fbt_null__"
        val stringMethods = MetaUnobfuscator.findMethodUsingStrings("__fbt_null__", returnType = "java.lang.String")
        if (stringMethods.isEmpty()) {
            XposedBridge.log("Rhpatch: [Flags] Could not find flag check method")
        } else {
            val targetClass = stringMethods.first().declaringClass
            val booleanMethods = targetClass.declaredMethods.filter { 
                it.returnType == Boolean::class.javaPrimitiveType && 
                it.parameterTypes.contains(Long::class.javaPrimitiveType) &&
                !java.lang.reflect.Modifier.isAbstract(it.modifiers)
            }
                booleanMethods.forEach { method ->
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val specifierIndex = param.args.indexOfFirst { it is Long }
                            if (specifierIndex != -1) {
                                val specifier = param.args[specifierIndex] as Long
                                
                                // Extract configId using Piko's bitwise logic
                                val shifted = specifier ushr 16
                                val flag = ((specifier ushr 62) and 1L) == 1L
                                val paramId = if (flag) (shifted and 0xffff) else (shifted and 0xfff)
                                
                                // To get the exact configId we need the universalId, but for now we can match paramId
                                // Employee options is usually associated with paramId 0 or specific config keys.
                                // Instead of full ID, we can check known specifiers directly if we want,
                                // OR we can just force true for all Dev Options config specifiers if we know them.
                                // Actually, since we don't have X.0B3D (universalId), we can just check paramId.
                                // Employee Options paramId is 0 for ig_android_employee_options::is_enabled, 
                                // but 0 is too common.
                                
                                // Let's try forcing true for known developer options specifiers (IG v435)
                                val knownDevSpecifiers = listOf(
                                    36873966567194635L, // is_employee
                                    36873966567260172L, // is_developer
                                    36874838445588523L  // dev_options
                                )
                                if (knownDevSpecifiers.contains(specifier)) {
                                    param.result = true
                                    return
                                }
                                
                                // Piko Suggested Content Flags (Forcing false)
                                // We can use the bitwise paramId to identify them roughly if universalId is unavailable.
                                // Example: "111509::3" -> paramId = 3. 
                                // Better yet, we can intercept the settings UI or just leave the robust UI hider we wrote.
                            }
                        }
                    })
                }
            XposedBridge.log("Rhpatch: [Flags] Hooks installed successfully on ${booleanMethods.size} boolean flag check methods")
        }
    }.onFailure {
        XposedBridge.log("Rhpatch: [Flags] Hook failed: $it")
    }
}
