package com.rhdevs.rhpatch.tiktok.speed

import com.rhdevs.rhpatch.youtube.AccessFlags
import com.rhdevs.rhpatch.youtube.findMethodDirect
import com.rhdevs.rhpatch.youtube.fingerprint

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
