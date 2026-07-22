package com.rhdevs.rhpatch.revanced.meta.privacy

import com.rhdevs.rhpatch.morphe.AccessFlags
import com.rhdevs.rhpatch.morphe.findMethodDirect
import com.rhdevs.rhpatch.morphe.fingerprint
import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.hookMethod
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge

val DMSeenFingerprint = findMethodDirect(
    fingerprint {
        returns("V")
        strings("mark_thread_seen-")
        accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL)
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
}
