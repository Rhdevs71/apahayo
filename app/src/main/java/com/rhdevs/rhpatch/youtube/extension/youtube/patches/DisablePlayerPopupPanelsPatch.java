package com.rhdevs.rhpatch.youtube.extension.youtube.patches;

import com.rhdevs.rhpatch.youtube.extension.shared.Logger;
import com.rhdevs.rhpatch.youtube.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class DisablePlayerPopupPanelsPatch {

    /**
     * Injection point.
     */
    public static boolean disablePlayerPopupPanels() {
        if (Settings.DISABLE_PLAYER_POPUP_PANELS.get()) {
            Logger.printDebug(() -> "disablePlayerPopupPanels: Popup panels blocked!");
            return true;
        }
        return false;
    }
}
