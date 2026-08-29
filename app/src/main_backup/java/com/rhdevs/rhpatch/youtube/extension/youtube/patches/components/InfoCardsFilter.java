package com.rhdevs.rhpatch.youtube.extension.youtube.patches.components;

import com.rhdevs.rhpatch.youtube.extension.shared.patches.components.Filter;
import com.rhdevs.rhpatch.youtube.extension.shared.patches.components.StringFilterGroup;

import com.rhdevs.rhpatch.youtube.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class InfoCardsFilter extends Filter {

    public InfoCardsFilter() {
        addIdentifierCallbacks(
                new StringFilterGroup(
                        Settings.HIDE_INFO_CARDS,
                        "info_card_teaser_overlay.e"
                )
        );
    }
}
