package com.rhdevs.rhpatch.camscanner

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val CamScannerPremiumPatch = patch(
    name = "CamScanner VIP / Premium Unlock",
    description = "Membuka seluruh fitur VIP/Premium (Tanpa Watermark, Ekspor PDF Kualitas Tinggi, OCR)"
) {
    runCatching {
        val accountPrefCls = XposedHelpers.findClassIfExists("com.intsig.comm.account_data.AccountPreference", classLoader)
        if (accountPrefCls != null) {
            // Hook all methods returning Long or Boolean that determine status code
            for (method in accountPrefCls.declaredMethods) {
                if (method.returnType == Long::class.javaPrimitiveType || method.returnType == java.lang.Long::class.java) {
                    XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(1L))
                }
            }
        }
        
        // Also hook property getter in user properties if present
        val userPropertyCls = XposedHelpers.findClassIfExists("com.intsig.camscanner.datastruct.UserProperty", classLoader)
        if (userPropertyCls != null) {
            XposedBridge.hookAllMethods(userPropertyCls, "isVIP", XC_MethodReplacement.returnConstant(true))
            XposedBridge.hookAllMethods(userPropertyCls, "isPaidUser", XC_MethodReplacement.returnConstant(true))
        }
    }
}

val CamScannerPatches = arrayOf(CamScannerPremiumPatch)
