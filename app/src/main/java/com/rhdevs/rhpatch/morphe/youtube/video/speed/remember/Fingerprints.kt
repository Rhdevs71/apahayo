package com.rhdevs.rhpatch.morphe.youtube.video.speed.remember

import com.rhdevs.rhpatch.morphe.findFieldDirect
import com.rhdevs.rhpatch.morphe.fingerprint

internal val initializePlaybackSpeedValuesFingerprint = fingerprint {
    parameters("[L", "I")
    strings("menu_item_playback_speed")
}

val onItemClickListenerClassFieldReference = findFieldDirect {
    initializePlaybackSpeedValuesFingerprint().usingFields.first().field
}
