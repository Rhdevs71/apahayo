/*
 * Copyright 2026 Morphe.
 * https://github.com/Morphecom/rhdevs/rhpatch/youtube-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package com.rhdevs.rhpatch.youtube.extension.reddit.patches;

import com.rhdevs.rhpatch.youtube.extension.reddit.settings.Settings;

@SuppressWarnings("unused")
public class DisableScreenshotPopupPatch {

    /**
     * @return If this patch was included during patching.
     */
    public static boolean isPatchIncluded() {
        return false;  // Modified during patching.
    }

    /**
     * Injection point.
     */
    public static Boolean disableScreenshotPopup(Boolean original) {
        return Settings.DISABLE_SCREENSHOT_POPUP.get() ? Boolean.FALSE : original;
    }
}
