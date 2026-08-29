package com.rhdevs.rhpatch.stickerly

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val StickerlyUnlockPatch = patch(
    name = "Sticker.ly AdFree & Unlimited WA Exports",
    description = "Menghilangkan iklan dan membuka batas ekspor paket stiker ke WhatsApp"
) {
    runCatching {
        val userClasses = listOf(
            "com.snowcorp.stickerly.model.User",
            "com.snowcorp.stickerly.model.SubscriptionInfo",
            "com.snowcorp.stickerly.ad.AdManager"
        )
        for (className in userClasses) {
            val cls = XposedHelpers.findClassIfExists(className, classLoader) ?: continue
            for (method in cls.declaredMethods) {
                val mName = method.name.lowercase()
                if (method.returnType == Boolean::class.javaPrimitiveType) {
                    if (mName.contains("ispro") || mName.contains("ispremium") || mName.contains("isadfree") || mName.contains("canexport")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true))
                    } else if (mName.contains("shouldshowad") || mName.contains("hasad")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false))
                    }
                }
            }
        }
    }
}

val StickerlyPatches = arrayOf(StickerlyUnlockPatch)
