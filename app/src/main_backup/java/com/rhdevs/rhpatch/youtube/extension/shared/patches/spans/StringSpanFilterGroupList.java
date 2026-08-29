/*
 * Copyright 2026 Morphe.
 * https://github.com/Morphecom/rhdevs/rhpatch/youtube-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package com.rhdevs.rhpatch.youtube.extension.shared.patches.spans;

import com.rhdevs.rhpatch.youtube.extension.shared.StringTrieSearch;

public final class StringSpanFilterGroupList extends SpanFilterGroupList<String, StringSpanFilterGroup> {
    protected StringTrieSearch createSearchGraph() {
        return new StringTrieSearch();
    }
}
