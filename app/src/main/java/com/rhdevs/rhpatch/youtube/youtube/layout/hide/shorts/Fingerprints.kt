package com.rhdevs.rhpatch.youtube.youtube.layout.hide.shorts

import com.rhdevs.rhpatch.youtube.AccessFlags
import com.rhdevs.rhpatch.youtube.Fingerprint
import com.rhdevs.rhpatch.youtube.InstructionLocation.MatchAfterWithin
import com.rhdevs.rhpatch.youtube.Opcode
import com.rhdevs.rhpatch.youtube.fieldAccess
import com.rhdevs.rhpatch.youtube.findFieldDirect
import com.rhdevs.rhpatch.youtube.literal
import com.rhdevs.rhpatch.youtube.methodCall
import com.rhdevs.rhpatch.youtube.opcode

internal object ShortsExperimentalPlayerFeatureFlagFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        literal(45677719L)
    )
)


internal object RenderNextUIFeatureFlagFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        literal(45649743L)
    )
)

internal object DoubleTapToLikeLogicFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf("Landroid/view/MotionEvent;"),
    filters = listOf(
        literal(255),
        methodCall("Landroid/view/MotionEvent;->getEventTime()J"),
        methodCall("Ljava/lang/Math;->hypot(DD)D"),
        fieldAccess(
            opcode = Opcode.IGET_BOOLEAN,
            definingClass = "this",
            location = MatchAfterWithin(25)
        ),
        opcode(Opcode.IF_EQZ, location = MatchAfterWithin(5))
    )
)

val isDoubleTapField = findFieldDirect {
    DoubleTapToLikeLogicFingerprint.instructionMatches[3].instruction.fieldRef!!
}
