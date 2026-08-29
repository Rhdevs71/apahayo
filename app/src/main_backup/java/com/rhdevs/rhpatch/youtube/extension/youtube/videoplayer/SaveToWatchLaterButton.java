/*
 * Copyright 2026 Morphe.
 * https://github.com/Morphecom/rhdevs/rhpatch/youtube-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package com.rhdevs.rhpatch.youtube.extension.youtube.videoplayer;

import static com.rhdevs.rhpatch.youtube.extension.youtube.patches.LegacyPlayerControlsPatch.RESTORE_OLD_PLAYER_BUTTONS;

import android.view.View;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.function.Function;

import com.rhdevs.rhpatch.youtube.extension.shared.Logger;
import com.rhdevs.rhpatch.youtube.extension.shared.ResourceType;
import com.rhdevs.rhpatch.youtube.extension.shared.ResourceUtils;
import com.rhdevs.rhpatch.youtube.extension.youtube.patches.SaveToWatchLaterPatch;
import com.rhdevs.rhpatch.youtube.extension.youtube.patches.VideoInformation;
import com.rhdevs.rhpatch.youtube.extension.youtube.patches.utils.PlaylistPatch;
import com.rhdevs.rhpatch.youtube.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class SaveToWatchLaterButton {

    static {
        if (Settings.SAVE_TO_WATCH_LATER_BUTTON.get()) {
            LegacyPlayerControlButton.incrementUpperButtonCount();
        }
    }

    @Nullable
    private static LegacyPlayerControlButton instance;

    /**
     * injection point.
     */
    public static void initializeLegacyButton(View controlsView) {
        try {
            // Start syncing queue playlist items in the background so that by the time
            // the user opens the queue menu, lastVideoIds is already populated.
            PlaylistPatch.syncIfNeeded();

            final boolean swapSaveAndQueue = Settings.SWAP_SAVE_AND_QUEUE_ACTIONS.get();
            //noinspection ExtractMethodRecommender
            WeakReference<View> controlsRef = new WeakReference<>(controlsView);

            Function<Boolean, Void> clickAction = openQueue -> {
                if (openQueue) {
                    View controls = controlsRef.get();
                    if (controls == null) {
                        Logger.printException(() -> "Context is null");
                        return null;
                    }
                    PlaylistPatch.prepareDialogBuilder(controls.getContext(), VideoInformation.getVideoId());
                } else {
                    SaveToWatchLaterPatch.saveVideo();
                }
                return null;
            };

            instance = new LegacyPlayerControlButton(
                    controlsView,
                    "morphe_save_to_watch_later_button",
                    null,
                    swapSaveAndQueue ? null : "morphe_save_to_watch_later_button",
                    Settings.SAVE_TO_WATCH_LATER_BUTTON::get,
                    v -> clickAction.apply(swapSaveAndQueue),
                    v -> {
                        clickAction.apply(!swapSaveAndQueue);
                        return true;
                    }
            );

            if (swapSaveAndQueue) {
                instance.setIcon(ResourceUtils.getIdentifier(ResourceType.DRAWABLE,
                        RESTORE_OLD_PLAYER_BUTTONS
                                ? "yt_outline_list_add_black_24"
                                : "yt_outline_experimental_playlist_add_vd_theme_24"
                ));
            }
        } catch (Exception ex) {
            Logger.printException(() -> "initialize failure", ex);
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
}
