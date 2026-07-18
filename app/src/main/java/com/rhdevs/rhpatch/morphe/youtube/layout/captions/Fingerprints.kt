package com.rhdevs.rhpatch.morphe.youtube.layout.captions

import com.rhdevs.rhpatch.morphe.AccessFlags
import com.rhdevs.rhpatch.morphe.Fingerprint
import com.rhdevs.rhpatch.morphe.Opcode
import com.rhdevs.rhpatch.morphe.OpcodesFilter
import com.wmods.wppenhacer.RequireAppVersion
import com.rhdevs.rhpatch.morphe.literal

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
