package com.rhdevs.rhpatch.youtube.youtube.video.audio

import com.rhdevs.rhpatch.youtube.extension.youtube.patches.ForceOriginalAudioPatch
import com.rhdevs.rhpatch.youtube.shared.misc.audio.tracks.forceOriginalAudioPatch
import com.rhdevs.rhpatch.youtube.youtube.misc.playservice.VersionCheck
import com.rhdevs.rhpatch.youtube.youtube.misc.playservice.is_21_26_or_greater
import com.rhdevs.rhpatch.youtube.youtube.misc.settings.PreferenceScreen
import com.rhdevs.rhpatch.youtube.youtube.shared.YouTubeActivityOnCreateFingerprint

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
