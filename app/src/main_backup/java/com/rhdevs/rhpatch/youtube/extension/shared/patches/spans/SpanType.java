/*
 * Copyright 2026 Morphe.
 * https://github.com/Morphecom/rhdevs/rhpatch/youtube-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package com.rhdevs.rhpatch.youtube.extension.shared.patches.spans;

import androidx.annotation.NonNull;

public enum SpanType {
    ABSOLUTE_SIZE("AbsoluteSizeSpan"),
    CLICKABLE("ClickableSpan"),
    CUSTOM_CHARACTER_STYLE("CustomCharacterStyle"),
    FOREGROUND_COLOR("ForegroundColorSpan"),
    IMAGE("ImageSpan"),
    LINE_HEIGHT("LineHeightSpan"),
    TYPEFACE("TypefaceSpan"),
    UNKNOWN("Unknown");

    @NonNull
    public final String type;

    SpanType(@NonNull String type) {
        this.type = type;
    }
}
