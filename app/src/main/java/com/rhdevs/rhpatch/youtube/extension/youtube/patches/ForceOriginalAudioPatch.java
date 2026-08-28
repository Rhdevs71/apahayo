package com.rhdevs.rhpatch.youtube.extension.youtube.patches;

import com.rhdevs.rhpatch.youtube.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class ForceOriginalAudioPatch {

    /**
     * Injection point.
     */
    public static void setEnabled() {
        com.rhdevs.rhpatch.youtube.extension.shared.patches.ForceOriginalAudioPatch.setEnabled(
                Settings.FORCE_ORIGINAL_AUDIO.get(),
                Settings.SPOOF_VIDEO_STREAMS_CLIENT_TYPE.get()
        );
    }
}
