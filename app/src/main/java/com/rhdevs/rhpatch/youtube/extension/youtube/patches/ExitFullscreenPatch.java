package com.rhdevs.rhpatch.youtube.extension.youtube.patches;

import com.rhdevs.rhpatch.youtube.extension.shared.Logger;
import com.rhdevs.rhpatch.youtube.extension.shared.Utils;
import com.rhdevs.rhpatch.youtube.extension.youtube.settings.Settings;
import com.rhdevs.rhpatch.youtube.extension.youtube.shared.PlayerType;

@SuppressWarnings("unused")
public class ExitFullscreenPatch {

    public enum FullscreenMode {
        DISABLED,
        PORTRAIT,
        LANDSCAPE,
        PORTRAIT_LANDSCAPE,
    }

    /**
     * Injection point.
     */
    public static void endOfVideoReached(Enum<?> status) {
        try {
            if (status == null || !"ENDED".equals(status.name())) {
                return;
            }
            FullscreenMode mode = Settings.EXIT_FULLSCREEN.get();
            if (mode == FullscreenMode.DISABLED) {
                return;
            }

            if (PlayerType.getCurrent() == PlayerType.WATCH_WHILE_FULLSCREEN) {
                if (mode != FullscreenMode.PORTRAIT_LANDSCAPE) {
                    if (Utils.isLandscapeOrientation()) {
                        if (mode == FullscreenMode.PORTRAIT) {
                            return;
                        }
                    } else if (mode == FullscreenMode.LANDSCAPE) {
                        return;
                    }
                }

                OpenVideosFullscreenHookPatch.exitFullscreenMode();
            }
        } catch (Exception ex) {
            Logger.printException(() -> "endOfVideoReached failure", ex);
        }
    }
}
