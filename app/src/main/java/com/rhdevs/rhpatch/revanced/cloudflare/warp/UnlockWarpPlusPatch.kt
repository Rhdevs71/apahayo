package com.rhdevs.rhpatch.revanced.cloudflare.warp

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import com.rhdevs.rhpatch.patch

val UnlockWarpPlusPatch = patch(
    name = "Spoof WARP+ Unlimited UI",
    description = "Unlocks WARP+ UI locally by spoofing AccountData.",
) {
    try {
        val warpPlusStateEnum = XposedHelpers.findClass("com.cloudflare.app.data.warpapi.WarpPlusState", classLoader)
        val unlimitedValue = XposedHelpers.getStaticObjectField(warpPlusStateEnum, "UNLIMITED")

        XposedBridge.hookAllConstructors(
            XposedHelpers.findClass("com.cloudflare.app.data.warpapi.AccountData", classLoader),
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.args.size >= 2 && param.args[1] != null && param.args[1].javaClass.name == "com.cloudflare.app.data.warpapi.WarpPlusState") {
                        param.args[1] = unlimitedValue
                    }
                }
            }
        )
    } catch (e: Exception) {
        XposedBridge.log("Failed to hook WARP+: ${e.message}")
    }
}
