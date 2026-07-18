package com.rhdevs.rhpatch.morphe.shared.misc.initialization

import com.rhdevs.rhpatch.morphe.AccessFlags
import com.rhdevs.rhpatch.morphe.Fingerprint
import com.rhdevs.rhpatch.morphe.Opcode
import com.rhdevs.rhpatch.morphe.findMethodDirect
import com.rhdevs.rhpatch.morphe.indexOfFirstInstructionReversed
import com.rhdevs.rhpatch.morphe.methodCall
import com.rhdevs.rhpatch.morphe.string

internal object GlobalConfigGroupFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            smali = "Ljava/util/concurrent/locks/ReentrantLock;->lock()V"
        ),
        string(string = "com.google.android.libraries.youtube.innertube.cold_stored_timestamp"),
        methodCall(
            opcode = Opcode.INVOKE_INTERFACE,
            name = "putLong"
        )
    )
)

val handleColdFingerprint = findMethodDirect {
    val method = GlobalConfigGroupFingerprint()
    val matches = GlobalConfigGroupFingerprint.matchOrNull(method)?.instructionMatches!!
    val str_index = matches[2].instruction.index

    val index = method.indexOfFirstInstructionReversed (str_index) {
        this.opcode == Opcode.INVOKE_STATIC.opCode
    }

    method.instructions[index].methodRef!!
}
