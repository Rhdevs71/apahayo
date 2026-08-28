package com.rhdevs.rhpatch.youtube.music.misc.privacy

import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.youtube.music.misc.settings.PreferenceScreen
import com.rhdevs.rhpatch.youtube.shared.misc.privacy.SanitizeSharingLinks

val SanitizeSharingLinks = patch(
    name = "Sanitize sharing links",
    description = "Removes the tracking query parameters from shared links."
) {
    SanitizeSharingLinks(
        preferenceScreen = PreferenceScreen.MISC,
        replaceMusicLinksWithYouTube = true
    )
}
