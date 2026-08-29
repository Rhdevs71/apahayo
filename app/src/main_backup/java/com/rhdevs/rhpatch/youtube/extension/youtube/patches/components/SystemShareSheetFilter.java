/*
 * Copyright 2026 Morphe.
 * https://github.com/Morphecom/rhdevs/rhpatch/youtube-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package com.rhdevs.rhpatch.youtube.extension.youtube.patches.components;

import static com.rhdevs.rhpatch.youtube.extension.youtube.patches.OpenSystemShareSheetPatch.closeLithoAppShareSheet;

import com.rhdevs.rhpatch.youtube.extension.shared.patches.components.BufferAsciiStrings;
import com.rhdevs.rhpatch.youtube.extension.shared.patches.components.ContextInterface;
import com.rhdevs.rhpatch.youtube.extension.shared.patches.components.Filter;
import com.rhdevs.rhpatch.youtube.extension.shared.patches.components.StringFilterGroup;
import com.rhdevs.rhpatch.youtube.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class SystemShareSheetFilter extends Filter {

    public SystemShareSheetFilter() {
        addPathCallbacks(new StringFilterGroup(
                Settings.OPEN_SYSTEM_SHARE_SHEET,
                "share_sheet_container.e"
        ));
    }

    @Override
    public boolean isFiltered(ContextInterface contextInterface,
                              String identifier,
                              String accessibility,
                              String path,
                              byte[] buffer,
                              BufferAsciiStrings asciiStrings,
                              StringFilterGroup matchedGroup,
                              FilterContentType contentType,
                              int contentIndex) {
        closeLithoAppShareSheet();

        return true;
    }
}
