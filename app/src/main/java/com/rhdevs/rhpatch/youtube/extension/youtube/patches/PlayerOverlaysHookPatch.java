package com.rhdevs.rhpatch.youtube.extension.youtube.patches;

import android.view.ViewGroup;

import com.rhdevs.rhpatch.youtube.extension.youtube.shared.PlayerOverlays;

@SuppressWarnings("unused")
public class PlayerOverlaysHookPatch {
    /**
     * Injection point.
     */
    public static void playerOverlayInflated(ViewGroup group) {
        PlayerOverlays.attach(group);
    }
}