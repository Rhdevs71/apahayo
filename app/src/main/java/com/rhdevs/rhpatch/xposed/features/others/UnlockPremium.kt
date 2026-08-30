package com.rhdevs.rhpatch.xposed.features.others

import android.content.SharedPreferences
import com.rhdevs.rhpatch.xposed.core.Feature
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

class UnlockPremium(classLoader: ClassLoader, preferences: SharedPreferences) :
    Feature(classLoader, preferences) {

    override fun doHook() {
        if (!preferences.getBoolean("pref_wa_premium", false)) return

        try {
            val targetStrings = arrayOf(
                "is_premium_user", "is_smb_premium", "smb_premium", "is_verified_user", "is_whatsapp_premium",
                "is_benefit_active", "is_wa_creator_plus_unlocked", "is_creator_plus_unlocked",
                "has_active_subscription", "is_subscriber", "is_premium", "is_verified", "is_plus"
            )
            
            var hookedCount = 0
            for (str in targetStrings) {
                val methods = com.rhdevs.rhpatch.xposed.core.devkit.Unobfuscator.findAllMethodUsingStrings(
                    classLoader,
                    org.luckypray.dexkit.query.enums.StringMatchType.Contains,
                    str
                )
                
                for (method in methods) {
                    if (method.returnType == Boolean::class.javaPrimitiveType || method.returnType == java.lang.Boolean::class.java) {
                        try {
                            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                                override fun beforeHookedMethod(param: MethodHookParam) {
                                    param.result = true
                                }
                            })
                            hookedCount++
                            XposedBridge.log("Rhpatch: [UnlockPremium] Hooked ${method.declaringClass.name}.${method.name} via '$str'")
                        } catch (e: Exception) {
                            // ignore hook errors for abstract/interface methods that bypassed DexKit somehow
                        }
                    }
                }
            }
            
            XposedBridge.log("Rhpatch: [UnlockPremium] Successfully hooked $hookedCount premium/verified checker methods.")
        } catch (e: Exception) {
            XposedBridge.log("Rhpatch: [UnlockPremium] Error: ${e.message}")
        }
    }

    override fun getPluginName(): String {
        return "Unlock Plus & Premium"
    }
}
