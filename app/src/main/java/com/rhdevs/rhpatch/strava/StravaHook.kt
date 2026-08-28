package com.rhdevs.rhpatch.strava

import com.rhdevs.rhpatch.strava.subscription.UnlockSubscription
import com.rhdevs.rhpatch.strava.upselling.DisableSubscriptionSuggestions

val StravaPatches = arrayOf(
    UnlockSubscription,
    DisableSubscriptionSuggestions
)
