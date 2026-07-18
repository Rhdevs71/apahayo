package com.rhdevs.rhpatch.morphe.youtube.video.quality

import app.morphe.extension.shared.settings.preference.NoTitlePreferenceCategory
import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.morphe.shared.misc.settings.preference.BasePreference
import com.rhdevs.rhpatch.morphe.shared.misc.settings.preference.PreferenceCategory
import com.rhdevs.rhpatch.morphe.shared.misc.settings.preference.PreferenceScreenPreference
import com.rhdevs.rhpatch.morphe.shared.misc.litho.filter.featureFlagCheck
import com.rhdevs.rhpatch.morphe.youtube.misc.playservice.VersionCheck
import com.rhdevs.rhpatch.morphe.youtube.misc.playservice.is_20_40_or_greater
import com.rhdevs.rhpatch.morphe.youtube.misc.settings.PreferenceScreen

val settingsMenuVideoQualityGroup = mutableSetOf<BasePreference>()

val VideoQuality = patch(
    name = "Video quality",
    description = "Adds options to set default video qualities and always use the advanced video quality menu."
) {
    dependsOn(
        RememberVideoQuality,
        AdvancedVideoQualityMenu,
        VideoQualityDialogButtonPatch,
        VersionCheck
    )

    PreferenceScreen.VIDEO.addPreferences(
        // Keep the preferences organized together.
        PreferenceCategory(
            key = "morphe_01_video_key", // Dummy key to force the quality preferences first.
            titleKey = null,
            sorting = PreferenceScreenPreference.Sorting.UNSORTED,
            tag = NoTitlePreferenceCategory::class.java,
            preferences = settingsMenuVideoQualityGroup
        )
    )

    // Flag breaks opening advanced quality menu for 20.40+.
    if (is_20_40_or_greater) {
        ::featureFlagCheck.hookMethod {
            before {
                if (it.args[0] == 45712556L) it.result = false
            }
        }
    }
}
