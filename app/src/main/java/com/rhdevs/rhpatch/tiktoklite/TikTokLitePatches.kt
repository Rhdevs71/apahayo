package com.rhdevs.rhpatch.tiktoklite

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val TikTokLiteRemoveAdsPatch = patch(
    name = "TikTok Lite AdBlock & Video Downloader",
    description = "Menghilangkan iklan feed, iklan splash, dan membuka izin download video tanpa watermark"
) {
    // 1. Hook Aweme Ad Gates (is3rdAd, isAppAd, isMsdkAd, isSoftAd, isMarketplace)
    runCatching {
        val awemeClasses = listOf(
            "com.ss.android.ugc.aweme.feed.model.Aweme",
            "com.ss.android.ugc.aweme.feed.model.AwemeRawAd",
            "com.ss.android.ugc.aweme.feed.model.AwemeSplashInfo"
        )
        for (className in awemeClasses) {
            val cls = XposedHelpers.findClassIfExists(className, classLoader) ?: continue
            for (method in cls.declaredMethods) {
                val mName = method.name.lowercase()
                if (method.returnType == Boolean::class.javaPrimitiveType) {
                    if (mName == "is3rdad" || mName == "isappad" || mName == "ismsdkad" || 
                        mName == "issoftad" || mName == "ismarketplace" || mName == "isad" ||
                        mName.contains("ispreventdownload")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false))
                    } else if (mName.contains("isallowdownload") || mName.contains("candownload")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true))
                    }
                }
            }
        }
    }

    // 2. Disable Splash Ad Init Task
    runCatching {
        val splashClasses = listOf(
            "com.ss.android.ugc.aweme.legoImp.task.SplashAdInitTask",
            "com.ss.android.ugc.aweme.splash.SplashAdActivity"
        )
        for (className in splashClasses) {
            val cls = XposedHelpers.findClassIfExists(className, classLoader) ?: continue
            for (method in cls.declaredMethods) {
                if (method.name == "run" || method.name == "execute") {
                    XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(null))
                }
            }
        }
    }
}

val TikTokLitePatches = arrayOf(TikTokLiteRemoveAdsPatch)
