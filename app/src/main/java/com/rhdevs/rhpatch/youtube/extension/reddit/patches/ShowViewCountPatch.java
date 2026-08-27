/*
 * Copyright 2026 Morphe.
 * https://github.com/Morphecom/rhdevs/rhpatch/youtube-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package com.rhdevs.rhpatch.youtube.extension.reddit.patches;

import com.rhdevs.rhpatch.youtube.extension.reddit.settings.Settings;
import com.rhdevs.rhpatch.youtube.extension.shared.Logger;
import com.rhdevs.rhpatch.youtube.extension.shared.Utils;

@SuppressWarnings("unused")
public final class ShowViewCountPatch {
    private static final String ANDROID_POST_UNIT_VIEWS_COUNT = "android_post_unit_views_count_v2";

    /**
     * @return If this patch was included during patching.
     */
    public static boolean isPatchIncluded() {
        return false;  // Modified during patching.
    }

    /**
     * Injection point.
     */
    public static boolean showViewCount(String experimentName, boolean original) {
        // Can be called before app context is set.
        if (Utils.getContext() == null) {
            Logger.printInfo(() -> "Cannot show view count, context is not yet set");
            return original;
        }

        if (Settings.SHOW_VIEW_COUNT.get() && experimentName != null
                && experimentName.startsWith(ANDROID_POST_UNIT_VIEWS_COUNT)) {
            return true;
        }

        return original;
    }
}
