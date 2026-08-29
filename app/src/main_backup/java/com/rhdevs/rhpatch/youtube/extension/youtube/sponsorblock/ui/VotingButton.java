package com.rhdevs.rhpatch.youtube.extension.youtube.sponsorblock.ui;

import android.view.View;

import androidx.annotation.Nullable;

import com.rhdevs.rhpatch.youtube.extension.shared.Logger;
import com.rhdevs.rhpatch.youtube.extension.youtube.settings.Settings;
import com.rhdevs.rhpatch.youtube.extension.shared.sponsorblock.SegmentPlaybackController;
import com.rhdevs.rhpatch.youtube.extension.youtube.sponsorblock.SponsorBlockUtils;
import com.rhdevs.rhpatch.youtube.extension.youtube.videoplayer.LegacyPlayerControlButton;

@SuppressWarnings("unused")
public class VotingButton {

    static {
        if (Settings.SB_ENABLED.get() && Settings.SB_VOTING_BUTTON.get()) {
            LegacyPlayerControlButton.incrementUpperButtonCount();
        }
    }

    @Nullable
    private static LegacyPlayerControlButton instance;

    public static void hideControls() {
        if (instance != null) instance.hide();
    }

    /**
     * injection point.
     */
    public static void initializeLegacyButton(View controlsView) {
        try {
            instance = new LegacyPlayerControlButton(
                    controlsView,
                    "morphe_sb_voting_button",
                    null,
                    "morphe_sb_voting",
                    VotingButton::isButtonEnabled,
                    v -> SponsorBlockUtils.onVotingClicked(v.getContext()),
                    null
            );
        } catch (Exception ex) {
            Logger.printException(() -> "initializeButton failure", ex);
        }
    }

    /**
     * injection point.
     */
    public static void setVisibilityNegatedImmediate() {
        if (instance != null) instance.setVisibilityNegatedImmediate();
    }

    /**
     * injection point.
     */
    public static void setVisibilityImmediate(boolean visible) {
        if (instance != null) instance.setVisibilityImmediate(visible);
    }

    /**
     * injection point.
     */
    public static void setVisibility(boolean visible, boolean animated) {
        if (instance != null) instance.setVisibility(visible, animated);
    }

    private static boolean isButtonEnabled() {
        return Settings.SB_ENABLED.get() && Settings.SB_VOTING_BUTTON.get()
                && SegmentPlaybackController.videoHasSegments()
                && !SegmentPlaybackController.isAdProgressTextVisible();
    }
}
