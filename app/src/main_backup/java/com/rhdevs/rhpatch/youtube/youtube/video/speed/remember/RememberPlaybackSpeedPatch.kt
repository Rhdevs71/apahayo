package com.rhdevs.rhpatch.youtube.youtube.video.speed.remember

import com.rhdevs.rhpatch.youtube.extension.youtube.patches.playback.speed.RememberPlaybackSpeedPatch
import com.rhdevs.rhpatch.youtube.extension.youtube.settings.preference.CustomVideoSpeedListPreference
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.ListPreference
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.SwitchPreference
import com.rhdevs.rhpatch.youtube.youtube.video.information.VideoInformationPatch
import com.rhdevs.rhpatch.youtube.youtube.video.information.onCreateHook
import com.rhdevs.rhpatch.youtube.youtube.video.information.setPlaybackSpeedClassField
import com.rhdevs.rhpatch.youtube.youtube.video.information.setPlaybackSpeedContainerClassField
import com.rhdevs.rhpatch.youtube.youtube.video.information.setPlaybackSpeedMethod
import com.rhdevs.rhpatch.youtube.youtube.video.information.userSelectedPlaybackSpeedHook
import com.rhdevs.rhpatch.youtube.youtube.video.speed.custom.CustomPlaybackSpeed
import com.rhdevs.rhpatch.youtube.youtube.video.speed.settingsMenuVideoSpeedGroup
import com.rhdevs.rhpatch.youtube.youtube.video.videoid.VideoId
import com.rhdevs.rhpatch.youtube.youtube.video.videoid.hookPlayerResponseVideoId
import com.rhdevs.rhpatch.patch

val RememberPlaybackSpeed = patch {
    dependsOn(
        VideoId,
        VideoInformationPatch,
        CustomPlaybackSpeed
    )

    settingsMenuVideoSpeedGroup.addAll(
        listOf(
            ListPreference(
                key = "morphe_playback_speed_default",
                // Entries and values are set by the extension code based on the actual speeds available.
                entriesKey = null,
                entryValuesKey = null,
                tag = CustomVideoSpeedListPreference::class.java
            ),
            SwitchPreference("morphe_remember_playback_speed_last_selected", summary = true),
            SwitchPreference("morphe_remember_playback_speed_last_selected_toast", summary = true),
            SwitchPreference("morphe_disable_playback_speed_music", summary = true)
        )
    )

    onCreateHook.add { RememberPlaybackSpeedPatch.newVideoStarted(it) }

    userSelectedPlaybackSpeedHook.add { RememberPlaybackSpeedPatch.userSelectedPlaybackSpeed(it) }

    hookPlayerResponseVideoId(RememberPlaybackSpeedPatch::preloadMusicVideoFetch)

    /*
     * Hook the code that is called when the playback speeds are initialized, and sets the playback speed
     */
    ::initializePlaybackSpeedValuesFingerprint.hookMethod {
        val onItemClickListenerClassField = ::onItemClickListenerClassFieldReference.field
        before {
            val playbackSpeedOverride = RememberPlaybackSpeedPatch.getPlaybackSpeedOverride()
            if (playbackSpeedOverride > 0.0f) {
                onItemClickListenerClassField.get(it.thisObject)
                    .let { setPlaybackSpeedContainerClassField.get(it) }
                    .let { setPlaybackSpeedClassField.get(it) }
                    .let { setPlaybackSpeedMethod(it, playbackSpeedOverride) }
            }
        }
    }
}
