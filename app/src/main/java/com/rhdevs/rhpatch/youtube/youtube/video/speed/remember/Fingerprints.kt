package com.rhdevs.rhpatch.youtube.youtube.video.speed.remember

import com.rhdevs.rhpatch.youtube.findFieldDirect
import com.rhdevs.rhpatch.youtube.fingerprint

internal val initializePlaybackSpeedValuesFingerprint = fingerprint {
    parameters("[L", "I")
    strings("menu_item_playback_speed")
}

val onItemClickListenerClassFieldReference = findFieldDirect {
    initializePlaybackSpeedValuesFingerprint().usingFields.first().field
}
