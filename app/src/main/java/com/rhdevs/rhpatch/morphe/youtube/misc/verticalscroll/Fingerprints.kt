package com.rhdevs.rhpatch.morphe.youtube.misc.verticalscroll

import com.rhdevs.rhpatch.morphe.AccessFlags
import com.rhdevs.rhpatch.morphe.Opcode
import com.rhdevs.rhpatch.morphe.fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

val canScrollVerticallyFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Z")
    parameters()
    opcodes(
        Opcode.MOVE_RESULT,
        Opcode.RETURN,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT
    )
    classMatcher { className(".SwipeRefreshLayout", StringMatchType.EndsWith) }
}
