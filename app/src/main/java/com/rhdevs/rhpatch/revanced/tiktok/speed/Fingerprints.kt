package com.rhdevs.rhpatch.revanced.tiktok.speed

import com.rhdevs.rhpatch.morphe.AccessFlags
import com.rhdevs.rhpatch.morphe.findMethodDirect
import com.rhdevs.rhpatch.morphe.fingerprint

val getSpeedFingerprint = findMethodDirect(
    fingerprint {
        name("onFeedSpeedSelectedEvent")
        definingClass(".*BaseListFragmentPanel.*")
    }
)

val setSpeedFingerprint = findMethodDirect(
    fingerprint {
        name("invoke")
        accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
        returns("java.lang.Object")
        strings("playback_speed")
        parameters()
    }
)
