package com.rhdevs.rhpatch.youtube.youtube.misc.litho.node

import com.rhdevs.rhpatch.youtube.AccessFlags
import com.rhdevs.rhpatch.youtube.Fingerprint
import com.rhdevs.rhpatch.youtube.Opcode
import com.rhdevs.rhpatch.youtube.methodCall
import com.rhdevs.rhpatch.youtube.youtube.layout.hide.general.ParseElementFromBufferFingerprint

internal object TreeNodeResultListFingerprint : Fingerprint(
    classFingerprint = ParseElementFromBufferFingerprint,
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "Ljava/util/List;",
    filters = listOf(
        methodCall(name = "nCopies", opcode = Opcode.INVOKE_STATIC),
    )
)
