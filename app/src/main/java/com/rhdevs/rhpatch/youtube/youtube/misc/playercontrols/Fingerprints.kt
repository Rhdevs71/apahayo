package com.rhdevs.rhpatch.youtube.youtube.misc.playercontrols

import com.rhdevs.rhpatch.RequireAppVersion
import com.rhdevs.rhpatch.SkipTest
import com.rhdevs.rhpatch.youtube.AccessFlags
import com.rhdevs.rhpatch.youtube.Fingerprint
import com.rhdevs.rhpatch.youtube.InstructionLocation.MatchAfterImmediately
import com.rhdevs.rhpatch.youtube.Opcode
import com.rhdevs.rhpatch.youtube.OpcodesFilter
import com.rhdevs.rhpatch.youtube.ResourceType
import com.rhdevs.rhpatch.youtube.checkCast
import com.rhdevs.rhpatch.youtube.literal
import com.rhdevs.rhpatch.youtube.methodCall
import com.rhdevs.rhpatch.youtube.opcode
import com.rhdevs.rhpatch.youtube.resourceLiteral
import com.rhdevs.rhpatch.youtube.resourceMappings

val fullscreen_button_id get() = resourceMappings["id", "fullscreen_button"]

internal object PlayerControlsVisibilityEntityModelInit : Fingerprint(
    classFingerprint = PlayerControlsVisibilityEntityModelFingerprint,
    name = "<init>"
)

internal object PlayerControlsVisibilityEntityModelFingerprint : Fingerprint(
    name = "getPlayerControlsVisibility",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "L",
    parameters = listOf(),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET,
        Opcode.INVOKE_STATIC
    )
)

private object YoutubeControlsOverlayFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(name = "setFocusableInTouchMode"),
        resourceLiteral(ResourceType.ID, "inset_overlay_view_layout"),
        resourceLiteral(ResourceType.ID, "scrim_overlay"),
    )
)

internal object MotionEventFingerprint : Fingerprint(
    classFingerprint = YoutubeControlsOverlayFingerprint,
    returnType = "V",
    parameters = listOf("Landroid/view/MotionEvent;"),
    filters = listOf(
        methodCall(name = "getPaddingTop")
    )
)

object PlayerTopControlsInflateFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        resourceLiteral(ResourceType.ID, "controls_layout_stub")
    )
)

@SkipTest
internal object PlayerBottomControlsInflateFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf(),
    filters = listOf(
        resourceLiteral(ResourceType.ID, "bottom_ui_container_stub"),
        methodCall(definingClass = "Landroid/view/ViewStub;", name = "inflate"),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately())
    )
)

internal object OverlayViewInflateFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/view/View;"),
    filters = listOf(
        resourceLiteral(ResourceType.ID, "heatseeker_viewstub"),
        resourceLiteral(ResourceType.ID, "fullscreen_button"),
        checkCast("Landroid/widget/ImageView;")
    )
)

object ControlsOverlayVisibilityFingerprint : Fingerprint(
    classFingerprint = PlayerTopControlsInflateFingerprint,
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Z", "Z"),
)

internal object PlayerBottomControlsExploderFeatureFlagFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        literal(45643739L)
    )
)

@RequireAppVersion("20.28.00")
internal object PlayerControlsLargeOverlayButtonsFeatureFlagFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        literal(45709810L)
    )
)

internal object PlayerControlsFullscreenLargeButtonsFeatureFlagFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        literal(45686474L)
    )
)

@RequireAppVersion("20.30.00")
internal object PlayerControlsButtonStrokeFeatureFlagFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        literal(45713296)
    )
)
