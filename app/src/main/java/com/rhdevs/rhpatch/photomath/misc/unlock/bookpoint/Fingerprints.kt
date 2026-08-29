package com.rhdevs.rhpatch.photomath.misc.unlock.bookpoint

import com.rhdevs.rhpatch.youtube.AccessFlags
import com.rhdevs.rhpatch.youtube.fingerprint

val isBookpointEnabledFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Z")
    parameters()
    strings(
        "NoGeoData",
        "NoCountryInGeo",
        "RemoteConfig",
        "GeoRCMismatch"
    )
}
