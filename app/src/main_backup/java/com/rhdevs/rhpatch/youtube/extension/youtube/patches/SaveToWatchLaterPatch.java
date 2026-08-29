/*
 * Copyright 2026 Morphe.
 * https://github.com/Morphecom/rhdevs/rhpatch/youtube-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package com.rhdevs.rhpatch.youtube.extension.youtube.patches;

import static com.rhdevs.rhpatch.youtube.extension.shared.StringRef.str;

import com.rhdevs.rhpatch.youtube.extension.shared.Logger;
import com.rhdevs.rhpatch.youtube.extension.shared.Utils;
import com.rhdevs.rhpatch.youtube.extension.shared.spoof.SpoofVideoStreamsPatch;
import com.rhdevs.rhpatch.youtube.extension.shared.spoof.requests.PlayerRoutes;
import com.rhdevs.rhpatch.youtube.extension.shared.spoof.requests.StreamOrDetailsDataRequest;

@SuppressWarnings("unused")
public final class SaveToWatchLaterPatch {

    /**
     * If the player is not active, the layout may break.
     * Use it only when it is guaranteed to be used in situations where the player is active.
     */
    private static volatile StreamOrDetailsDataRequest saveVideoRequest;

    public static void saveVideo() {
        try {
            // Prevent a new request until the previous (if exists) is not done
            if (saveVideoRequest != null && !saveVideoRequest.fetchIsDone()) {
                return;
            }
            String videoId = VideoInformation.getVideoId();
            saveVideoRequest = SpoofVideoStreamsPatch.fetchDetails(
                    PlayerRoutes.SEND_SAVE_VIDEO_TO_WATCH_LATER,
                    videoId
            );

            Utils.runOnBackgroundThread(() -> {
                if (saveVideoRequest.getStreamDetails() instanceof String saveToWatchLaterResponse
                        && !saveToWatchLaterResponse.isEmpty()) {
                    Logger.printDebug(() -> "watch later response: " + saveToWatchLaterResponse);

                    if (saveToWatchLaterResponse.contains("STATUS_SUCCEEDED")) {
                        Utils.showToastShort(str(
                                saveToWatchLaterResponse.contains("\"playlistEditResults\"")
                                        ? "morphe_save_to_watch_later_success_toast"
                                        : "morphe_save_to_watch_later_already_exists_toast"));
                    }
                } else {
                    Logger.printDebug(() -> "Could not save video, stream details are null: " + videoId);
                }
            });
        } catch (Exception ex) {
            Logger.printDebug(() -> "Could not fetch video details", ex);
            Utils.showToastShort(str("morphe_save_to_watch_later_error_toast"));
        }
    }
}
