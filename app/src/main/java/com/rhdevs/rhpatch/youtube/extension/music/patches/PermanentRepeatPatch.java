package com.rhdevs.rhpatch.youtube.extension.music.patches;

import com.rhdevs.rhpatch.youtube.extension.music.settings.Settings;

@SuppressWarnings("unused")
public class PermanentRepeatPatch {

    /**
     * Injection point
     */
    public static boolean permanentRepeat() {
        return Settings.PERMANENT_REPEAT.get();
    }
}
