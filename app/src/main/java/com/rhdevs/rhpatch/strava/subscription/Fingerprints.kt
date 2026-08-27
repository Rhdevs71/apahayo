package com.rhdevs.rhpatch.strava.subscription

import com.rhdevs.rhpatch.youtube.Opcode
import com.rhdevs.rhpatch.youtube.fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

val getSubscribedFingerprint = fingerprint {
    opcodes(Opcode.IGET_BOOLEAN)
    classMatcher { className(".SubscriptionDetailResponse", StringMatchType.EndsWith) }
    methodMatcher { name = "getSubscribed" }
}
