package com.rhdevs.rhpatch.morphe.youtube.misc.privacy

import com.rhdevs.rhpatch.morphe.shared.misc.privacy.SanitizeSharingLinks
import com.rhdevs.rhpatch.morphe.youtube.misc.settings.PreferenceScreen
import com.rhdevs.rhpatch.patch

val SanitizeSharingLinks = patch(
    name = "Sanitize sharing links",
    description = "Removes the tracking query parameters from shared links."
) {
    SanitizeSharingLinks(
        preferenceScreen = PreferenceScreen.MISC,
        replaceLinksWithShortener = true
    )
}
