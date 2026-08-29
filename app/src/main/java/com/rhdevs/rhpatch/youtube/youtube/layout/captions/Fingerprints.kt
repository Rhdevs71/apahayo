package com.rhdevs.rhpatch.youtube.youtube.layout.captions

import com.rhdevs.rhpatch.youtube.AccessFlags
import com.rhdevs.rhpatch.youtube.Fingerprint
import com.rhdevs.rhpatch.youtube.Opcode
import com.rhdevs.rhpatch.youtube.OpcodesFilter
import com.rhdevs.rhpatch.RequireAppVersion
import com.rhdevs.rhpatch.youtube.literal

internal object StartVideoInformerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_INTERFACE,
        Opcode.RETURN_VOID,
    ),
    strings = listOf("pc"),
)

/**
 * YouTube 20.26+
 */
@RequireAppVersion("20.26.00")
internal object NoVolumeCaptionsFeatureFlagFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    filters = listOf(
        literal(45692436L)
    ),
)
