package com.rhdevs.rhpatch.revanced.strava.upselling

import com.rhdevs.rhpatch.morphe.Opcode
import com.rhdevs.rhpatch.morphe.fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

val getModulesFingerprint = fingerprint {
    opcodes(Opcode.IGET_OBJECT)
    methodMatcher { name = "getModules" }
    classMatcher { className(".GenericLayoutEntry", StringMatchType.EndsWith) }
}
