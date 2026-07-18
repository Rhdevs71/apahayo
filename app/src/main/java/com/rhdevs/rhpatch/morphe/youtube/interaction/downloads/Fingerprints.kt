package com.rhdevs.rhpatch.morphe.youtube.interaction.downloads

import com.rhdevs.rhpatch.morphe.AccessFlags
import com.rhdevs.rhpatch.morphe.Fingerprint
import com.rhdevs.rhpatch.morphe.anyInstruction
import com.rhdevs.rhpatch.morphe.string

internal object OfflineVideoEndpointFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        "Ljava/util/Map;",
        "L",
        "Ljava/lang/String", // VideoId
        "L",
    ),
    filters = listOf(
        anyInstruction(
            string("Unsupported Offline Video Action: "), // 21.14 and lower
            string("Unsupported Offline Video Action: %s") // 21.15+
        )
    ),
    custom = {
        addUsingString("Unsupported Offline Video Action: ")
    }
)
