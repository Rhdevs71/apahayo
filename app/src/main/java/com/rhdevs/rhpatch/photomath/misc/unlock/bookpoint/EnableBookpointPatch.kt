package com.rhdevs.rhpatch.photomath.misc.unlock.bookpoint

import de.robv.android.xposed.XC_MethodReplacement
import com.rhdevs.rhpatch.patch

val EnableBookpoint = patch(
    description = "Enables textbook access",
) {
    ::isBookpointEnabledFingerprint.hookMethod(XC_MethodReplacement.returnConstant(true))
}
