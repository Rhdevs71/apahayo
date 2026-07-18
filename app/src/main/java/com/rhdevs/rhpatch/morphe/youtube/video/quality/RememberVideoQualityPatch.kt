package com.rhdevs.rhpatch.morphe.youtube.video.quality

import app.morphe.extension.youtube.patches.playback.quality.RememberVideoQualityPatch
import com.rhdevs.rhpatch.getIntField
import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.scopedHook
import com.rhdevs.rhpatch.morphe.shared.misc.settings.preference.ListPreference
import com.rhdevs.rhpatch.morphe.shared.misc.settings.preference.SwitchPreference
import com.rhdevs.rhpatch.morphe.youtube.misc.playertype.PlayerTypeHook
import com.rhdevs.rhpatch.morphe.youtube.shared.VideoQualityReceiver
import com.rhdevs.rhpatch.morphe.youtube.shared.videoQualityChangedFingerprint
import com.rhdevs.rhpatch.morphe.youtube.video.information.VideoInformationPatch
import com.rhdevs.rhpatch.morphe.youtube.video.information.onCreateHook
import `j$`.util.Optional

val RememberVideoQuality = patch {
    dependsOn(
        VideoInformationPatch,
        PlayerTypeHook,
    )

    settingsMenuVideoQualityGroup.addAll(
        listOf(
            ListPreference(
                key = "morphe_video_quality_default_mobile",
                entriesKey = "morphe_video_quality_default_entries",
                entryValuesKey = "morphe_video_quality_default_entry_values"
            ),
            ListPreference(
                key = "morphe_video_quality_default_wifi",
                entriesKey = "morphe_video_quality_default_entries",
                entryValuesKey = "morphe_video_quality_default_entry_values"
            ),
            SwitchPreference("morphe_remember_video_quality_last_selected", summary = true),

            ListPreference(
                key = "morphe_shorts_quality_default_mobile",
                entriesKey = "morphe_shorts_quality_default_entries",
                entryValuesKey = "morphe_shorts_quality_default_entry_values",
            ),
            ListPreference(
                key = "morphe_shorts_quality_default_wifi",
                entriesKey = "morphe_shorts_quality_default_entries",
                entryValuesKey = "morphe_shorts_quality_default_entry_values"
            ),
            SwitchPreference("morphe_remember_shorts_quality_last_selected", summary = true),
            SwitchPreference("morphe_remember_video_quality_last_selected_toast", summary = true)
        )
    )

    onCreateHook.add { controller ->
        RememberVideoQualityPatch.newVideoStarted(controller)
    }

    // Inject a call to override initial video quality.
    ::PlaybackStartParametersInit.hookMethod {
        val initialResolutionField = ::InitialResolutionField.field
        after {
            val oldValue = initialResolutionField.get(it.thisObject)
            val newValue = RememberVideoQualityPatch.getInitialVideoQuality(oldValue as Optional<*>?)
            initialResolutionField.set(it.thisObject, newValue)
        }
    }

    // Inject a call to remember the selected quality for Shorts.
    ::videoQualityItemOnClickFingerprint.hookMethod {
        before { param ->
            RememberVideoQualityPatch.userChangedShortsQuality(param.args[2] as Int)
        }
    }

    // Inject a call to remember the user selected quality for regular videos.
    ::videoQualityChangedFingerprint.hookMethod(scopedHook(::VideoQualityReceiver.member) {
        before { param ->
            val selectedQualityIndex = param.args[0].getIntField("a")
            RememberVideoQualityPatch.userChangedQuality(selectedQualityIndex)
        }
    })
}
