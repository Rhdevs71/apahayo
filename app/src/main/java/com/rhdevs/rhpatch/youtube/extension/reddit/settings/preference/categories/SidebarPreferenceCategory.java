/*
 * Copyright 2026 Morphe.
 * https://github.com/Morphecom/rhdevs/rhpatch/youtube-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package com.rhdevs.rhpatch.youtube.extension.reddit.settings.preference.categories;

import static com.rhdevs.rhpatch.youtube.extension.reddit.patches.VersionCheckPatch.is_2025_52_or_greater;
import static com.rhdevs.rhpatch.youtube.extension.shared.StringRef.str;

import android.content.Context;
import android.preference.PreferenceScreen;

import com.rhdevs.rhpatch.youtube.extension.reddit.patches.HideSidebarComponentsPatch;
import com.rhdevs.rhpatch.youtube.extension.reddit.settings.Settings;
import com.rhdevs.rhpatch.youtube.extension.reddit.settings.preference.BooleanSettingPreference;

@SuppressWarnings("deprecation")
public class SidebarPreferenceCategory extends ConditionalPreferenceCategory {
    public SidebarPreferenceCategory(Context context, PreferenceScreen screen) {
        super(context, screen);
        setTitle(str("morphe_screen_sidebar_title"));
    }

    @Override
    public boolean getSettingsStatus() {
        return HideSidebarComponentsPatch.isPatchIncluded();
    }

    @Override
    public void addPreferences(Context context) {
        if (HideSidebarComponentsPatch.isPatchIncluded()) {
            addPreference(new BooleanSettingPreference(
                    context,
                    Settings.HIDE_RECENTLY_VISITED_SHELF
            ));
            addPreference(new BooleanSettingPreference(
                    context,
                    Settings.HIDE_GAMES_ON_REDDIT_SHELF
            ));
            addPreference(new BooleanSettingPreference(
                    context,
                    Settings.HIDE_REDDIT_PRO_SHELF
            ));

            if (is_2025_52_or_greater) {
                addPreference(new BooleanSettingPreference(
                        context,
                        Settings.HIDE_ABOUT_SHELF
                ));
                addPreference(new BooleanSettingPreference(
                        context,
                        Settings.HIDE_RESOURCES_SHELF
                ));
            }
        }

    }
}
