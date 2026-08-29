package com.rhdevs.rhpatch.youtube.extension.youtube.patches;

import com.rhdevs.rhpatch.youtube.extension.youtube.patches.spoof.SpoofAppVersionPatch;
import com.rhdevs.rhpatch.youtube.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class TranscriptPatch {
    private static final boolean OVERRIDE_TRANSCRIPT_APP_VERSION = Settings.FIX_TRANSCRIPT.get()
            && !SpoofAppVersionPatch.isSpoofingToLessThan("20.05.00");

    /**
     * Injection point.
     */
    public static String getTranscriptAppVersionOverride(String version) {
        return OVERRIDE_TRANSCRIPT_APP_VERSION
                ? "20.05.46"
                : version;
    }
}
