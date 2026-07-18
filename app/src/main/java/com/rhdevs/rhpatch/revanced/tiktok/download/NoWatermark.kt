package com.rhdevs.rhpatch.revanced.tiktok.download

import de.robv.android.xposed.XC_MethodReplacement
import com.rhdevs.rhpatch.patch

val NoWatermark = patch(
    name = "No Watermark",
) {
    ::aclCommonShareGetCode.hookMethod(XC_MethodReplacement.returnConstant(0))
    ::aclCommonShareGetShowType.hookMethod(XC_MethodReplacement.returnConstant(2))
    ::aclCommonShareGetTranscode.hookMethod(XC_MethodReplacement.returnConstant(1))
}
