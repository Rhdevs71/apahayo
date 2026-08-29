/*
 * Copyright 2026 Morphe.
 * https://github.com/Morphecom/rhdevs/rhpatch/youtube-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package com.rhdevs.rhpatch.youtube.extension.youtube.videoplayer;

import static com.rhdevs.rhpatch.youtube.extension.youtube.patches.LegacyPlayerControlsPatch.RESTORE_OLD_PLAYER_BUTTONS;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import com.rhdevs.rhpatch.youtube.extension.shared.Logger;
import com.rhdevs.rhpatch.youtube.extension.shared.Utils;
import com.rhdevs.rhpatch.youtube.extension.youtube.patches.voiceovertranslation.VoiceOverTranslationPatch;
import com.rhdevs.rhpatch.youtube.extension.youtube.patches.voiceovertranslation.VotBottomSheet;
import com.rhdevs.rhpatch.youtube.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class VoiceOverTranslationButton {

    @Nullable
    private static LegacyPlayerControlButton legacy;

    @Nullable
    private static WeakReference<ImageView> overlayButtonRef;

    /** Injection point. */
    public static void initializeButton(View controlsView) {
        try {
            if (RESTORE_OLD_PLAYER_BUTTONS || !Settings.VOT_ENABLED.get()) return;

            VoiceOverTranslationPatch.setOnTranslationStateChangeCallback(
                    VoiceOverTranslationButton::refreshActivatedState);

            ImageView button = PlayerOverlayButton.addButton(
                    controlsView,
                    "morphe_yt_vot_bold",
                    view -> {
                        VoiceOverTranslationPatch.toggleTranslation();
                        refreshActivatedState();
                    },
                    view -> {
                        VotBottomSheet.show(view.getContext());
                        return true;
                    });
            overlayButtonRef = button != null ? new WeakReference<>(button) : null;
            refreshActivatedState();
        } catch (Exception ex) {
            Logger.printException(() -> "initializeButton failure", ex);
        }
    }

    /** Injection point. */
    public static void initializeLegacyButton(View controlsView) {
        try {
            if (!RESTORE_OLD_PLAYER_BUTTONS) return;

            VoiceOverTranslationPatch.setOnTranslationStateChangeCallback(
                    VoiceOverTranslationButton::refreshActivatedState);

            legacy = new LegacyPlayerControlButton(
                    controlsView,
                    "morphe_vot_button",
                    null,
                    "morphe_yt_vot",
                    Settings.VOT_ENABLED::get,
                    view -> {
                        VoiceOverTranslationPatch.toggleTranslation();
                        refreshActivatedState();
                    },
                    view -> {
                        VotBottomSheet.show(view.getContext());
                        return true;
                    });
            refreshActivatedState();
        } catch (Exception ex) {
            Logger.printException(() -> "initializeLegacyButton failure", ex);
        }
    }

    private static void refreshActivatedState() {
        Utils.verifyOnMainThread();
        try {
            final int alpha = VoiceOverTranslationPatch.isSessionEnabled() ? 255 : 128;
            WeakReference<ImageView> ref = overlayButtonRef;
            ImageView overlay = ref != null ? ref.get() : null;
            if (overlay != null) {
                overlay.setImageAlpha(alpha);
            }
            LegacyPlayerControlButton leg = legacy;
            if (leg != null) {
                leg.setImageAlpha(alpha);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "refreshActivatedState failure", ex);
        }
    }

    /** Injection point. */
    public static void setVisibilityNegatedImmediate() {
        if (legacy != null) legacy.setVisibilityNegatedImmediate();
    }

    /** Injection point. */
    public static void setVisibilityImmediate(boolean visible) {
        if (legacy != null) legacy.setVisibilityImmediate(visible);
    }

    /** Injection point. */
    public static void setVisibility(boolean visible, boolean animated) {
        if (legacy != null) legacy.setVisibility(visible, animated);
    }
}
