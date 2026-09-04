package com.rhdevs.rhpatch.meta.misc

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val UnlockPlusBenefitsPatch = patch(
    name = "Unlock Creator Plus (User Object Hook)",
    description = "Mengaktifkan fitur eksklusif berlangganan Creator Plus."
) {
    runCatching {
        // Find com.instagram.user.model.User
        val userClass = XposedHelpers.findClassIfExists("com.instagram.user.model.User", classLoader)
        if (userClass != null) {
            val booleanMethods = userClass.declaredMethods.filter { 
                it.returnType == Boolean::class.javaPrimitiveType || it.returnType == java.lang.Boolean::class.java 
            }
            
            // We want to hook methods that sound like "is_creator", "is_subscriber", "is_eligible"
            // But since it's obfuscated, we might not know the exact name.
            // However, User class boolean getters that return true for Creator Plus can be hooked blindly 
            // if we filter them carefully, or we can just hook ALL boolean methods that have no arguments 
            // and return true when the pref is enabled? NO, that would break everything (e.g. is_blocked, is_restricted).
            
            // Wait! In User class, is there a map or a config object we can intercept?
            // Actually, Instagram's "is_benefit_active" check takes the UserSession and a string/enum.
            // Since we can't find it via DexKit, let's use a known Xposed trick: hook UserSession's SharedPreferences or Experiment Configs (QuickExperiment).
            
            // But wait, the user said we can just hook User. Let's hook User.AXX boolean methods that might be related to creator plus.
            // Since this is too blind, let's hook the method that returns the subscription status if it's available.
            // Actually, we can hook all methods that return `Boolean` in `com.instagram.user.model.User` and just log them if they are called, 
            // OR we can hook the ones that might be the target.
            
            // Let's use a safer fallback: `User.A00()` or similar often returns User's boolean states.
            // Actually, let's just log and skip blind hooks. We will hook any method that takes "is_creator_plus" or similar as string.
            
            // A better way: Hook `X.0TJ.A05(UserSession)` or similar QuickExperiment hooks? Too hard without strings.
            
            // Let's hook boolean methods in `User` that have 0 parameters and return boolean.
            // To be safe, we only force true if the method name is known? It's obfuscated.
            XposedBridge.log("Rhpatch: [UnlockPlus] Found User class, but blind boolean hooking is disabled to prevent crashes. Awaiting manual signature.")
        } else {
            XposedBridge.log("Rhpatch: [UnlockPlus] User class not found.")
        }
        
        // Let's try to hook java.util.Map.get(Object) to see if they query "is_creator_plus"
        XposedBridge.hookAllMethods(java.util.Map::class.java, "get", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val key = param.args[0] as? String ?: return
                if (key == "is_creator_plus" || key == "is_creator_plus_subscriber" || key == "is_ig_creator_plus_unlocked") {
                    try {
                        val context = android.app.AndroidAppHelper.currentApplication()
                        val prefs = context?.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
                        if (prefs?.getBoolean("pref_ig_plus", true) == true) {
                            param.result = true
                        }
                    } catch (e: Exception) {}
                }
            }
        })
        
        // Try hooking SharedPreferences.getBoolean in IG
        val sharedPrefsImpl = XposedHelpers.findClassIfExists("android.app.SharedPreferencesImpl", null)
        if (sharedPrefsImpl != null) {
            XposedBridge.hookAllMethods(sharedPrefsImpl, "getBoolean", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as? String ?: return
                    if (key.contains("creator_plus") || key.contains("subscriber")) {
                        try {
                            val context = android.app.AndroidAppHelper.currentApplication()
                            val prefs = context?.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
                            if (prefs?.getBoolean("pref_ig_plus", true) == true) {
                                param.result = true
                            }
                        } catch (e: Exception) {}
                    }
                }
            })
        }
    }.onFailure { XposedBridge.log("Rhpatch: [UnlockPlus] Patch failed: $it") }
}

