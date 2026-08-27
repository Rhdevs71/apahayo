package com.rhdevs.rhpatch.ibispaint

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val IbisPaintPrimePatch = patch(
    name = "IbisPaint X Prime Membership Unlock",
    description = "Membuka status keanggotaan Prime (Semua Kuas/Brushes, Material, Canvas, dan Font)"
) {
    runCatching {
        // Hook all Prime and License methods in IbisPaint
        val potentialClasses = listOf(
            "jp.ne.ibis.ibispaintx.app.PrimeMemberManager",
            "jp.ne.ibis.ibispaint.common.PrimeFeature",
            "jp.ne.ibis.ibispaint.billing.BillingManager",
            "jp.ne.ibis.ibispaint.membership.MembershipInfo"
        )
        for (className in potentialClasses) {
            val cls = XposedHelpers.findClassIfExists(className, classLoader)
            if (cls != null) {
                for (method in cls.declaredMethods) {
                    val mName = method.name.lowercase()
                    if (method.returnType == Boolean::class.javaPrimitiveType) {
                        if (mName.contains("prime") || mName.contains("paid") || mName.contains("purchased") || mName.contains("valid")) {
                            XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true))
                        }
                    }
                }
            }
        }
    }
}

val IbisPaintPatches = arrayOf(IbisPaintPrimePatch)
