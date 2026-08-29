package com.rhdevs.rhpatch.youtube.extension.youtube.patches;

import androidx.annotation.Nullable;

import com.rhdevs.rhpatch.youtube.extension.youtube.shared.PlayerControlsVisibility;

@SuppressWarnings("unused")
public class PlayerControlsVisibilityHookPatch {

    /**
     * Injection point.
     */
    public static void setPlayerControlsVisibility(@Nullable Enum<?> youTubePlayerControlsVisibility) {
        if (youTubePlayerControlsVisibility == null) return;

        PlayerControlsVisibility.setFromString(youTubePlayerControlsVisibility.name());
    }
}
