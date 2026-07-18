package com.rhdevs.rhpatch.morphe.shared.ad

import app.morphe.extension.shared.patches.HideFullscreenAdsPatch
import com.rhdevs.rhpatch.morphe.shared.misc.settings.preference.BasePreferenceScreen
import com.rhdevs.rhpatch.morphe.shared.misc.settings.preference.SwitchPreference
import com.rhdevs.rhpatch.patch

fun HideFullscreenAds(preferenceScreen: BasePreferenceScreen.Screen) = patch(
    description = "Adds an option to hide fullscreen premium popup ads."
) {
    preferenceScreen.addPreferences(
        SwitchPreference("morphe_hide_fullscreen_ads")
    )


    // Hide fullscreen ad
    LithoDialogBuilderFingerprint.hookMethod {
        var dialogField = ::LithoDialogField.field
        after {
            val buffer = it.args[0] as ByteArray?
            val dialog = dialogField.get(it.thisObject)
            HideFullscreenAdsPatch.closeFullscreenAd(dialog, buffer)
        }
    }
}
