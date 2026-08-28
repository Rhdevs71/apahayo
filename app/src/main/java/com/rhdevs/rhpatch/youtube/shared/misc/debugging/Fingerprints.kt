package com.rhdevs.rhpatch.youtube.shared.misc.debugging

import com.rhdevs.rhpatch.youtube.AccessFlags
import com.rhdevs.rhpatch.TargetApp
import com.rhdevs.rhpatch.youtube.accessFlags
import com.rhdevs.rhpatch.youtube.findClassDirect
import com.rhdevs.rhpatch.youtube.fingerprint
import com.rhdevs.rhpatch.youtube.returns
import com.rhdevs.rhpatch.youtube.strings

internal val experimentalFeatureFlagParentFingerprint = findClassDirect {
    findMethod {
        matcher {
            accessFlags(AccessFlags.STATIC)
            returns("L")
            strings("Unable to parse proto typed experiment flag: ")
        }
    }.filter { methodData ->
        methodData.paramTypeNames.let {
            // Early targets is: "L", "J", "[B"
            // Later targets is: "L", "J"
            (it.size == 2 || it.size == 3) && it[1] == "long"
        }
    }.map { it.declaredClass }.distinct().single()!!
}

internal val experimentalBooleanFeatureFlagFingerprint = fingerprint {
    classFingerprint(experimentalFeatureFlagParentFingerprint)
    accessFlags(AccessFlags.STATIC)
    returns("Z")
    parameters("L", "J", "Z")
}

internal val experimentalDoubleFeatureFlagFingerprint = fingerprint {
    classFingerprint(experimentalFeatureFlagParentFingerprint)
    accessFlags(AccessFlags.STATIC)
    returns("D")
    parameters("L", "J", "D")
}

internal val experimentalLongFeatureFlagFingerprint = fingerprint {
    classFingerprint(experimentalFeatureFlagParentFingerprint)
    accessFlags(AccessFlags.STATIC)
    returns("J")
    parameters("L", "J", "J")
}

@get:TargetApp("youtube")
internal val experimentalStringFeatureFlagFingerprint = fingerprint {
    classFingerprint(experimentalFeatureFlagParentFingerprint)
    accessFlags(AccessFlags.STATIC)
    returns("Ljava/lang/String;")
    parameters("L", "J", "Ljava/lang/String;")
}
