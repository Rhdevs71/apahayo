package com.rhdevs.rhpatch.youtube.youtube.layout.thumbnails

import com.rhdevs.rhpatch.youtube.extension.youtube.patches.AlternativeThumbnailsPatch.handleCronetFailure
import com.rhdevs.rhpatch.youtube.extension.youtube.patches.AlternativeThumbnailsPatch.handleCronetSuccess
import com.rhdevs.rhpatch.youtube.extension.youtube.patches.AlternativeThumbnailsPatch.overrideImageURL
import com.rhdevs.rhpatch.youtube.extension.youtube.settings.preference.AlternativeThumbnailsAboutDeArrowPreference
import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.ListPreference
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.NonInteractivePreference
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.SwitchPreference
import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.TextPreference
import com.rhdevs.rhpatch.youtube.youtube.misc.imageurlhook.addImageUrlErrorCallbackHook
import com.rhdevs.rhpatch.youtube.youtube.misc.imageurlhook.addImageUrlHook
import com.rhdevs.rhpatch.youtube.youtube.misc.imageurlhook.addImageUrlSuccessCallbackHook
import com.rhdevs.rhpatch.youtube.youtube.misc.imageurlhook.cronetImageUrlHookPatch
import com.rhdevs.rhpatch.youtube.youtube.misc.settings.PreferenceScreen

val AlternativeThumbnailsPatch = patch(
    name = "Alternative thumbnails",
    description = "Adds options to replace video thumbnails using the DeArrow API or image captures from the video.",
) {
    dependsOn(
        cronetImageUrlHookPatch,
    )

    val entries = "morphe_alt_thumbnail_options_entries"
    val values = "morphe_alt_thumbnail_options_entry_values"
    PreferenceScreen.ALTERNATIVE_THUMBNAILS.addPreferences(
        ListPreference(
            key = "morphe_alt_thumbnail_home",
            entriesKey = entries,
            entryValuesKey = values
        ),
        ListPreference(
            key = "morphe_alt_thumbnail_subscription",
            entriesKey = entries,
            entryValuesKey = values
        ),
        ListPreference(
            key = "morphe_alt_thumbnail_library",
            entriesKey = entries,
            entryValuesKey = values
        ),
        ListPreference(
            key = "morphe_alt_thumbnail_player",
            entriesKey = entries,
            entryValuesKey = values
        ),
        ListPreference(
            key = "morphe_alt_thumbnail_search",
            entriesKey = entries,
            entryValuesKey = values
        ),
        NonInteractivePreference(
            "morphe_alt_thumbnail_dearrow_about",
            // Custom about preference with link to the DeArrow website.
            tag = AlternativeThumbnailsAboutDeArrowPreference::class.java,
            selectable = true,
        ),
        SwitchPreference("morphe_alt_thumbnail_dearrow_connection_toast", summary = true),
        TextPreference("morphe_alt_thumbnail_dearrow_api_url"),
        NonInteractivePreference("morphe_alt_thumbnail_stills_about"),
        SwitchPreference("morphe_alt_thumbnail_stills_fast", summary = true),
        ListPreference("morphe_alt_thumbnail_stills_time"),
    )

    addImageUrlHook(::overrideImageURL)
    addImageUrlSuccessCallbackHook(::handleCronetSuccess)
    addImageUrlErrorCallbackHook(::handleCronetFailure)
}
