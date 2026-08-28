package com.rhdevs.rhpatch.youtube.youtube.video.speed.button

import com.rhdevs.rhpatch.youtube.extension.youtube.videoplayer.PlaybackSpeedDialogButton
import com.rhdevs.rhpatch.R
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.SwitchPreference
import com.rhdevs.rhpatch.youtube.youtube.layout.buttons.overlay.addPlayerOverlayPreferences
import com.rhdevs.rhpatch.youtube.youtube.layout.player.buttons.addPlayerBottomButton
import com.rhdevs.rhpatch.youtube.youtube.layout.player.buttons.playerOverlayButtonsHook
import com.rhdevs.rhpatch.youtube.youtube.misc.playercontrols.ControlInitializer
import com.rhdevs.rhpatch.youtube.youtube.misc.playercontrols.LegacyPlayerControls
import com.rhdevs.rhpatch.youtube.youtube.misc.playercontrols.addLegacyBottomControl
import com.rhdevs.rhpatch.youtube.youtube.misc.playercontrols.initializeLegacyBottomControl
import com.rhdevs.rhpatch.youtube.youtube.video.information.VideoInformationPatch
import com.rhdevs.rhpatch.youtube.youtube.video.information.userSelectedPlaybackSpeedHook
import com.rhdevs.rhpatch.youtube.youtube.video.information.videoSpeedChangedHook
import com.rhdevs.rhpatch.youtube.youtube.video.speed.custom.CustomPlaybackSpeed
import com.rhdevs.rhpatch.patch

val PlaybackSpeedButton = patch(
    description = "Adds the option to display playback speed dialog button in the video player.",
) {
    dependsOn(
        CustomPlaybackSpeed,
        LegacyPlayerControls,
        playerOverlayButtonsHook,
        VideoInformationPatch,
    )

    addPlayerOverlayPreferences(
        SwitchPreference("morphe_playback_speed_dialog_button", summary = true),
    )

    addPlayerBottomButton(PlaybackSpeedDialogButton::initializeButton)

    addLegacyBottomControl(R.layout.morphe_playback_speed_dialog_button)
    initializeLegacyBottomControl(
        ControlInitializer(
            R.id.morphe_playback_speed_dialog_button_container,
            PlaybackSpeedDialogButton::initializeLegacyButton,
            PlaybackSpeedDialogButton::setVisibility,
            PlaybackSpeedDialogButton::setVisibilityImmediate,
            PlaybackSpeedDialogButton::setVisibilityNegatedImmediate,
        )
    )

    videoSpeedChangedHook.add { PlaybackSpeedDialogButton.videoSpeedChanged(it) }
    userSelectedPlaybackSpeedHook.add { PlaybackSpeedDialogButton.videoSpeedChanged(it) }
}

