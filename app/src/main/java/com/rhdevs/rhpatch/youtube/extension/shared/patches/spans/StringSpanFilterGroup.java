/*
 * Copyright 2026 Morphe.
 * https://github.com/Morphecom/rhdevs/rhpatch/youtube-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package com.rhdevs.rhpatch.youtube.extension.shared.patches.spans;

import com.rhdevs.rhpatch.youtube.extension.shared.settings.BooleanSetting;

public class StringSpanFilterGroup extends SpanFilterGroup<String> {

    public StringSpanFilterGroup(final BooleanSetting setting, final String... filters) {
        super(setting, filters);
    }

    @Override
    public SpanFilterGroup.FilterGroupResult check(final String string) {
        int matchedIndex = -1;
        if (isEnabled()) {
            for (String pattern : filters) {
                if (!string.isEmpty()) {
                    final int indexOf = string.indexOf(pattern);
                    if (indexOf >= 0) {
                        matchedIndex = indexOf;
                        break;
                    }
                }
            }
        }
        return new SpanFilterGroup.FilterGroupResult(setting, matchedIndex);
    }
}
