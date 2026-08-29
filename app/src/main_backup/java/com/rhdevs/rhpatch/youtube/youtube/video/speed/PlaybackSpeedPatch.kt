package com.rhdevs.rhpatch.youtube.youtube.video.speed

import com.rhdevs.rhpatch.youtube.extension.shared.settings.preference.NoTitlePreferenceCategory
import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.BasePreference
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.PreferenceCategory
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import com.rhdevs.rhpatch.youtube.youtube.misc.settings.PreferenceScreen
import com.rhdevs.rhpatch.youtube.youtube.video.speed.button.PlaybackSpeedButton
import com.rhdevs.rhpatch.youtube.youtube.video.speed.custom.CustomPlaybackSpeed
import com.rhdevs.rhpatch.youtube.youtube.video.speed.remember.RememberPlaybackSpeed

/**
 * Speed menu settings.  Used to organize all speed related settings together.
 */
internal val settingsMenuVideoSpeedGroup = mutableSetOf<BasePreference>()

@Suppress("unused")
val PlaybackSpeed = patch(
    name = "Playback speed",
    description = "Adds options to customize available playback speeds, set a default playback speed, " +
            "and show a speed dialog button in the video player.",
) {
    dependsOn(
        CustomPlaybackSpeed,
        RememberPlaybackSpeed,
        PlaybackSpeedButton,
    )

    PreferenceScreen.VIDEO.addPreferences(
        PreferenceCategory(
            key = "morphe_zz_video_key", // Dummy key to force the speed settings last.
            titleKey = null,
            sorting = Sorting.UNSORTED,
            tag = NoTitlePreferenceCategory::class.java,
            preferences = settingsMenuVideoSpeedGroup
        )
    )
}

