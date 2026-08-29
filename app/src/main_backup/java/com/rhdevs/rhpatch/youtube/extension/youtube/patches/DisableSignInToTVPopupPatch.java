package com.rhdevs.rhpatch.youtube.extension.youtube.patches;

import com.rhdevs.rhpatch.youtube.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class DisableSignInToTVPopupPatch {

    /**
     * Injection point.
     */
    public static boolean disableSignInToTVPopup() {
        return Settings.DISABLE_SIGN_IN_TO_TV_POPUP.get();
    }
}
