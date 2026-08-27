/*
 * Copyright 2026 Morphe.
 * https://github.com/Morphecom/rhdevs/rhpatch/youtube-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package com.rhdevs.rhpatch.youtube.extension.youtube.patches;

import com.rhdevs.rhpatch.youtube.extension.youtube.settings.Settings;
import com.rhdevs.rhpatch.youtube.extension.youtube.settings.YouTubeActivityHook;

@SuppressWarnings("unused")
public class FixPreferenceIconPatch {
    private static final boolean REMOVE_BROKEN_PREFERENCE_ICON =
            Settings.RESTORE_OLD_SETTINGS_MENUS.get() || !YouTubeActivityHook.useBoldIcons(true);

    /**
     * Injection point.
     */
    public static boolean removePreferenceIcon() {
        return REMOVE_BROKEN_PREFERENCE_ICON;
    }
}

