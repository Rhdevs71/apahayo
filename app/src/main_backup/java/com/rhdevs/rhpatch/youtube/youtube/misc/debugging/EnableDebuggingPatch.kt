package com.rhdevs.rhpatch.youtube.youtube.misc.debugging

import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.youtube.shared.misc.debugging.EnableDebugging
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.SwitchPreference
import com.rhdevs.rhpatch.youtube.youtube.misc.settings.PreferenceScreen

val EnableDebugging = patch(
    name = "Enable debugging",
    description = "Adds options for debugging and exporting ReVanced logs to the clipboard.",
) {
    EnableDebugging(
        preferenceScreen = PreferenceScreen.MISC,
        additionalDebugPreferences = listOf(SwitchPreference("morphe_debug_protobuffer", summary = true))
    )
}
