package com.rhdevs.rhpatch.morphe.music.layout.upgradebutton

import com.rhdevs.rhpatch.morphe.AccessFlags
import com.rhdevs.rhpatch.morphe.Opcode
import com.rhdevs.rhpatch.morphe.findFieldDirect
import com.rhdevs.rhpatch.morphe.fingerprint

internal val pivotBarConstructorFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    returns("V")
    parameters("L", "Z")
    opcodes(
        Opcode.INVOKE_INTERFACE,
        Opcode.GOTO,
        Opcode.IPUT_OBJECT,
        Opcode.RETURN_VOID
    )
}

val pivotBarElementField = findFieldDirect {
    pivotBarConstructorFingerprint().declaredClass!!.fields.single { f -> f.typeName == "java.util.List" }
}
