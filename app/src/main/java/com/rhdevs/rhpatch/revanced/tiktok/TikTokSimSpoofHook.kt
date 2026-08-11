package com.rhdevs.rhpatch.revanced.tiktok

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

object TikTokSimSpoofHook {
    fun apply(classLoader: ClassLoader, prefs: android.content.SharedPreferences) {
        
        if (!prefs.getBoolean("tiktok_sim_spoof", false)) return
        
        val targetCountry = prefs.getString("tiktok_sim_country", "US") ?: "US"
        val targetMccMnc = prefs.getString("tiktok_sim_mcc_mnc", "310260") ?: "310260"
        val targetOperator = prefs.getString("tiktok_sim_operator_name", "T-Mobile") ?: "T-Mobile"
        
        try {
            val tmClass = XposedHelpers.findClass("android.telephony.TelephonyManager", classLoader)
            
            // Hook for Country ISO
            val hookCountry = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    param.result = targetCountry
                }
            }
            XposedBridge.hookAllMethods(tmClass, "getSimCountryIso", hookCountry)
            XposedBridge.hookAllMethods(tmClass, "getNetworkCountryIso", hookCountry)

            // Hook for MCC/MNC
            val hookMccMnc = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    param.result = targetMccMnc
                }
            }
            XposedBridge.hookAllMethods(tmClass, "getSimOperator", hookMccMnc)
            XposedBridge.hookAllMethods(tmClass, "getNetworkOperator", hookMccMnc)

            // Hook for Operator Name
            val hookOperatorName = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    param.result = targetOperator
                }
            }
            XposedBridge.hookAllMethods(tmClass, "getSimOperatorName", hookOperatorName)
            XposedBridge.hookAllMethods(tmClass, "getNetworkOperatorName", hookOperatorName)
            
            XposedBridge.log("Rhpatch TikTok: SIM Spoof hooked successfully (Country: $targetCountry, MCC/MNC: $targetMccMnc, Operator: $targetOperator).")
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch TikTok SIM Spoof Error: ${e.message}")
        }
    }
}

