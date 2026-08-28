package com.rhdevs.rhpatch.meta.flags

import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

val HookFlagsPatch = patch(
    name = "Override Experiment Flags (Instagram)",
    description = "Hooks Instagram's MobileConfig flag check to force enable/disable features (like Rhpatch)"
) {

    runCatching {
        // [DISABLED] Hooking MobileConfig boolean getters via Xposed is incredibly slow
        // and is highly suspected to cause the Broadcast Channels bug (timeouts/crashes).
        // Since Developer Options is currently broken anyway, we disable this aggressive hook.
        /*
        if (!MetaUnobfuscator.init(appContext)) {
            XposedBridge.log("Rhpatch: [Flags] Failed to initialize MetaUnobfuscator")
            return@runCatching
        }
        ...
        */
        XposedBridge.log("Rhpatch: [Flags] Hook disabled temporarily to fix Saluran DM bug.")
    }.onFailure {
        XposedBridge.log("Rhpatch: [Flags] Hook failed: $it")
    }
}
