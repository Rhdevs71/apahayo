package com.rhdevs.rhpatch.morphe.youtube.misc.litho.node

import com.rhdevs.rhpatch.morphe.AccessFlags
import com.rhdevs.rhpatch.morphe.Fingerprint
import com.rhdevs.rhpatch.morphe.Opcode
import com.rhdevs.rhpatch.morphe.methodCall
import com.rhdevs.rhpatch.morphe.youtube.layout.hide.general.ParseElementFromBufferFingerprint

internal object TreeNodeResultListFingerprint : Fingerprint(
    classFingerprint = ParseElementFromBufferFingerprint,
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "Ljava/util/List;",
    filters = listOf(
        methodCall(name = "nCopies", opcode = Opcode.INVOKE_STATIC),
    )
)
