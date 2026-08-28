package com.rhdevs.rhpatch.youtube.extension.shared.patches.components;

import com.rhdevs.rhpatch.youtube.extension.shared.StringTrieSearch;

public class StringFilterGroupList extends FilterGroupList<String, StringFilterGroup> {
    protected StringTrieSearch createSearchGraph() {
        return new StringTrieSearch();
    }
}
