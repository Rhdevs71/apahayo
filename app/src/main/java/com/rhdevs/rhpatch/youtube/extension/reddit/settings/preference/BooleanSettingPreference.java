/*
 * Copyright 2026 Morphe.
 * https://github.com/Morphecom/rhdevs/rhpatch/youtube-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package com.rhdevs.rhpatch.youtube.extension.reddit.settings.preference;

import com.rhdevs.rhpatch.youtube.extension.shared.ResourceUtils;
import static com.rhdevs.rhpatch.youtube.extension.shared.StringRef.str;

import android.content.Context;
import android.preference.SwitchPreference;

import com.rhdevs.rhpatch.youtube.extension.shared.settings.BooleanSetting;

@SuppressWarnings("deprecation")
public class BooleanSettingPreference extends SwitchPreference {

    public BooleanSettingPreference(Context context, BooleanSetting setting) {
        super(context);
        this.setTitle(str(setting.key + "_title"));
        String summaryKey = setting.key + "_summary";
        if (ResourceUtils.getStringIdentifier(summaryKey) != 0) {
            this.setSummary(str(summaryKey));
        }
        this.setKey(setting.key);
        this.setChecked(setting.get());
    }
}
