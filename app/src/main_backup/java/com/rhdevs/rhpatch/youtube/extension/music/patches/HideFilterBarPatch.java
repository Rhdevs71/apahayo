package com.rhdevs.rhpatch.youtube.extension.music.patches;

import static com.rhdevs.rhpatch.youtube.extension.shared.Utils.hideViewBy0dpUnderCondition;

import android.view.View;

import com.rhdevs.rhpatch.youtube.extension.music.settings.Settings;

@SuppressWarnings("unused")
public class HideFilterBarPatch {

    /**
     * Injection point
     */
    public static void hideFilterBar(View view) {
        hideViewBy0dpUnderCondition(Settings.HIDE_FILTER_BAR, view);
    }
}
