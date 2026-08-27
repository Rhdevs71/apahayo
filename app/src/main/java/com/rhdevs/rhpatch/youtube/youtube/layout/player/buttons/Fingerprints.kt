package com.rhdevs.rhpatch.youtube.youtube.layout.player.buttons

import com.rhdevs.rhpatch.youtube.AccessFlags
import com.rhdevs.rhpatch.youtube.Fingerprint
import com.rhdevs.rhpatch.youtube.Opcode
import com.rhdevs.rhpatch.youtube.ResourceType
import com.rhdevs.rhpatch.youtube.opcode
import com.rhdevs.rhpatch.youtube.resourceLiteral

internal object ExploderUIFullscreenButtonFingerprint : Fingerprint(
    classFingerprint = ExploderUIFullscreenButtonParentFingerprint,
    filters = listOf(
        resourceLiteral(ResourceType.ID, "fullscreen_button"),
        opcode(Opcode.MOVE_RESULT_OBJECT)
    )
)

private object ExploderUIFullscreenButtonParentFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    filters = listOf(
        resourceLiteral(ResourceType.ID, "time_bar_live_label")
    )
)
