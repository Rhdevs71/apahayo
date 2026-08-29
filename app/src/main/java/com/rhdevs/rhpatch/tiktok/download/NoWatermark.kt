package com.rhdevs.rhpatch.tiktok.download

import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedBridge
import com.rhdevs.rhpatch.patch

val NoWatermark = patch(
    name = "No Watermark",
) {
    ::aclCommonShareGetCode.hookMethod(XC_MethodReplacement.returnConstant(0))
    ::aclCommonShareGetShowType.hookMethod(XC_MethodReplacement.returnConstant(2))
    ::aclCommonShareGetTranscode.hookMethod(XC_MethodReplacement.returnConstant(1))

    try {
        val awemeClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.model.Aweme", classLoader)
        if (awemeClass != null) {
            XposedHelpers.findAndHookMethod(awemeClass, "getVideo", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val video = param.result ?: return
                    try {
                        val downloadNoWatermarkAddr = XposedHelpers.getObjectField(video, "downloadNoWatermarkAddr")
                        val playAddrBytevc1 = XposedHelpers.getObjectField(video, "playAddrBytevc1")
                        val h264PlayAddr = XposedHelpers.getObjectField(video, "h264PlayAddr")
                        val playAddr = XposedHelpers.getObjectField(video, "playAddr")
                        
                        if (playAddrBytevc1 != null) {
                            XposedHelpers.setObjectField(video, "downloadNoWatermarkAddr", playAddrBytevc1)
                        } else if (h264PlayAddr != null) {
                            XposedHelpers.setObjectField(video, "downloadNoWatermarkAddr", h264PlayAddr)
                        } else if (playAddr != null) {
                            XposedHelpers.setObjectField(video, "downloadNoWatermarkAddr", playAddr)
                        }
                    } catch (e: Throwable) {
                        // Ignore reflection errors on fields if TikTok obfuscated them
                    }
                }
            })
            XposedBridge.log("Rhpatch: Successfully hooked Aweme.getVideo for NoWatermark")
        }
    } catch (e: Throwable) {
        XposedBridge.log("Rhpatch: Failed to hook Aweme.getVideo for NoWatermark: ${e.message}")
    }
}
