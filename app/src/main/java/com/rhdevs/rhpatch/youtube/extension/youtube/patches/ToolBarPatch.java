/*
 * Copyright 2026 Morphe.
 * https://github.com/Morphecom/rhdevs/rhpatch/youtube-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package com.rhdevs.rhpatch.youtube.extension.youtube.patches;

import android.view.View;
import android.widget.ImageView;

import com.rhdevs.rhpatch.youtube.extension.shared.Logger;

@SuppressWarnings("unused")
public class ToolBarPatch {

    /**
     * Injection point.
     */
    public static void hookToolBar(Enum<?> iconEnum, ImageView imageView) {
        if (iconEnum != null && imageView.getParent() instanceof View parentView) {
            String enumName = iconEnum.name();
            Logger.printDebug(() -> "enum: " + enumName);
            hookToolBar(enumName, parentView, imageView);
        }
    }

    private static void hookToolBar(String enumString, View parentView, ImageView imageView) {
        // Code added during patching.
    }
}