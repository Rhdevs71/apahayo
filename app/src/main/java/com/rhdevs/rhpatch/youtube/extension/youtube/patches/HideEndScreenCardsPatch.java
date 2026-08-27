package com.rhdevs.rhpatch.youtube.extension.youtube.patches;

import android.view.View;

import com.rhdevs.rhpatch.youtube.extension.shared.Utils;
import com.rhdevs.rhpatch.youtube.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class HideEndScreenCardsPatch {

    /**
     * Injection point.
     */
    public static void hideEndScreenCardView(View view) {
        Utils.hideViewUnderCondition(Settings.HIDE_END_SCREEN_CARDS, view);
    }

    /**
     * Injection point.
     */
    public static boolean hideEndScreenCards() {
        return Settings.HIDE_END_SCREEN_CARDS.get();
    }
}