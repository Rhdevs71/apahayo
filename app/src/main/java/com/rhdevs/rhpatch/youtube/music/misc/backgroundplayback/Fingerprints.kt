package com.rhdevs.rhpatch.youtube.music.misc.backgroundplayback

import com.rhdevs.rhpatch.youtube.AccessFlags
import com.rhdevs.rhpatch.youtube.Opcode
import com.rhdevs.rhpatch.youtube.fingerprint

internal val backgroundPlaybackDisableFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returns("Z")
    parameters("L")
    opcodes(
        Opcode.CONST_4,
        Opcode.IF_EQZ,
        Opcode.IGET,
        Opcode.AND_INT_LIT16,
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
        Opcode.SGET_OBJECT,
        Opcode.IGET,
    )
}

internal val kidsBackgroundPlaybackPolicyControllerFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("V")
    parameters("I", "L", "Z")
    opcodes(
        Opcode.IGET,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.IF_NEZ,
        Opcode.GOTO_16,
        Opcode.CONST_4,
        Opcode.IF_NE,
        Opcode.CONST_4,
        Opcode.IF_NE,
        Opcode.CONST_4,
        Opcode.NEW_ARRAY,
    )
}
