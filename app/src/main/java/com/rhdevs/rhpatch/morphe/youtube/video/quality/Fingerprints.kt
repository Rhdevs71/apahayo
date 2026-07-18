package com.rhdevs.rhpatch.morphe.youtube.video.quality

import com.rhdevs.rhpatch.morphe.AccessFlags
import com.rhdevs.rhpatch.morphe.Fingerprint
import com.rhdevs.rhpatch.morphe.Opcode
import com.rhdevs.rhpatch.morphe.accessFlags
import com.rhdevs.rhpatch.morphe.fieldAccess
import com.rhdevs.rhpatch.morphe.findFieldDirect
import com.rhdevs.rhpatch.morphe.findFieldFromToString
import com.rhdevs.rhpatch.morphe.findMethodDirect
import com.rhdevs.rhpatch.morphe.findMethodListDirect
import com.rhdevs.rhpatch.morphe.fingerprint
import com.rhdevs.rhpatch.morphe.literal
import com.rhdevs.rhpatch.morphe.opcodes
import com.rhdevs.rhpatch.morphe.parameters
import com.rhdevs.rhpatch.morphe.resourceMappings
import com.rhdevs.rhpatch.morphe.returns
import com.rhdevs.rhpatch.morphe.string

internal object NewAdvancedQualityMenuStyleFlyout : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    filters = listOf(literal(45712556))
)

internal const val FIXED_RESOLUTION_STRING = ", initialPlaybackVideoQualityFixedResolution="

internal object PlaybackStartParametersToStringFingerprint : Fingerprint(
    name = "toString",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    filters = listOf(
        string(FIXED_RESOLUTION_STRING)
    )
)

val InitialResolutionField = findFieldDirect {
    PlaybackStartParametersToStringFingerprint().findFieldFromToString(FIXED_RESOLUTION_STRING)
}

val PlaybackStartParametersInit = findMethodDirect {
    Fingerprint(
        classFingerprint = PlaybackStartParametersToStringFingerprint,
        name = "<init>",
        filters = listOf(
            fieldAccess(
                opcode = Opcode.IPUT_OBJECT,
                reference = InitialResolutionField()
            )
        )
    )()
}

val videoQualityItemOnClickParentFingerprint = fingerprint {
    returns("V")
    strings("VIDEO_QUALITIES_MENU_BOTTOM_SHEET_FRAGMENT")
}

/**
 * Resolves to class found in [videoQualityItemOnClickFingerprint].
 */
val videoQualityItemOnClickFingerprint = fingerprint {
    classFingerprint(videoQualityItemOnClickParentFingerprint)
    methodMatcher { name = "onItemClick" }
}

val videoQualityQuickMenuAdvancedMenuDescription get() = resourceMappings[
    "string",
    "video_quality_quick_menu_advanced_menu_description",
]

val videoQualityMenuOptionsFingerprint = fingerprint {
    accessFlags(AccessFlags.STATIC)
    returns("[L")
    parameters("Landroid/content/Context", "L", "L")
    opcodes(
        Opcode.CONST_4, // First instruction of method.
        Opcode.CONST_4,
        Opcode.IF_EQZ,
        Opcode.IGET_BOOLEAN, // Use the quality menu, that contains the advanced menu.
        Opcode.IF_NEZ,
    )
    literal { videoQualityQuickMenuAdvancedMenuDescription }
}
val videoQualityBottomSheetListFragmentTitle get() = resourceMappings[
    "layout",
    "video_quality_bottom_sheet_list_fragment_title",
]

val videoQualityMenuViewInflateFingerprint = findMethodListDirect {
    // two matches in versions 20.43.32
    // one match in versions <=v20.42.xx
    findMethod {
        matcher {
            accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
            returns("L")
            parameters("L", "L", "L")
            opcodes(
                Opcode.INVOKE_SUPER,
                Opcode.CONST,
                Opcode.CONST_4,
                Opcode.INVOKE_VIRTUAL,
                Opcode.MOVE_RESULT_OBJECT,
                Opcode.CONST,
                Opcode.INVOKE_VIRTUAL,
                Opcode.MOVE_RESULT_OBJECT,
                Opcode.CONST_16,
                Opcode.INVOKE_VIRTUAL,
                Opcode.CONST,
                Opcode.INVOKE_VIRTUAL,
                Opcode.MOVE_RESULT_OBJECT,
                Opcode.CHECK_CAST,
            )
            literal { videoQualityBottomSheetListFragmentTitle }
        }
    }
}
