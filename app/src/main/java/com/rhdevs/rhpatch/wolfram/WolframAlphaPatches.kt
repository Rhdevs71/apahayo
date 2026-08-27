package com.rhdevs.rhpatch.wolfram

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val WolframAlphaUnlockPatch = patch(
    name = "WolframAlpha Pro & Step-by-Step Solutions",
    description = "Membuka solusi langkah-demi-langkah dan kalkulasi Pro di WolframAlpha"
) {
    runCatching {
        val proClasses = listOf(
            "com.wolfram.android.alpha.user.UserStatus",
            "com.wolfram.android.alpha.billing.BillingManager",
            "com.wolfram.android.alpha.common.AccountManager"
        )
        for (className in proClasses) {
            val cls = XposedHelpers.findClassIfExists(className, classLoader) ?: continue
            for (method in cls.declaredMethods) {
                val mName = method.name.lowercase()
                if (method.returnType == Boolean::class.javaPrimitiveType) {
                    if (mName.contains("ispro") || mName.contains("haspro") || mName.contains("hasstepbystep") || mName.contains("ispremium")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true))
                    }
                }
            }
        }
    }
}

val WolframAlphaPatches = arrayOf(WolframAlphaUnlockPatch)
