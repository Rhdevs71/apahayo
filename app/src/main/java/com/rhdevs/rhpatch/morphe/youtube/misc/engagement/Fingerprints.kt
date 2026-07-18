package com.rhdevs.rhpatch.morphe.youtube.misc.engagement

import com.rhdevs.rhpatch.morphe.AccessFlags
import com.rhdevs.rhpatch.morphe.Fingerprint
import com.rhdevs.rhpatch.morphe.Opcode
import com.rhdevs.rhpatch.morphe.fieldAccess
import com.rhdevs.rhpatch.morphe.findClassDirect
import com.rhdevs.rhpatch.morphe.findMethodDirect
import com.rhdevs.rhpatch.morphe.youtube.shared.EngagementPanelControllerFingerprint

internal object EngagementPanelUpdateFingerprint : Fingerprint(
    classFingerprint = EngagementPanelControllerFingerprint,
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "Z"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Landroid/app/Activity;"
        )
    )
)

val panelInitFingerprint = findMethodDirect {
    panelClass().findMethod {
        matcher {
            name = "<init>"
            paramTypes(String::class.java, null, null)
        }
    }.single()
}

val panelClass = findClassDirect {
    EngagementPanelControllerFingerprint.instructionMatches[3].instruction.classRef!!
}
