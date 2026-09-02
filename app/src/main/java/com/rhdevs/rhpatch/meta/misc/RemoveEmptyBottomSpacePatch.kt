package com.rhdevs.rhpatch.meta.misc

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

val RemoveEmptyBottomSpacePatch = patch(
    name = "Hapus Ruang Kosong Bawah",
    description = "Hilangkan space kosong di bawah layar"
) {
    runCatching {
        val processName = android.app.Application.getProcessName()
        if (processName != appContext.packageName) return@runCatching

        if (!com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.init(appContext)) return@runCatching

        val methods = com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.findMethodUsingStrings(
            "android", "config_showNavigationBar", "_hasNavigationBar_notFound",
            returnType = "boolean"
        )

        if (methods.isNotEmpty()) {
            XposedBridge.hookMethod(methods.first(), object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val prefs = de.robv.android.xposed.XSharedPreferences("com.rhdevs.rhpatch", "com.instagram.android")
                        prefs.makeWorldReadable()
                        if (prefs.getBoolean("pref_remove_empty_bottom", false)) {
                            param.result = false
                        }
                    } catch (e: Exception) {}
                }
            })
        }
    }.onFailure { XposedBridge.log("Rhpatch: [RemoveEmptyBottomSpace] Patch failed: it") }
}
