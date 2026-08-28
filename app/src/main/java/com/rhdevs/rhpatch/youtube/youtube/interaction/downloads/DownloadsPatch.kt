package com.rhdevs.rhpatch.youtube.youtube.interaction.downloads

import android.app.Activity
import com.rhdevs.rhpatch.youtube.extension.shared.settings.preference.ExternalDownloaderPreference
import com.rhdevs.rhpatch.youtube.extension.youtube.patches.DownloadsPatch
import com.rhdevs.rhpatch.youtube.extension.youtube.videoplayer.ExternalDownloadButton
import com.rhdevs.rhpatch.R
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.PreferenceScreenPreference
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.SwitchPreference
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.TextPreference
import com.rhdevs.rhpatch.youtube.youtube.misc.playercontrols.ControlInitializer
import com.rhdevs.rhpatch.youtube.youtube.misc.playercontrols.LegacyPlayerControls
import com.rhdevs.rhpatch.youtube.youtube.misc.playercontrols.addTopControl
import com.rhdevs.rhpatch.youtube.youtube.misc.playercontrols.initializeTopControl
import com.rhdevs.rhpatch.youtube.youtube.misc.settings.PreferenceScreen
import com.rhdevs.rhpatch.youtube.youtube.shared.mainActivityOnCreateFingerprint
import com.rhdevs.rhpatch.youtube.youtube.video.information.VideoInformationPatch
import com.rhdevs.rhpatch.patch

val Downloads = patch(
    name = "Downloads",
    description = "Adds support to download videos with an external downloader app " +
            "using the in-app download button or a video player action button.",
) {
    dependsOn(
        LegacyPlayerControls,
        VideoInformationPatch,
    )

    PreferenceScreen.PLAYER.addPreferences(
        PreferenceScreenPreference(
            key = "morphe_external_downloader_screen",
            sorting = PreferenceScreenPreference.Sorting.UNSORTED,
            preferences = setOf(
                SwitchPreference("morphe_external_downloader", summary = true),
                SwitchPreference("morphe_external_downloader_action_button", summary = true),
                TextPreference(
                    "morphe_external_downloader_name",
                    tag = ExternalDownloaderPreference::class.java
                ),
            ),
        ),
    )

    addTopControl(
        R.layout.morphe_external_download_button,
        R.id.morphe_external_download_button,
        R.id.morphe_external_download_button
    )

    initializeTopControl(
        ControlInitializer(
            R.id.morphe_external_download_button,
            ExternalDownloadButton::initializeLegacyButton,
            ExternalDownloadButton::setVisibility,
            ExternalDownloadButton::setVisibilityImmediate,
            ExternalDownloadButton::setVisibilityNegatedImmediate,
        )
    )

    OfflineVideoEndpointFingerprint.hookMethod {
        before {
            if (DownloadsPatch.inAppDownloadButtonOnClick(it.args[2] as String)) it.result = null
        }
    }
}
