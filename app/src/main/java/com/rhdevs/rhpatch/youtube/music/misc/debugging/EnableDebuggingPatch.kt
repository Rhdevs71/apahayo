package com.rhdevs.rhpatch.youtube.music.misc.debugging

import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.youtube.music.misc.settings.PreferenceScreen
import com.rhdevs.rhpatch.youtube.shared.misc.debugging.EnableDebugging

val EnableDebugging = patch(
    name = "Enable debugging",
    description = "Adds options for debugging and exporting ReVanced logs to the clipboard.",
) {
    EnableDebugging(
        preferenceScreen = PreferenceScreen.MISC
    )
}
