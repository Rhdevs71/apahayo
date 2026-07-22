package com.rhdevs.rhpatch.revanced.googlephotos.misc.features

import android.os.Build
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import com.rhdevs.rhpatch.patch
import org.luckypray.dexkit.wrap.DexMethod

val SpoofFeaturesPatch = patch(
    name = "Spoof features",
    description = "Spoofs the device to enable Google Pixel exclusive features, including unlimited storage.",
) {
    XposedHelpers.findAndHookMethod("android.app.Application", classLoader, "onCreate", object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            try {
                XposedHelpers.setStaticObjectField(Build::class.java, "BRAND", "google")
                XposedHelpers.setStaticObjectField(Build::class.java, "MANUFACTURER", "Google")
                XposedHelpers.setStaticObjectField(Build::class.java, "MODEL", "Pixel XL")
                XposedHelpers.setStaticObjectField(Build::class.java, "DEVICE", "marlin")
                XposedHelpers.setStaticObjectField(Build::class.java, "PRODUCT", "marlin")
                XposedHelpers.setStaticObjectField(Build::class.java, "FINGERPRINT", "google/marlin/marlin:10/QP1A.191005.007.A3/5972272:user/release-keys")
            } catch (e: Exception) {
                // Ignore
            }
        }
    })

    val featuresToEnable = setOf(
        "com.google.android.apps.photos.NEXUS_PRELOAD",
        "com.google.android.apps.photos.nexus_preload",
    )

    val featuresToDisable = setOf(
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
    )

    val hook = object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            val feature = param.args[0] as String
            param.result = when (feature) {
                in featuresToEnable -> true
                in featuresToDisable -> false
                else -> return
            }
        }
    }

    DexMethod("Landroid/app/ApplicationPackageManager;->hasSystemFeature(Ljava/lang/String;)Z")
        .hookMethod(hook)
    DexMethod("Landroid/app/ApplicationPackageManager;->hasSystemFeature(Ljava/lang/String;I)Z")
        .hookMethod(hook)
}
