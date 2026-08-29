package com.rhdevs.rhpatch.kahoot

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val KahootUnlockPatch = patch(
    name = "Kahoot! Pro & Premium Features",
    description = "Membuka fitur langganan pembelajaran dan kuis Kahoot! Pro"
) {
    runCatching {
        val userClasses = listOf(
            "com.kahoot.android.domain.user.User",
            "com.kahoot.android.domain.user.SubscriptionStatus",
            "com.kahoot.android.feature.billing.BillingManager"
        )
        for (className in userClasses) {
            val cls = XposedHelpers.findClassIfExists(className, classLoader) ?: continue
            for (method in cls.declaredMethods) {
                val mName = method.name.lowercase()
                if (method.returnType == Boolean::class.javaPrimitiveType) {
                    if (mName.contains("ispro") || mName.contains("ispremium") || mName.contains("hasaccess") || mName.contains("hasplussubscription")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true))
                    }
                }
            }
        }
    }
}

val KahootPatches = arrayOf(KahootUnlockPatch)
