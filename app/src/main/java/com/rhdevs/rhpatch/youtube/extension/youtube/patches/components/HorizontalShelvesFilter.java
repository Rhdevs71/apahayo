/*
 * Copyright 2026 Morphe.
 * https://github.com/Morphecom/rhdevs/rhpatch/youtube-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package com.rhdevs.rhpatch.youtube.extension.youtube.patches.components;

import static com.rhdevs.rhpatch.youtube.extension.youtube.patches.LayoutReloadObserverPatch.isActionBarVisible;

import com.rhdevs.rhpatch.youtube.extension.shared.patches.components.BufferAsciiStrings;
import com.rhdevs.rhpatch.youtube.extension.shared.patches.components.ByteArrayFilterGroup;
import com.rhdevs.rhpatch.youtube.extension.shared.patches.components.ByteArrayFilterGroupList;
import com.rhdevs.rhpatch.youtube.extension.shared.patches.components.ContextInterface;
import com.rhdevs.rhpatch.youtube.extension.shared.patches.components.Filter;
import com.rhdevs.rhpatch.youtube.extension.shared.patches.components.StringFilterGroup;
import com.rhdevs.rhpatch.youtube.extension.youtube.settings.Settings;
import com.rhdevs.rhpatch.youtube.extension.youtube.shared.EngagementPanel;
import com.rhdevs.rhpatch.youtube.extension.youtube.shared.NavigationBar;
import com.rhdevs.rhpatch.youtube.extension.youtube.shared.NavigationBar.NavigationButton;
import com.rhdevs.rhpatch.youtube.extension.youtube.shared.PlayerType;
import com.rhdevs.rhpatch.youtube.extension.youtube.shared.ShortsPlayerState;

@SuppressWarnings("unused")
public final class HorizontalShelvesFilter extends Filter {
    private final ByteArrayFilterGroupList descriptionBuffers = new ByteArrayFilterGroupList();
    private final ByteArrayFilterGroupList generalBuffers = new ByteArrayFilterGroupList();

    public HorizontalShelvesFilter() {
        StringFilterGroup horizontalShelves = new StringFilterGroup(null, "horizontal_shelf.e");
        addPathCallbacks(horizontalShelves);

        descriptionBuffers.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_ATTRIBUTES_SECTION,
                        // May no longer work on v20.31+, even though the component is still there.
                        "cell_video_attribute"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_FEATURED_PLACES_SECTION,
                        "yt_fill_experimental_star",
                        "yt_fill_star"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_GAMING_SECTION,
                        "yt_outline_experimental_gaming",
                        "yt_outline_gaming"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_MUSIC_SECTION,
                        "yt_outline_experimental_audio",
                        "yt_outline_audio"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_QUIZZES_SECTION,
                        "post_base_wrapper_slim"
                )
        );

        generalBuffers.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_CREATOR_STORE_SHELF,
                        "shopping_item_card_list"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYABLES,
                        "FEmini_app_destination"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_TICKET_SHELF,
                        "ticket_item.e"
                )
        );
    }

    private boolean isPlayerOrDescription() {
        return EngagementPanel.isDescription()
                || PlayerType.getCurrent().isMaximizedOrFullscreen()
                || isActionBarVisible.get()
                || ShortsPlayerState.isOpen();
    }

    private boolean hideShelves(ContextInterface contextInterface) {
        if (!Settings.HIDE_HORIZONTAL_SHELVES.get() || isPlayerOrDescription()) {
            return false;
        }
        return contextInterface.isHomeFeedOrRelatedVideo()
                || NavigationBar.isSearchBarActive()
                || NavigationBar.isBackButtonVisible()
                || NavigationButton.getSelectedNavigationButton() != NavigationButton.LIBRARY;
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
        if (contentIndex != 0) {
            return false;
        }
        if (generalBuffers.check(buffer).isFiltered()) {
            return true;
        }
        if (descriptionBuffers.check(buffer).isFiltered()) {
            return isPlayerOrDescription();
        }
        return hideShelves(contextInterface);
    }
}
