package com.rhdevs.rhpatch.hoodles.morphe.alltrails.pro

import com.rhdevs.rhpatch.youtube.Fingerprint

object IsProFingerprint : Fingerprint(
    name = "isPro",
    returnType = "Z",
)

object GetSubscriptionTierFingerprint : Fingerprint(
    name = "getSubscriptionTier",
    returnType = "Ljava/lang/String;",
)
