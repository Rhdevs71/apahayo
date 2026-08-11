package com.rhdevs.rhpatch.revanced.meta.privacy

import com.rhdevs.rhpatch.morphe.AccessFlags
import com.rhdevs.rhpatch.morphe.findMethodDirect
import com.rhdevs.rhpatch.morphe.fingerprint
import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.hookMethod
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import com.rhdevs.rhpatch.morphe.findMethodListDirect
import com.rhdevs.rhpatch.morphe.fingerprintList

val DMSeenFingerprint = findMethodDirect(
    fingerprint {
        returns("V")
        strings("mark_thread_seen-", "thread_id", "action")
        accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL)
    }
)

val StorySeenFingerprints = findMethodListDirect(
    fingerprintList {
        returns("Z")
        classMatcher {
            strings("media/seen/?reel=%s&live_vod=0")
        }
    }
)

val GhostModePatch = patch(
    name = "Instagram Ghost Mode",
    description = "Sembunyikan status dilihat pada DM dan Story (Fitur Piko)"
) {
    // DM Seen Hook
    runCatching {
        ::DMSeenFingerprint.hookMethod(XC_MethodReplacement.returnConstant(null))
        XposedBridge.log("Rhpatch: [GhostMode] DM Seen (mark_thread_seen) disabled")
    }.onFailure { XposedBridge.log("Rhpatch: [GhostMode] DMSeen hook failed: $it") }

    // Story Seen Hook
    runCatching {
        val methods = ::StorySeenFingerprints.dexMethodList
        if (methods.isNotEmpty()) {
            // Piko only patches the last method returning Z in that class!
            val dexMethod = methods.last()
            val targetMethod = dexMethod.toMethod()
            if (targetMethod != null) {
                // MetaUnobfuscator filters out abstract methods now, but doing an extra check is safe
                if (!java.lang.reflect.Modifier.isAbstract(targetMethod.modifiers)) {
                    XposedBridge.hookMethod(targetMethod, XC_MethodReplacement.returnConstant(false))
                    XposedBridge.log("Rhpatch: [GhostMode] Story Seen disabled")
                }
            } else {
                XposedBridge.log("Rhpatch: [GhostMode] Story Seen method failed to resolve")
            }
        } else {
            XposedBridge.log("Rhpatch: [GhostMode] Story Seen method not found")
        }
    }.onFailure { XposedBridge.log("Rhpatch: [GhostMode] Story Seen hook failed: $it") }
}
