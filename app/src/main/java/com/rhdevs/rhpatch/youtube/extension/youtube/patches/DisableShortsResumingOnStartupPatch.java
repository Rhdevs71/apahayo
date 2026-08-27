package com.rhdevs.rhpatch.youtube.extension.youtube.patches;

import com.rhdevs.rhpatch.youtube.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class DisableShortsResumingOnStartupPatch {

    /**
     * Injection point.
     */
    public static boolean disableShortsResumingOnStartup() {
        return Settings.DISABLE_SHORTS_RESUMING_ON_STARTUP.get();
    }

    /**
     * Injection point.
     */
    public static boolean disableShortsResumingOnStartup(boolean original) {
        return original && !Settings.DISABLE_SHORTS_RESUMING_ON_STARTUP.get();
    }
}