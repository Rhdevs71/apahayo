package com.rhdevs.rhpatch.getcontact

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val GetcontactUnlockPatch = patch(
    name = "Getcontact Premium Yearly & Unlimited Lookups",
    description = "Membuka status Premium Yearly (Tariff 10), pencarian tag tak terbatas, siapa yang melihat profil, dan bebas iklan"
) {
    // 1. Hook SubscriptionModel & Usage getters for Play Store & Direct packages
    runCatching {
        val modelClasses = listOf(
            // Play Store package models
            "app.source.getcontact.data.model.SubscriptionModel",
            "app.source.getcontact.data.model.UsageInfo",
            "app.source.getcontact.data.model.UserData",
            "app.source.getcontact.data.model.PremiumInfo",
            "app.source.getcontact.models.Subscription",
            "app.source.getcontact.models.PremiumStatus",
            "app.source.getcontact.models.UserSubscription",
            // Direct/Alt package models
            "com.getcontact.app.data.model.SubscriptionModel",
            "com.getcontact.app.data.model.UsageInfo",
            "com.getcontact.app.data.model.UserData",
            "com.getcontact.app.data.model.PremiumInfo"
        )
        for (className in modelClasses) {
            val cls = XposedHelpers.findClassIfExists(className, classLoader) ?: continue
            for (method in cls.declaredMethods) {
                val mName = method.name.lowercase()
                val retType = method.returnType
                
                if (retType == Boolean::class.javaPrimitiveType || retType == java.lang.Boolean::class.java) {
                    if (mName.contains("pro") || mName.contains("tariff") || mName.contains("premium") || 
                        mName.contains("showwho") || mName.contains("showstatics") || mName.contains("trialused") ||
                        mName.contains("mainsubscription") || mName.contains("issubscribed") || mName.contains("isvip") ||
                        mName.contains("haspremium") || mName.contains("ispremium") || mName.contains("isadsfree")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true))
                    } else if (mName.contains("showtagusage") || mName.contains("showtrustscoreusage") || mName.contains("iscolorred") || mName.contains("showoffer")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false))
                    }
                } else if (retType == Int::class.javaPrimitiveType || retType == java.lang.Integer::class.java) {
                    if (mName.contains("remainingcount") || mName.contains("limit") || mName.contains("count") || mName.contains("remaintagsearch")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(999999))
                    }
                } else if (retType == String::class.java) {
                    if (mName.contains("packagetext") || mName.contains("planname") || mName.contains("tariffname")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant("Premium Yearly"))
                    } else if (mName.contains("productid")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant("tariff10"))
                    }
                }
            }
        }
    }
}

val GetcontactPatches = arrayOf(GetcontactUnlockPatch)
