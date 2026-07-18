package com.rhdevs.rhpatch.revanced.photomath.misc.unlock.plus

import de.robv.android.xposed.XC_MethodReplacement
import com.rhdevs.rhpatch.patch
import com.wmods.wppenhacer.Revanced.photomath.misc.unlock.bookpoint.EnableBookpoint

val UnlockPlus = patch(
    name = "Unlock plus",
) {
    dependsOn(EnableBookpoint)
    ::isPlusUnlockedFingerprint.hookMethod(XC_MethodReplacement.returnConstant(true))
}
