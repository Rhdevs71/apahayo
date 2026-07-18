package com.rhdevs.rhpatch.morphe.youtube.video.quality

import app.morphe.extension.youtube.videoplayer.VideoQualityDialogButton
import com.wmods.wppenhacer.R
import com.rhdevs.rhpatch.morphe.shared.misc.settings.preference.SwitchPreference
import com.rhdevs.rhpatch.morphe.youtube.layout.buttons.overlay.addPlayerOverlayPreferences
import com.rhdevs.rhpatch.morphe.youtube.layout.player.buttons.addPlayerBottomButton
import com.rhdevs.rhpatch.morphe.youtube.layout.player.buttons.playerOverlayButtonsHook
import com.rhdevs.rhpatch.morphe.youtube.misc.playercontrols.ControlInitializer
import com.rhdevs.rhpatch.morphe.youtube.misc.playercontrols.LegacyPlayerControls
import com.rhdevs.rhpatch.morphe.youtube.misc.playercontrols.addLegacyBottomControl
import com.rhdevs.rhpatch.morphe.youtube.misc.playercontrols.initializeLegacyBottomControl
import com.rhdevs.rhpatch.patch

val VideoQualityDialogButtonPatch = patch(
    description = "Adds the option to display video quality dialog button in the video player.",
) {
    dependsOn(
        RememberVideoQuality,
        LegacyPlayerControls,
        playerOverlayButtonsHook
    )

    addPlayerOverlayPreferences(
        SwitchPreference("morphe_video_quality_dialog_button", summary = true),
    )
    addPlayerBottomButton(VideoQualityDialogButton::initializeButton)

    addLegacyBottomControl(R.layout.morphe_video_quality_dialog_button_container)
    initializeLegacyBottomControl(
        ControlInitializer(
            R.id.morphe_video_quality_dialog_button_container,
            VideoQualityDialogButton::initializeLegacyButton,
            VideoQualityDialogButton::setVisibility,
            VideoQualityDialogButton::setVisibilityImmediate,
            VideoQualityDialogButton::setVisibilityNegatedImmediate
        )
    )
}
