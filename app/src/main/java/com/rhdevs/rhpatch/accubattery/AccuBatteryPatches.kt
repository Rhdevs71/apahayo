package com.rhdevs.rhpatch.accubattery

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val AccuBatteryUnlockPatch = patch(
    name = "AccuBattery Pro Features",
    description = "Membuka status keanggotaan Pro untuk monitoring kesehatan baterai dan tema AMOLED"
) {
    runCatching {
        val proClasses = listOf(
            "com.digibites.accubattery.billing.BillingManager",
            "com.digibites.accubattery.data.user.UserStatus"
        )
        for (className in proClasses) {
            val cls = XposedHelpers.findClassIfExists(className, classLoader) ?: continue
            for (method in cls.declaredMethods) {
                val mName = method.name.lowercase()
                if (method.returnType == Boolean::class.javaPrimitiveType) {
                    if (mName.contains("ispro") || mName.contains("ispremium") || mName.contains("isdonated")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true))
                    }
                }
            }
        }
    }
}

val AccuBatteryPatches = arrayOf(AccuBatteryUnlockPatch)
