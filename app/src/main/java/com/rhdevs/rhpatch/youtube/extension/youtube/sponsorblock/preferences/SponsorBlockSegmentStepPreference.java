/*
 * Copyright 2026 Morphe.
 * https://github.com/Morphecom/rhdevs/rhpatch/youtube-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package com.rhdevs.rhpatch.youtube.extension.youtube.sponsorblock.preferences;

import static com.rhdevs.rhpatch.youtube.extension.shared.StringRef.str;

import android.content.Context;
import android.util.AttributeSet;

import com.rhdevs.rhpatch.youtube.extension.shared.Logger;
import com.rhdevs.rhpatch.youtube.extension.shared.Utils;
import com.rhdevs.rhpatch.youtube.extension.shared.settings.preference.ResettableEditTextPreference;

/**
 * Numeric input for the "create new segment" time-adjustment step. Rejects zero and noninteger
 * values with a toast, matching the legacy SponsorBlock preferences behavior.
 */
@SuppressWarnings({"unused", "deprecation"})
public class SponsorBlockSegmentStepPreference extends ResettableEditTextPreference {

    public SponsorBlockSegmentStepPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        installValidator();
    }

    public SponsorBlockSegmentStepPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        installValidator();
    }

    public SponsorBlockSegmentStepPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        installValidator();
    }

    public SponsorBlockSegmentStepPreference(Context context) {
        super(context);
        installValidator();
    }

    private void installValidator() {
        setOnPreferenceChangeListener((p, newValue) -> {
            try {
                if (Integer.parseInt(newValue.toString()) != 0) {
                    return true;
                }
            } catch (NumberFormatException ex) {
                Logger.printInfo(() -> "Invalid new segment step", ex);
            }
            Utils.showToastLong(str("morphe_sb_create_new_segment_step_invalid"));
            return false;
        });
    }
}
