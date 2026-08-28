package com.rhdevs.rhpatch.youtube.extension.youtube.patches;

import com.rhdevs.rhpatch.youtube.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class DisablePreciseSeekingGesturePatch {
    public static boolean isGestureDisabled() {
        return Settings.DISABLE_PRECISE_SEEKING_GESTURE.get();
    }
}
