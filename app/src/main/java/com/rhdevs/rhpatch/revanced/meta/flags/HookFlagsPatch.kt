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
                            
                            // Piko unconditionally enables dev options if the feature is activated.
                            // We will hardcode the flag specifiers for Dev Options (e.g. 36873966567194635L)
                            // and unlock them.
                            if (specifier == 36873966567194635L || specifier == 36873966567260172L || specifier == 36874838445588523L) {
                                param.result = true
                                return
                            }
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
