package com.rhdevs.rhpatch.macrodroid

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val MacroDroidUnlockPatch = patch(
    name = "MacroDroid Pro Unlimited Macros",
    description = "Membuka status MacroDroid Pro dan menghapus batas jumlah makro otomatisasi"
) {
    runCatching {
        val proClasses = listOf(
            "com.arlosoft.macrodroid.upgrade.UpgradeManager",
            "com.arlosoft.macrodroid.upgrade.billing.BillingDataSource",
            "com.arlosoft.macrodroid.settings.Settings"
        )
        for (className in proClasses) {
            val cls = XposedHelpers.findClassIfExists(className, classLoader) ?: continue
            for (method in cls.declaredMethods) {
                val mName = method.name.lowercase()
                if (method.returnType == Boolean::class.javaPrimitiveType) {
                    if (mName.contains("ispro") || mName.contains("ispremium") || mName.contains("haspro") || mName.contains("isunlocked")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true))
                    }
                }
            }
        }
    }
}

val MacroDroidPatches = arrayOf(MacroDroidUnlockPatch)
