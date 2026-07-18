package com.rhdevs.rhpatch.revanced.strava.subscription

import com.rhdevs.rhpatch.morphe.Opcode
import com.rhdevs.rhpatch.morphe.fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

val getSubscribedFingerprint = fingerprint {
    opcodes(Opcode.IGET_BOOLEAN)
    classMatcher { className(".SubscriptionDetailResponse", StringMatchType.EndsWith) }
    methodMatcher { name = "getSubscribed" }
}
