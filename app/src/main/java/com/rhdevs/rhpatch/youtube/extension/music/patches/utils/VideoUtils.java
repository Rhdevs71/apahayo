/*
 * Copyright 2026 Morphe.
 * https://github.com/Morphecom/rhdevs/rhpatch/youtube-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package com.rhdevs.rhpatch.youtube.extension.music.patches.utils;

import android.annotation.SuppressLint;

import java.time.Duration;

import com.rhdevs.rhpatch.youtube.extension.shared.Utils;

@SuppressWarnings("unused")
public class VideoUtils {

    public static String getFormattedTimeStamp(long videoTime) {
        return "'" + videoTime + "' (" + getTimeStamp(videoTime) + ")\n";
    }

    @SuppressLint("DefaultLocale")
    public static String getTimeStamp(long time) {
        final long hours;
        final long minutes;
        final long seconds;

        if (Utils.isSDKAbove(26)) {
            final Duration duration = Duration.ofMillis(time);
            hours = duration.toHours();
            minutes = duration.toMinutes() % 60;
            seconds = duration.getSeconds() % 60;
        } else {
            final long currentVideoTimeInSeconds = time / 1000;
            hours = currentVideoTimeInSeconds / (60 * 60);
            minutes = (currentVideoTimeInSeconds / 60) % 60;
            seconds = currentVideoTimeInSeconds % 60;
        }

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }
}
