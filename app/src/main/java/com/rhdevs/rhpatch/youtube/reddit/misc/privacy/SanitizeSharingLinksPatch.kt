package com.rhdevs.rhpatch.youtube.reddit.misc.privacy

import com.rhdevs.rhpatch.youtube.extension.reddit.patches.SanitizeSharingLinksPatch
import com.rhdevs.rhpatch.patch

val SanitizeSharingLinks = patch(
    name = "Sanitize sharing links",
    description = "Adds an option to sanitize sharing links by removing tracking query parameters."
) {
    ShareLinkFormatterFingerprint.hookMethod {
        before {
            if (SanitizeSharingLinksPatch.stripQueryParameters()) {
                it.result = it.args[0]
            }
        }
    }
}
