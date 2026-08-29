package com.rhdevs.rhpatch.googlephotos.misc.features

import android.content.pm.FeatureInfo
import android.os.Build
import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val SpoofFeaturesPatch = patch(
    name = "Spoof features",
    description = "Spoofs the device to enable Google Pixel exclusive features, including unlimited storage.",
) {
    try {
        XposedHelpers.setStaticObjectField(Build::class.java, "BRAND", "google")
        XposedHelpers.setStaticObjectField(Build::class.java, "MANUFACTURER", "Google")
        XposedHelpers.setStaticObjectField(Build::class.java, "MODEL", "Pixel XL")
        XposedHelpers.setStaticObjectField(Build::class.java, "DEVICE", "marlin")
        XposedHelpers.setStaticObjectField(Build::class.java, "PRODUCT", "marlin")
        XposedHelpers.setStaticObjectField(Build::class.java, "HARDWARE", "marlin")
        XposedHelpers.setStaticObjectField(Build::class.java, "ID", "QP1A.191005.007.A3")
        XposedHelpers.setStaticObjectField(Build::class.java, "FINGERPRINT", "google/marlin/marlin:10/QP1A.191005.007.A3/5972272:user/release-keys")
    } catch (_: Exception) {}

    // Hook SystemProperties
    runCatching {
        val sysPropCls = XposedHelpers.findClassIfExists("android.os.SystemProperties", classLoader)
        if (sysPropCls != null) {
            XposedBridge.hookAllMethods(sysPropCls, "get", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val key = param.args.getOrNull(0) as? String ?: return
                    when (key) {
                        "ro.product.model", "ro.product.vendor.model", "ro.product.odm.model" -> param.result = "Pixel XL"
                        "ro.product.brand", "ro.product.vendor.brand", "ro.product.odm.brand" -> param.result = "google"
                        "ro.product.manufacturer", "ro.product.vendor.manufacturer" -> param.result = "Google"
                        "ro.product.device", "ro.product.name" -> param.result = "marlin"
                    }
                }
            })
        }
    }

    val featuresToEnable = setOf(
        "com.google.android.apps.photos.PIXEL_2016_PRELOAD",
        "com.google.android.apps.photos.NEXUS_PRELOAD",
        "com.google.android.apps.photos.nexus_preload",
        "com.google.android.feature.PIXEL_2016_EXPERIENCE",
        "com.google.android.feature.PIXEL_2016_PRELOAD"
    )

    val featuresToDisable = setOf(
        "com.google.android.feature.PIXEL_EXPERIENCE",
        "com.google.android.feature.PIXEL_2017_EXPERIENCE",
        "com.google.android.feature.PIXEL_2018_EXPERIENCE",
        "com.google.android.feature.PIXEL_2019_MIDYEAR_EXPERIENCE",
        "com.google.android.feature.PIXEL_2019_EXPERIENCE",
        "com.google.android.apps.photos.PIXEL_2017_PRELOAD",
        "com.google.android.apps.photos.PIXEL_2018_PRELOAD",
        "com.google.android.apps.photos.PIXEL_2019_MIDYEAR_PRELOAD",
        "com.google.android.apps.photos.PIXEL_2019_PRELOAD",
        "com.google.android.feature.PIXEL_2020_MIDYEAR_EXPERIENCE",
        "com.google.android.feature.PIXEL_2020_EXPERIENCE",
        "com.google.android.feature.PIXEL_2021_MIDYEAR_EXPERIENCE",
        "com.google.android.feature.PIXEL_2021_EXPERIENCE",
        "com.google.android.feature.PIXEL_2022_MIDYEAR_EXPERIENCE",
        "com.google.android.feature.PIXEL_2022_EXPERIENCE",
        "com.google.android.feature.PIXEL_2023_MIDYEAR_EXPERIENCE",
        "com.google.android.feature.PIXEL_2023_EXPERIENCE",
        "com.google.android.feature.PIXEL_2024_MIDYEAR_EXPERIENCE",
        "com.google.android.feature.PIXEL_2024_EXPERIENCE",
        "com.google.android.feature.PIXEL_2025_MIDYEAR_EXPERIENCE",
        "com.google.android.feature.PIXEL_2025_EXPERIENCE",
        "com.google.android.feature.PIXEL_2026_MIDYEAR_EXPERIENCE",
        "com.google.android.feature.PIXEL_2026_EXPERIENCE"
    )

    val featureHook = object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            val feature = param.args[0] as? String ?: return
            if (feature in featuresToEnable) {
                param.result = true
            } else if (feature in featuresToDisable) {
                param.result = false
            }
        }
    }

    runCatching {
        val appPkgMgr = XposedHelpers.findClassIfExists("android.app.ApplicationPackageManager", classLoader)
        if (appPkgMgr != null) {
            XposedBridge.hookAllMethods(appPkgMgr, "hasSystemFeature", featureHook)
            
            // Also hook getSystemAvailableFeatures
            XposedBridge.hookAllMethods(appPkgMgr, "getSystemAvailableFeatures", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val original = (param.result as? Array<*>)?.filterIsInstance<FeatureInfo>() ?: return
                    val filtered = original.filter { it.name !in featuresToDisable }.toMutableList()
                    for (feat in featuresToEnable) {
                        if (filtered.none { it.name == feat }) {
                            val info = FeatureInfo().apply { name = feat }
                            filtered.add(info)
                        }
                    }
                    param.result = filtered.toTypedArray()
                }
            })
        }
    }
}
