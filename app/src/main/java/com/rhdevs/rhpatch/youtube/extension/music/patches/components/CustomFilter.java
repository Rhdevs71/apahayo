/*
 * Copyright 2026 Morphe.
 * https://github.com/Morphecom/rhdevs/rhpatch/youtube-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package com.rhdevs.rhpatch.youtube.extension.music.patches.components;

import com.rhdevs.rhpatch.youtube.extension.music.settings.Settings;
import com.rhdevs.rhpatch.youtube.extension.shared.patches.components.BaseCustomFilter;

/**
 * YT Music entry point for the shared custom filter. See {@link BaseCustomFilter} for the
 * expression syntax reference.
 */
@SuppressWarnings("unused")
public final class CustomFilter extends BaseCustomFilter {

    public CustomFilter() {
        super(
                Settings.CUSTOM_FILTER,
                Settings.CUSTOM_FILTER_STRINGS,
                "morphe_custom_filter_toast_invalid_syntax"
        );
    }
}
