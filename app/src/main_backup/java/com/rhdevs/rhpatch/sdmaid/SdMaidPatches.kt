package com.rhdevs.rhpatch.sdmaid

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val SdMaidUnlockPatch = patch(
    name = "SD Maid SE Pro License",
    description = "Membuka lisensi Pro SD Maid SE untuk pembersihan memori dan sistem otomatis"
) {
    runCatching {
        val billingClasses = listOf(
            "eu.darken.sdmse.common.upgrade.UpgradeRepo",
            "eu.darken.sdmse.common.billing.BillingManager",
            "eu.darken.sdmse.main.core.GeneralSettings"
        )
        for (className in billingClasses) {
            val cls = XposedHelpers.findClassIfExists(className, classLoader) ?: continue
            for (method in cls.declaredMethods) {
                val mName = method.name.lowercase()
                if (method.returnType == Boolean::class.javaPrimitiveType) {
                    if (mName.contains("ispro") || mName.contains("isunlocked") || mName.contains("isdonated")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true))
                    }
                }
            }
        }
    }
}

val SdMaidPatches = arrayOf(SdMaidUnlockPatch)
