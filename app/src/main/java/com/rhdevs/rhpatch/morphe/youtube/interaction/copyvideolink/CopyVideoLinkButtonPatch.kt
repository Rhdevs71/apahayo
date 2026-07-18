package com.rhdevs.rhpatch.morphe.youtube.interaction.copyvideolink

import app.morphe.extension.youtube.videoplayer.CopyVideoLinkButton
import com.wmods.wppenhacer.R
import com.rhdevs.rhpatch.morphe.shared.misc.settings.preference.SwitchPreference
import com.rhdevs.rhpatch.morphe.shared.misc.settings.preference.noTitleUnsortedPreferenceCategory
import com.rhdevs.rhpatch.morphe.youtube.layout.buttons.overlay.addPlayerOverlayPreferences
import com.rhdevs.rhpatch.morphe.youtube.layout.player.buttons.addPlayerBottomButton
import com.rhdevs.rhpatch.morphe.youtube.layout.player.buttons.playerOverlayButtonsHook
import com.rhdevs.rhpatch.morphe.youtube.misc.playercontrols.ControlInitializer
import com.rhdevs.rhpatch.morphe.youtube.misc.playercontrols.LegacyPlayerControls
import com.rhdevs.rhpatch.morphe.youtube.misc.playercontrols.addLegacyBottomControl
import com.rhdevs.rhpatch.morphe.youtube.misc.playercontrols.initializeLegacyBottomControl
import com.rhdevs.rhpatch.morphe.youtube.video.information.VideoInformationPatch
import com.rhdevs.rhpatch.patch

val CopyVideoLinkButtonPatch = patch(
    name = "Copy video link",
    description = "Adds options to display buttons in the video player to copy video links.",
) {
    dependsOn(
        LegacyPlayerControls,
        playerOverlayButtonsHook,
        VideoInformationPatch,
    )

    addPlayerOverlayPreferences(
        noTitleUnsortedPreferenceCategory(
            SwitchPreference("morphe_copy_video_link_button", summary = true),
            SwitchPreference("morphe_copy_video_link_with_timestamp_button", summary = true)
        )
    )
    addPlayerBottomButton(CopyVideoLinkButton::initializeButton)

    addLegacyBottomControl(R.layout.morphe_copy_video_url_button)
    initializeLegacyBottomControl(
        ControlInitializer(
            R.id.morphe_copy_video_url_button,
            CopyVideoLinkButton::initializeLegacyButton,
            CopyVideoLinkButton::setVisibility,
            CopyVideoLinkButton::setVisibilityImmediate,
            CopyVideoLinkButton::setVisibilityNegatedImmediate
        )
    )
}
