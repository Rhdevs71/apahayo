package com.rhdevs.rhpatch.strava.upselling

import com.rhdevs.rhpatch.youtube.Opcode
import com.rhdevs.rhpatch.youtube.fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

val getModulesFingerprint = fingerprint {
    opcodes(Opcode.IGET_OBJECT)
    methodMatcher { name = "getModules" }
    classMatcher { className(".GenericLayoutEntry", StringMatchType.EndsWith) }
}
