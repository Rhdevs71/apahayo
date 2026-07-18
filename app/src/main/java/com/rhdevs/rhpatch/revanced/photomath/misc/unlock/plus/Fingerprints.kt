package com.rhdevs.rhpatch.revanced.photomath.misc.unlock.plus

import com.rhdevs.rhpatch.morphe.AccessFlags
import com.rhdevs.rhpatch.morphe.fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

val isPlusUnlockedFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Z")
    strings("genius")
    classMatcher { className(".User", StringMatchType.EndsWith) }
}
