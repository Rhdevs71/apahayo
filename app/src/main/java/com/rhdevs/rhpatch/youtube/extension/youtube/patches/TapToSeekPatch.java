package com.rhdevs.rhpatch.youtube.extension.youtube.patches;

import com.rhdevs.rhpatch.youtube.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class TapToSeekPatch {
    public static boolean tapToSeekEnabled() {
        return Settings.TAP_TO_SEEK.get();
    }
}
