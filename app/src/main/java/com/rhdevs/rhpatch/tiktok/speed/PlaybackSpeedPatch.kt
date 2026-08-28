package com.rhdevs.rhpatch.tiktok.speed

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import com.rhdevs.rhpatch.patch

private var rememberedSpeed = 1.0f

val PlaybackSpeed = patch(
    name = "Playback Speed",
    description = "Enables the playback speed option for all videos."
) {
    ::getSpeedFingerprint.hookMethod(object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            val speed = param.args[0] as? Float ?: return
            rememberedSpeed = speed
        }
    })

    ::setSpeedFingerprint.hookMethod(object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            // Speed enforcement logic can be added here if needed.
        }
    })
}
