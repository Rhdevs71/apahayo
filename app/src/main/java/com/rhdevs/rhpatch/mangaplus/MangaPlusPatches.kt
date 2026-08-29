package com.rhdevs.rhpatch.mangaplus

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val MangaPlusUnlockPatch = patch(
    name = "MangaPlus AdFree & Reader Tweaks",
    description = "Membaca manga tanpa jeda iklan dan navigasi bab lancar"
) {
    runCatching {
        val adClasses = listOf(
            "jp.co.shueisha.mangaplus.ad.AdManager",
            "jp.co.shueisha.mangaplus.ad.AdProvider",
            "jp.co.shueisha.mangaplus.model.UserStatus"
        )
        for (className in adClasses) {
            val cls = XposedHelpers.findClassIfExists(className, classLoader) ?: continue
            for (method in cls.declaredMethods) {
                val mName = method.name.lowercase()
                if (method.returnType == Boolean::class.javaPrimitiveType) {
                    if (mName.contains("isadfree") || mName.contains("ispremium") || mName.contains("issubscribed")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true))
                    } else if (mName.contains("shouldshowad") || mName.contains("hasad")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false))
                    }
                }
            }
        }
    }
}

val MangaPlusPatches = arrayOf(MangaPlusUnlockPatch)
