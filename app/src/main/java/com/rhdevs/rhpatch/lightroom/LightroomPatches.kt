package com.rhdevs.rhpatch.lightroom

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val LightroomPremiumPatch = patch(
    name = "Adobe Lightroom Premium Unlock",
    description = "Membuka seluruh fitur Adobe Lightroom Premium (Selective Editing, Raw, Healing, Presets)"
) {
    runCatching {
        // Hook isPremium / isLTPUActive on all classes in adobe mobile
        val potentialClasses = listOf(
            "com.adobe.lrmobile.subscription.SubscriptionManager",
            "com.adobe.lrmobile.model.AccountInfo",
            "com.adobe.lrmobile.features.PremiumFeatures"
        )
        for (className in potentialClasses) {
            val cls = XposedHelpers.findClassIfExists(className, classLoader)
            if (cls != null) {
                for (method in cls.declaredMethods) {
                    val mName = method.name.lowercase()
                    if (method.returnType == Boolean::class.javaPrimitiveType &&
                        (mName.contains("premium") || mName.contains("subscribed") || mName.contains("ltpu") || mName.contains("vip"))) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true))
                    }
                }
            }
        }
    }
}

val LightroomPatches = arrayOf(LightroomPremiumPatch)
