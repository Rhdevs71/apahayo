/*
 * Copyright 2026 Morphe.
 * https://github.com/Morphecom/rhdevs/rhpatch/youtube-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package com.rhdevs.rhpatch.youtube.extension.reddit.settings.preference.categories;

import static com.rhdevs.rhpatch.youtube.extension.shared.StringRef.str;

import android.content.Context;
import android.preference.PreferenceScreen;

import com.rhdevs.rhpatch.youtube.extension.reddit.patches.DisableModernHomePatch;
import com.rhdevs.rhpatch.youtube.extension.reddit.patches.DisableScreenshotPopupPatch;
import com.rhdevs.rhpatch.youtube.extension.reddit.patches.HideAskButtonPatch;
import com.rhdevs.rhpatch.youtube.extension.reddit.patches.HideCommunitiesShelf;
import com.rhdevs.rhpatch.youtube.extension.reddit.patches.HideTrendingShelvesPatch;
import com.rhdevs.rhpatch.youtube.extension.reddit.patches.RemoveSubRedditDialogPatch;
import com.rhdevs.rhpatch.youtube.extension.reddit.patches.ShowViewCountPatch;
import com.rhdevs.rhpatch.youtube.extension.reddit.settings.Settings;
import com.rhdevs.rhpatch.youtube.extension.reddit.settings.preference.BooleanSettingPreference;

@SuppressWarnings("deprecation")
public class LayoutPreferenceCategory extends ConditionalPreferenceCategory {
    public LayoutPreferenceCategory(Context context, PreferenceScreen screen) {
        super(context, screen);
        setTitle(str("morphe_screen_layout_title"));
    }

    @Override
    public boolean getSettingsStatus() {
        return DisableModernHomePatch.isPatchIncluded() ||
                DisableScreenshotPopupPatch.isPatchIncluded() ||
                HideAskButtonPatch.isPatchIncluded() ||
                HideCommunitiesShelf.isPatchIncluded() ||
                HideTrendingShelvesPatch.isPatchIncluded() ||
                RemoveSubRedditDialogPatch.isPatchIncluded();
    }

    @Override
    public void addPreferences(Context context) {
        if (DisableModernHomePatch.isPatchIncluded()) {
            addPreference(new BooleanSettingPreference(
                    context,
                    Settings.DISABLE_MODERN_HOME
            ));
        }

        if (DisableScreenshotPopupPatch.isPatchIncluded()) {
            addPreference(new BooleanSettingPreference(
                    context,
                    Settings.DISABLE_SCREENSHOT_POPUP
            ));
        }

        if (HideAskButtonPatch.isPatchIncluded()) {
            addPreference(new BooleanSettingPreference(
                    context,
                    Settings.HIDE_ASK_BUTTON
            ));
        }

        if (HideCommunitiesShelf.isPatchIncluded()) {
            addPreference(new BooleanSettingPreference(
                    context,
                    Settings.HIDE_COMMUNITIES_SHELF
            ));
        }

        if (HideTrendingShelvesPatch.isPatchIncluded()) {
            addPreference(new BooleanSettingPreference(
                    context,
                    Settings.HIDE_TRENDING_SHELVES
            ));
        }

        if (RemoveSubRedditDialogPatch.isPatchIncluded()) {
            addPreference(new BooleanSettingPreference(
                    context,
                    Settings.REMOVE_NSFW_DIALOG
            ));
            addPreference(new BooleanSettingPreference(
                    context,
                    Settings.REMOVE_NOTIFICATION_DIALOG
            ));
        }

        if (ShowViewCountPatch.isPatchIncluded()) {
            addPreference(new BooleanSettingPreference(
                    context,
                    Settings.SHOW_VIEW_COUNT
            ));
        }
    }
}
