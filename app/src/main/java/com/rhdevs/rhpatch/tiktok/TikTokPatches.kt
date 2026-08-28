package com.rhdevs.rhpatch.tiktok

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import org.json.JSONObject
import org.json.JSONArray

val TikTokRemoveAdsPatch = patch(
    name = "Blokir Iklan Feed & Splash",
    description = "Menghilangkan iklan bersponsor, iklan live, produk marketplace, dan promosi di feed TikTok"
) {
    runCatching {
        // Hook Aweme Ad checks
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
                        mName == "issoftad" || mName == "ismarketplace" || mName == "isad") {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false))
                    }
                }
            }
        }
    }
}

val TikTokDownloaderPatch = patch(
    name = "Unduh Video Tanpa Watermark",
    description = "Membuka batasan unduhan, memunculkan tombol Simpan Video, dan mengunduh video kualitas murni tanpa watermark"
) {
    runCatching {
        val awemeClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.model.Aweme", classLoader)
        if (awemeClass != null) {
            XposedBridge.hookAllMethods(awemeClass, "isPreventDownload", XC_MethodReplacement.returnConstant(false))
            XposedBridge.hookAllMethods(awemeClass, "isProhibited", XC_MethodReplacement.returnConstant(false))
            
            // Swap watermark URL with original playAddr
            XposedBridge.hookAllMethods(awemeClass, "getVideo", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val video = param.result ?: return
                        val playAddr = XposedHelpers.getObjectField(video, "playAddr")
                        val h264PlayAddr = XposedHelpers.getObjectField(video, "h264PlayAddr")
                        val fallback = playAddr ?: h264PlayAddr
                        if (fallback != null) {
                            XposedHelpers.setObjectField(video, "downloadNoWatermarkAddr", fallback)
                        }
                    } catch (_: Throwable) {}
                }
            })
        }

        val aclCommonShareClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.model.ACLCommonShare", classLoader)
        if (aclCommonShareClass != null) {
            XposedBridge.hookAllMethods(aclCommonShareClass, "getCode", XC_MethodReplacement.returnConstant(0))
            XposedBridge.hookAllMethods(aclCommonShareClass, "getShowType", XC_MethodReplacement.returnConstant(2))
        }
    }
}

val TikTokRegionBypassPatch = patch(
    name = "Bypass Batasan Wilayah (SIM Spoof)",
    description = "Membuka blokir wilayah TikTok dengan menyamarkan operator SIM ke United States (US / T-Mobile)"
) {
    runCatching {
        val telephonyManagerClass = XposedHelpers.findClassIfExists("android.telephony.TelephonyManager", classLoader)
        if (telephonyManagerClass != null) {
            XposedBridge.hookAllMethods(telephonyManagerClass, "getSimCountryIso", XC_MethodReplacement.returnConstant("us"))
            XposedBridge.hookAllMethods(telephonyManagerClass, "getNetworkCountryIso", XC_MethodReplacement.returnConstant("us"))
            XposedBridge.hookAllMethods(telephonyManagerClass, "getSimOperator", XC_MethodReplacement.returnConstant("310260"))
            XposedBridge.hookAllMethods(telephonyManagerClass, "getSimOperatorName", XC_MethodReplacement.returnConstant("T-Mobile"))
            XposedBridge.hookAllMethods(telephonyManagerClass, "getNetworkOperatorName", XC_MethodReplacement.returnConstant("T-Mobile"))
        }
    }
}

val TikTokInterfacePatch = patch(
    name = "Kustomisasi Tampilan & UI",
    description = "Sembunyikan tombol Tako AI, bersihkan tampilan feed, dan tampilkan tanggal upload video"
) {
    runCatching {
        // Intercept JSON to hide tako and clear popups
        XposedBridge.hookAllConstructors(JSONObject::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val json = param.thisObject as JSONObject
                    if (json.has("tako_info") || json.has("search_ai_info")) {
                        json.remove("tako_info")
                        json.remove("search_ai_info")
                    }
                } catch (_: Throwable) {}
            }
        })
    }
}

val TikTokFeedFilterPatch = patch(
    name = "Filter Siaran Langsung & Story",
    description = "Menyaring live streaming dan story dari beranda utama For You"
) {
    runCatching {
        XposedBridge.hookAllConstructors(JSONObject::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val json = param.thisObject as JSONObject
                    if (json.has("aweme_list")) {
                        val awemeList = json.optJSONArray("aweme_list") ?: return
                        val filteredList = JSONArray()
                        for (i in 0 until awemeList.length()) {
                            val item = awemeList.optJSONObject(i) ?: continue
                            val isLive = item.has("live_room") || item.optInt("aweme_type", 0) == 101
                            if (!isLive) {
                                filteredList.put(item)
                            }
                        }
                        json.put("aweme_list", filteredList)
                    }
                } catch (_: Throwable) {}
            }
        })
    }
}

val TikTokPatches = arrayOf(
    TikTokRemoveAdsPatch,
    TikTokDownloaderPatch,
    TikTokRegionBypassPatch,
    TikTokInterfacePatch,
    TikTokFeedFilterPatch
)
