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

import com.rhdevs.rhpatch.youtube.extension.reddit.patches.HideNavigationButtonsPatch;
import com.rhdevs.rhpatch.youtube.extension.reddit.settings.Settings;
import com.rhdevs.rhpatch.youtube.extension.reddit.settings.preference.BooleanSettingPreference;

@SuppressWarnings("deprecation")
public class NavigationBarPreferenceCategory extends ConditionalPreferenceCategory {
    public NavigationBarPreferenceCategory(Context context, PreferenceScreen screen) {
        super(context, screen);
        setTitle(str("morphe_screen_navigation_bar_title"));
    }

    @Override
    public boolean getSettingsStatus() {
        return HideNavigationButtonsPatch.isPatchIncluded();
    }

    @Override
    public void addPreferences(Context context) {
        addPreference(new BooleanSettingPreference(
                context,
                Settings.HIDE_ANSWERS_BUTTON
        ));
        addPreference(new BooleanSettingPreference(
                context,
                Settings.HIDE_CHAT_BUTTON
        ));
        addPreference(new BooleanSettingPreference(
                context,
                Settings.HIDE_CREATE_BUTTON
        ));
        addPreference(new BooleanSettingPreference(
                context,
                Settings.HIDE_DISCOVER_BUTTON
        ));
        addPreference(new BooleanSettingPreference(
                context,
                Settings.HIDE_GAMES_BUTTON
        ));
    }
}
