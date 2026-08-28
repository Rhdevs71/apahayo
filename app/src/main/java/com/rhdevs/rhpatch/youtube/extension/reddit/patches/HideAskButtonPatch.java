/*
 * Copyright 2026 Morphe.
 * https://github.com/Morphecom/rhdevs/rhpatch/youtube-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package com.rhdevs.rhpatch.youtube.extension.reddit.patches;

import com.rhdevs.rhpatch.youtube.extension.reddit.settings.Settings;

@SuppressWarnings("unused")
public final class HideAskButtonPatch {
    private static final String ANDROID_SEARCH_BAR_ASK_BUTTON = "android_search_bar_ask_button";

    /**
     * @return If this patch was included during patching.
     */
    public static boolean isPatchIncluded() {
        return false;  // Modified during patching.
    }

    /**
     * Injection point.
     */
    public static boolean hideAskButton(String experimentName, boolean original) {
        if (Settings.HIDE_ASK_BUTTON.get() && experimentName != null && experimentName.startsWith(ANDROID_SEARCH_BAR_ASK_BUTTON)) {
            return false;
        }

        return original;
    }

    /**
     * Injection point.
     */
    public static boolean shouldHideAskButton() {
        return Settings.HIDE_ASK_BUTTON.get();
    }
}
