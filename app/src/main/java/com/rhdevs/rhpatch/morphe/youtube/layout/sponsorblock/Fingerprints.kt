package com.rhdevs.rhpatch.morphe.youtube.layout.sponsorblock

import com.rhdevs.rhpatch.morphe.AccessFlags
import com.rhdevs.rhpatch.morphe.Fingerprint
import com.rhdevs.rhpatch.morphe.InstructionLocation.MatchAfterWithin
import com.rhdevs.rhpatch.morphe.Opcode
import com.rhdevs.rhpatch.morphe.OpcodesFilter
import com.rhdevs.rhpatch.morphe.fieldAccess
import com.rhdevs.rhpatch.morphe.findFieldDirect
import com.rhdevs.rhpatch.morphe.findMethodDirect
import com.rhdevs.rhpatch.morphe.opcode
import com.rhdevs.rhpatch.morphe.resourceMappings
import com.rhdevs.rhpatch.morphe.youtube.shared.seekbarFingerprint

internal object AppendTimeFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        "Ljava/lang/CharSequence;", "Ljava/lang/CharSequence;", "Ljava/lang/CharSequence;"
    ),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
    ),
)

val SponsorBarRect = findFieldDirect {
    val clazz = seekbarFingerprint().declaredClass!!
    clazz.findMethod {
        matcher {
            addInvoke {
                name = "invalidate"
                paramTypes("android.graphics.Rect")
            }
        }
    }.single().usingFields.last { it.field.typeName == "android.graphics.Rect" }.field
}

val seekbarOnDrawFingerprint = findMethodDirect {
    seekbarFingerprint().declaredClass!!.findMethod {
        matcher {
            name = "onDraw"
        }
    }.single()
}

val inset_overlay_view_layout get() = resourceMappings["id", "inset_overlay_view_layout"]

val controlsOverlayFingerprint = findMethodDirect {
    findMethod {
        matcher {
            addUsingNumber(inset_overlay_view_layout)
            paramCount = 0
            returnType = "void"
        }
    }.single()
}

internal object AdProgressTextViewVisibilityFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Z"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Ljava/lang/Object;"
        ),
        opcode(opcode = Opcode.CHECK_CAST, location = MatchAfterWithin(4)),
    ),
    custom = {
        addInvoke {
            descriptor =
                "Lcom/google/android/libraries/youtube/ads/player/ui/AdProgressTextView;->setVisibility(I)V"
        }
    }
)

val AdProgressTextField = findFieldDirect {
    AdProgressTextViewVisibilityFingerprint.instructionMatches[0].instruction.fieldRef!!
}
