package com.rhdevs.rhpatch.morphe.youtube.video.audio

import app.morphe.extension.youtube.patches.ForceOriginalAudioPatch
import com.rhdevs.rhpatch.morphe.shared.misc.audio.tracks.forceOriginalAudioPatch
import com.rhdevs.rhpatch.morphe.youtube.misc.playservice.VersionCheck
import com.rhdevs.rhpatch.morphe.youtube.misc.playservice.is_21_26_or_greater
import com.rhdevs.rhpatch.morphe.youtube.misc.settings.PreferenceScreen
import com.rhdevs.rhpatch.morphe.youtube.shared.YouTubeActivityOnCreateFingerprint

val ForceOriginalAudio = forceOriginalAudioPatch(
    block =  {
        dependsOn(
            VersionCheck
        )
    },
    // Localized audio track flag was removed in 21.26+ but might be replaced with 45673827L
    fixUseLocalizedAudioTrackFlag = { !is_21_26_or_greater },
    mainActivityOnCreateFingerprint = YouTubeActivityOnCreateFingerprint,
    subclassExtensionSetEnabled = ForceOriginalAudioPatch::setEnabled,
    preferenceScreen = PreferenceScreen.VIDEO,
)
