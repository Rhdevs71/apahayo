package com.rhdevs.rhpatch.youtube.youtube.misc.playertype

import com.rhdevs.rhpatch.youtube.AccessFlags
import com.rhdevs.rhpatch.youtube.Fingerprint
import com.rhdevs.rhpatch.youtube.Opcode
import com.rhdevs.rhpatch.youtube.accessFlags
import com.rhdevs.rhpatch.youtube.findClassDirect
import com.rhdevs.rhpatch.youtube.findFieldDirect
import com.rhdevs.rhpatch.youtube.findMethodDirect
import com.rhdevs.rhpatch.youtube.fingerprint
import com.rhdevs.rhpatch.youtube.opcodes
import com.rhdevs.rhpatch.youtube.parameters
import com.rhdevs.rhpatch.youtube.resourceMappings
import com.rhdevs.rhpatch.youtube.returns
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.result.FieldUsingType

val playerTypeFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("V")
    methodMatcher {
        addParamType { superClass { descriptor = "Ljava/lang/Enum;" } }
    }
    classMatcher {
        className(".YouTubePlayerOverlaysLayout", StringMatchType.EndsWith)
    }
}

val reelWatchPlayerId get() = resourceMappings["id", "reel_watch_player"]
val reelWatchPagerFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Landroid/view/View;")
    literal { reelWatchPlayerId }
}

val ReelPlayerViewField = findFieldDirect {
    reelWatchPagerFingerprint().declaredClass!!.fields.single { it.typeName.endsWith("ReelPlayerView") }
}

val ControlsState = findClassDirect {
    findClass {
        matcher {
            usingStrings("controls can be in the buffering state only if in PLAYING or PAUSED video state")
        }
    }.single()
}

val videoStateFingerprint = findMethodDirect {
    // TODO this is terrible
    val controlsStateClass = ControlsState(this).descriptor
    findMethod {
        matcher {
            accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
            returns("V")
            parameters(controlsStateClass)
            opcodes(
                Opcode.CONST_4,
                Opcode.IF_EQZ,
                Opcode.IF_EQZ,
                Opcode.IGET_OBJECT, // obfuscated parameter field name
            )
        }
    }.first()
}

val videoStateParameterField = findFieldDirect {
    videoStateFingerprint().let { method ->
        method.usingFields.distinct().single { field ->
            // obfuscated parameter field name
            field.usingType == FieldUsingType.Read && field.field.declaredClass == method.paramTypes[0]
        }.field
    }
}
