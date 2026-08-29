package com.rhdevs.rhpatch.adobescan

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val AdobeScanUnlockPatch = patch(
    name = "Adobe Scan Premium & OCR",
    description = "Membuka batas OCR teks dan fitur ekspor dokumen premium di Adobe Scan"
) {
    runCatching {
        val scanClasses = listOf(
            "com.adobe.scan.android.user.UserSubscription",
            "com.adobe.scan.android.subscription.SubscriptionManager",
            "com.adobe.scan.android.models.AccountInfo"
        )
        for (className in scanClasses) {
            val cls = XposedHelpers.findClassIfExists(className, classLoader) ?: continue
            for (method in cls.declaredMethods) {
                val mName = method.name.lowercase()
                if (method.returnType == Boolean::class.javaPrimitiveType) {
                    if (mName.contains("ispro") || mName.contains("ispremium") || mName.contains("hasocr") || mName.contains("issubscribed")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true))
                    }
                }
            }
        }
    }
}

val AdobeScanPatches = arrayOf(AdobeScanUnlockPatch)
