package com.rhdevs.rhpatch.youtube.youtube.layout.thumbnails

import com.rhdevs.rhpatch.youtube.extension.youtube.patches.BypassImageRegionRestrictionsPatch.overrideImageURL
import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.SwitchPreference
import com.rhdevs.rhpatch.youtube.youtube.misc.imageurlhook.addImageUrlHook
import com.rhdevs.rhpatch.youtube.youtube.misc.imageurlhook.cronetImageUrlHookPatch
import com.rhdevs.rhpatch.youtube.youtube.misc.settings.PreferenceScreen

val BypassImageRegionRestrictionsPatch = patch(
    name = "Bypass image region restrictions",
    description = "Adds an option to use a different host for user avatar and channel images " +
            "and can fix missing images that are blocked in some countries.",
) {
    dependsOn(
        cronetImageUrlHookPatch,
    )

    PreferenceScreen.MISC.addPreferences(
        SwitchPreference("morphe_bypass_image_region_restrictions", summary = true),
    )

    // A priority hook is not needed, as the image urls of interest are not modified
    // by AlternativeThumbnails or any other patch in this repo.
    addImageUrlHook(::overrideImageURL)
}
