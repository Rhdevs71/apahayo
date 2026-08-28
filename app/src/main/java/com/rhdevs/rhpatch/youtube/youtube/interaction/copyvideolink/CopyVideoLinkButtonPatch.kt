package com.rhdevs.rhpatch.youtube.youtube.interaction.copyvideolink

import com.rhdevs.rhpatch.youtube.extension.youtube.videoplayer.CopyVideoLinkButton
import com.rhdevs.rhpatch.R
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.SwitchPreference
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.noTitleUnsortedPreferenceCategory
import com.rhdevs.rhpatch.youtube.youtube.layout.buttons.overlay.addPlayerOverlayPreferences
import com.rhdevs.rhpatch.youtube.youtube.layout.player.buttons.addPlayerBottomButton
import com.rhdevs.rhpatch.youtube.youtube.layout.player.buttons.playerOverlayButtonsHook
import com.rhdevs.rhpatch.youtube.youtube.misc.playercontrols.ControlInitializer
import com.rhdevs.rhpatch.youtube.youtube.misc.playercontrols.LegacyPlayerControls
import com.rhdevs.rhpatch.youtube.youtube.misc.playercontrols.addLegacyBottomControl
import com.rhdevs.rhpatch.youtube.youtube.misc.playercontrols.initializeLegacyBottomControl
import com.rhdevs.rhpatch.youtube.youtube.video.information.VideoInformationPatch
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
