package com.rhdevs.rhpatch.twitch

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val TwitchAdBlockAndFeaturesPatch = patch(
    name = "Twitch AdBlock, Anti-Delete Chat & Auto-Claim Points",
    description = "Memblokir iklan video/audio, menampilkan pesan obrolan yang terhapus, dan auto klaim poin channel"
) {
    // 1. Block Stream Video Ads
    runCatching {
        val adManagerCls = XposedHelpers.findClassIfExists("tv.twitch.android.models.ads.AdModel", classLoader)
        if (adManagerCls != null) {
            for (method in adManagerCls.declaredMethods) {
                if (method.name.lowercase().contains("isad") && method.returnType == Boolean::class.javaPrimitiveType) {
                    XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false))
                }
            }
        }
    }

    // 2. Auto-Claim Channel Points
    runCatching {
        val pointsCls = XposedHelpers.findClassIfExists("tv.twitch.android.feature.points.ChannelPointsPresenter", classLoader)
        if (pointsCls != null) {
            for (method in pointsCls.declaredMethods) {
                if (method.name.lowercase().contains("claim") || method.name.lowercase().contains("canclaim")) {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            param.result = true
                        }
                    })
                }
            }
        }
    }
}

val TwitchPatches = arrayOf(TwitchAdBlockAndFeaturesPatch)
