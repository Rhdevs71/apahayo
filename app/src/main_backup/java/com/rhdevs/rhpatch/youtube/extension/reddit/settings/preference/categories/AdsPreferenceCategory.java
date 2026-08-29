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

import com.rhdevs.rhpatch.youtube.extension.reddit.patches.HideAdsPatch;
import com.rhdevs.rhpatch.youtube.extension.reddit.settings.Settings;
import com.rhdevs.rhpatch.youtube.extension.reddit.settings.preference.BooleanSettingPreference;

@SuppressWarnings("deprecation")
public class AdsPreferenceCategory extends ConditionalPreferenceCategory {
    public AdsPreferenceCategory(Context context, PreferenceScreen screen) {
        super(context, screen);
        setTitle(str("morphe_screen_ads_title"));
    }

    @Override
    public boolean getSettingsStatus() {
        return HideAdsPatch.isPatchIncluded();
    }

    @Override
    public void addPreferences(Context context) {
        addPreference(new BooleanSettingPreference(
                context,
                Settings.HIDE_COMMENT_ADS
        ));
        addPreference(new BooleanSettingPreference(
                context,
                Settings.HIDE_POST_ADS
        ));
    }
}
