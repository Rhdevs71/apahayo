package com.rhdevs.rhpatch.youtube.youtube.misc.privacy

import com.rhdevs.rhpatch.youtube.shared.misc.privacy.SanitizeSharingLinks
import com.rhdevs.rhpatch.youtube.youtube.misc.settings.PreferenceScreen
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
