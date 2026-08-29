package com.rhdevs.rhpatch.strava.subscription

import com.rhdevs.rhpatch.patch

val UnlockSubscription = patch(
    name = "Unlock subscription features",
    description = "Unlocks \"Routes\", \"Matched Runs\" and \"Segment Efforts\".",
) {
    ::getSubscribedFingerprint.hookMethod {
        before { param ->
            param.result = true
        }
    }
}
