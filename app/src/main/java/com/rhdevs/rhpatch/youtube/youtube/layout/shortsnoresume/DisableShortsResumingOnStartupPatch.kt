package com.rhdevs.rhpatch.youtube.youtube.layout.shortsnoresume

import com.rhdevs.rhpatch.youtube.extension.youtube.patches.DisableShortsResumingOnStartupPatch
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.SwitchPreference
import com.rhdevs.rhpatch.youtube.youtube.misc.settings.PreferenceScreen
import com.rhdevs.rhpatch.patch

val DisableShortsResumingOnStartup = patch(
    name = "Disable Shorts resuming on startup",
    description = "Adds an option to disable Shorts from resuming on app startup when Shorts were last being watched.",
) {
    PreferenceScreen.SHORTS.addPreferences(
        SwitchPreference("morphe_disable_shorts_resuming_on_startup"),
    )

    // TODO UserWasInShortsEvaluateFingerprint (21.03+) â€” METHOD_MID
    // TODO UserWasInShortsListenerFingerprint (20.03-21.02) â€” METHOD_MID
    // TODO UserWasInShortsLegacyFingerprint (<20.03) â€” METHOD_MID

    UserWasInShortsConfigFingerprint.hookMethod {
        before {
            if (DisableShortsResumingOnStartupPatch.disableShortsResumingOnStartup()) {
                it.result = false
            }
        }
    }
}
