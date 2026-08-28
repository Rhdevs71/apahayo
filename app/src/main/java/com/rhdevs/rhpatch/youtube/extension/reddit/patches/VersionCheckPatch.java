/*
 * Copyright 2026 Morphe.
 * https://github.com/Morphecom/rhdevs/rhpatch/youtube-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package com.rhdevs.rhpatch.youtube.extension.reddit.patches;

import com.rhdevs.rhpatch.youtube.extension.shared.Utils;

public class VersionCheckPatch {
    @SuppressWarnings("SameParameterValue")
    private static boolean isVersionOrGreater(String version) {
        return Utils.getAppVersionName().compareTo(version) >= 0;
    }

    public static final boolean is_2025_52_or_greater = isVersionOrGreater("2025.52.0");
}
