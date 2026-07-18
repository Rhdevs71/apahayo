package com.rhdevs.rhpatch.morphe.music.ad

import com.rhdevs.rhpatch.morphe.AccessFlags
import com.rhdevs.rhpatch.morphe.Fingerprint
import com.rhdevs.rhpatch.morphe.Opcode
import com.rhdevs.rhpatch.morphe.OpcodesFilter
import com.rhdevs.rhpatch.morphe.findMethodDirect
import com.rhdevs.rhpatch.morphe.fingerprint

internal object ShowVideoAdsFingerprint : Fingerprint(
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET_OBJECT,
    ),
    strings = listOf("maybeRegenerateCpnAndStatsClient called unexpectedly, but no error.")
)

val showVideoAds = findMethodDirect {
    ShowVideoAdsFingerprint.instructionMatches[1].instruction.methodRef!!
}

val hideGetPremiumFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    strings("FEmusic_history", "FEmusic_offline")
}

internal val membershipSettingsFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Ljava/lang/CharSequence;")
    opcodes(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
    )
}
