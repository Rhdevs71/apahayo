package com.rhdevs.rhpatch.speedtest

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val SpeedtestUnlockPatch = patch(
    name = "Speedtest by Ookla Ad-Free & Premium Features",
    description = "Menghilangkan iklan pengujian dan membuka fitur pengujian video dan VPN Speedtest"
) {
    runCatching {
        val stClasses = listOf(
            "org.zwanoo.android.speedtest.model.UserStatus",
            "org.zwanoo.android.speedtest.ad.AdManager",
            "org.zwanoo.android.speedtest.billing.BillingManager"
        )
        for (className in stClasses) {
            val cls = XposedHelpers.findClassIfExists(className, classLoader) ?: continue
            for (method in cls.declaredMethods) {
                val mName = method.name.lowercase()
                if (method.returnType == Boolean::class.javaPrimitiveType) {
                    if (mName.contains("isadfree") || mName.contains("ispremium") || mName.contains("ispro")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true))
                    } else if (mName.contains("shouldshowad")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false))
                    }
                }
            }
        }
    }
}

val SpeedtestPatches = arrayOf(SpeedtestUnlockPatch)
