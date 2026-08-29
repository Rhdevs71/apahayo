package com.rhdevs.rhpatch.youtube.extension.youtube.patches;

import com.rhdevs.rhpatch.youtube.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class HideEndScreenSuggestedVideoPatch {
    /**
     * Injection point.
     */
    public static boolean hideEndScreenSuggestedVideo() {
        return Settings.HIDE_END_SCREEN_SUGGESTED_VIDEO.get();
    }
}
