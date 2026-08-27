/*
 * Copyright 2026 Morphe.
 * https://github.com/Morphecom/rhdevs/rhpatch/youtube-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package com.rhdevs.rhpatch.youtube.extension.youtube.patches;

import android.view.View;
import android.view.ViewGroup;

import com.rhdevs.rhpatch.youtube.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class QuickActionsMarginPatch {

    /**
     * Injection point.
     * Sets the top margin of the quick actions container view in dp units.
     */
    public static void setQuickActionsMargin(View view) {
        final int marginDp = Settings.QUICK_ACTIONS_TOP_MARGIN.get();
        if (marginDp == 0) {
            return;
        }

        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams params) {
            params.topMargin = Math.round(marginDp * view.getResources().getDisplayMetrics().density);
            view.requestLayout();
        }
    }
}
