package com.rhdevs.rhpatch.youtube.youtube.layout.buttons.navigation

import com.rhdevs.rhpatch.youtube.AccessFlags
import com.rhdevs.rhpatch.youtube.Fingerprint
import com.rhdevs.rhpatch.youtube.Opcode
import com.rhdevs.rhpatch.youtube.OpcodesFilter
import com.rhdevs.rhpatch.youtube.findMethodDirect
import com.rhdevs.rhpatch.youtube.fingerprint
import com.rhdevs.rhpatch.youtube.strings

internal const val ANDROID_AUTOMOTIVE_STRING = "Android Automotive"

val addCreateButtonViewFingerprint = fingerprint {
    strings("Android Wear", ANDROID_AUTOMOTIVE_STRING)
}

// rvxp
val AutoMotiveFeatureMethod = findMethodDirect {
    addCreateButtonViewFingerprint().invokes.findMethod {
        matcher { strings("android.hardware.type.automotive") }
    }.single()
}

internal object CreatePivotBarFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    parameters = listOf(
        "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;",
        "Landroid/widget/TextView;",
        "Ljava/lang/CharSequence;",
    ),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID,
    ),
)
