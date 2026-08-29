package com.rhdevs.rhpatch.youtube.youtube.layout.buttons.overlay

import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.BasePreference
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.PreferenceScreenPreference
import com.rhdevs.rhpatch.youtube.youtube.misc.settings.PreferenceScreen
import com.rhdevs.rhpatch.patch

// Use initially null field so an exception is thrown if this patch was not included.
private var playerOverlayPreferences : MutableSet<BasePreference>? = mutableSetOf()

internal fun addPlayerOverlayPreferences(vararg preference: BasePreference) {
    playerOverlayPreferences!!.addAll(preference)
}

val PlayerOverlayButtonsSettings = patch {
    if (playerOverlayPreferences!!.isNotEmpty()) {
        PreferenceScreen.PLAYER.addPreferences(
            PreferenceScreenPreference(
                key = "morphe_overlay_buttons_screen",
                preferences = playerOverlayPreferences!!
            )
        )
    }
}
