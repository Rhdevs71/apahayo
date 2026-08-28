package com.rhdevs.rhpatch.meta.feed

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val HideSuggestedContent = patch(
    name = "Sembunyikan Konten Disarankan",
    description = "Hides suggested stories, reels, threads across all feeds.",
) {
    runCatching {
        // Safe interception via JSONObject initialization globally to prevent Regex crashes
        XposedHelpers.findAndHookConstructor(
            "org.json.JSONObject",
            classLoader,
            String::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    var json = param.args[0] as? String ?: return
                    if (json.contains("\"tray_type\":\"suggested_users\"") || json.contains("\"netego_type\":\"suggested_users\"")) {
                        // Change the type so Instagram ignores/drops it safely
                        json = json.replace("\"tray_type\":\"suggested_users\"", "\"tray_type\":\"unknown_ignored\"")
                        json = json.replace("\"netego_type\":\"suggested_users\"", "\"netego_type\":\"unknown_ignored\"")
                        param.args[0] = json
                    }
                }
            }
        )
        XposedBridge.log("Rhpatch: [Suggested] IG JSON interception activated for Hide Content")
    }.onFailure {
        XposedBridge.log("Rhpatch: [Suggested] JSON hook failed: $it")
    }
}
