package com.rhdevs.rhpatch.rar

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val RarUnlockPatch = patch(
    name = "RAR Premium & Ad-Free",
    description = "Membuka lisensi RAR Premium dan menghilangkan seluruh iklan"
) {
    runCatching {
        val rarClasses = listOf(
            "com.rarlab.rar.MainActivity",
            "com.rarlab.rar.billing.BillingManager",
            "com.rarlab.rar.License"
        )
        for (className in rarClasses) {
            val cls = XposedHelpers.findClassIfExists(className, classLoader) ?: continue
            for (method in cls.declaredMethods) {
                val mName = method.name.lowercase()
                if (method.returnType == Boolean::class.javaPrimitiveType) {
                    if (mName.contains("ispremium") || mName.contains("isadfree") || mName.contains("islicensed") || mName.contains("haslicense")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true))
                    }
                }
            }
        }
    }
}

val RarPatches = arrayOf(RarUnlockPatch)
