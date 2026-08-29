package com.rhdevs.rhpatch.youtube.extension.youtube.patches;

import static com.rhdevs.rhpatch.youtube.extension.shared.StringRef.str;

import android.widget.ImageView;

import com.rhdevs.rhpatch.youtube.extension.shared.Utils;
import com.rhdevs.rhpatch.youtube.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class CustomPlayerOverlayOpacityPatch {

    private static final int PLAYER_OVERLAY_OPACITY_LEVEL;

    static {
        int opacity = Settings.PLAYER_OVERLAY_OPACITY.get();

        if (opacity < 0 || opacity > 100) {
            Utils.showToastLong(str("morphe_player_overlay_opacity_invalid_toast"));
            opacity = Settings.PLAYER_OVERLAY_OPACITY.resetToDefault();
        }

        PLAYER_OVERLAY_OPACITY_LEVEL = (opacity * 255) / 100;
    }

    /**
     * Injection point.
     */
    public static void changeOpacity(ImageView imageView) {
        imageView.setImageAlpha(PLAYER_OVERLAY_OPACITY_LEVEL);
    }
}
