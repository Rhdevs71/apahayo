package com.rhdevs.rhpatch.morphe.music.misc.debugging

import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.morphe.music.misc.settings.PreferenceScreen
import com.rhdevs.rhpatch.morphe.shared.misc.debugging.EnableDebugging

val EnableDebugging = patch(
    name = "Enable debugging",
    description = "Adds options for debugging and exporting ReVanced logs to the clipboard.",
) {
    EnableDebugging(
        preferenceScreen = PreferenceScreen.MISC
    )
}
