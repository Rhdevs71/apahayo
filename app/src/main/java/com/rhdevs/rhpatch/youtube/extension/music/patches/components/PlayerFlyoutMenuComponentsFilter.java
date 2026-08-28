/*
 * Copyright 2026 Morphe.
 * https://github.com/Morphecom/rhdevs/rhpatch/youtube-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package com.rhdevs.rhpatch.youtube.extension.music.patches.components;

import com.rhdevs.rhpatch.youtube.extension.music.settings.Settings;
import com.rhdevs.rhpatch.youtube.extension.shared.patches.components.BufferAsciiStrings;
import com.rhdevs.rhpatch.youtube.extension.shared.patches.components.ByteArrayFilterGroup;
import com.rhdevs.rhpatch.youtube.extension.shared.patches.components.ByteArrayFilterGroupList;
import com.rhdevs.rhpatch.youtube.extension.shared.patches.components.ContextInterface;
import com.rhdevs.rhpatch.youtube.extension.shared.patches.components.Filter;
import com.rhdevs.rhpatch.youtube.extension.shared.patches.components.StringFilterGroup;

/**
 * Hides Litho-rendered components inside the YT Music player/queue flyout menu.
 */
@SuppressWarnings("unused")
public final class PlayerFlyoutMenuComponentsFilter extends Filter {

    private static final String LIST_ITEM_ROOT_PREFIX = "list_item.";

    private final StringFilterGroup listItem;
    private final ByteArrayFilterGroupList bufferGroupList = new ByteArrayFilterGroupList();

    public PlayerFlyoutMenuComponentsFilter() {
        addIdentifierCallbacks(
                new StringFilterGroup(
                        Settings.HIDE_FLYOUT_MENU_3_COLUMN_COMPONENT,
                        "music_highlight_menu_item_carousel.",
                        "tile_button_carousel."
                ),
                new StringFilterGroup(
                        Settings.HIDE_FLYOUT_MENU_LIKE_DISLIKE,
                        "like_toggle_button."
                )
        );

        listItem = new StringFilterGroup(
                null,
                LIST_ITEM_ROOT_PREFIX
        );

        bufferGroupList.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_FLYOUT_MENU_DOWNLOAD,
                        "yt_outline_download",
                        "yt_outline_experimental_download"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_FLYOUT_MENU_TASTE_MATCH,
                        "yt_outline_circles_overlap",
                        "yt_outline_experimental_account_link_vd_theme_24"
                )
        );

        addPathCallbacks(listItem);
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
        if (matchedGroup == listItem) {
            // Scope strictly to components whose path *root* is `list_item.`. The substring
            // also occurs inside `music_list_item.` used by home feed track cards, whose
            // buffer happens to embed `yt_outline_download` - filtering those would blank
            // the home page.
            if (!path.startsWith(LIST_ITEM_ROOT_PREFIX)) {
                return false;
            }
            return bufferGroupList.check(buffer).isFiltered();
        }

        return true;
    }
}
