package com.rhdevs.rhpatch.meta.misc

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import android.graphics.Color

val ThemeAMOLED = patch(
    name = "Tema AMOLED (Pitch Black)",
    description = "Mengubah warna latar belakang aplikasi menjadi hitam pekat."
) {
    runCatching {
        // Hook setContentView on all Activities to set root view background to BLACK
        XposedBridge.hookAllMethods(android.app.Activity::class.java, "setContentView", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val activity = param.thisObject as? android.app.Activity ?: return
                    val prefs = de.robv.android.xposed.XSharedPreferences("com.rhdevs.rhpatch", "com.instagram.android")
                    prefs.makeWorldReadable()
                    if (prefs.getBoolean("Tema AMOLED (Pitch Black)", true)) {
                        activity.window.decorView.setBackgroundColor(Color.BLACK)
                    }
                } catch (e: Exception) {}
            }
        })
    }.onFailure { XposedBridge.log("Rhpatch: [ThemeAMOLED] Patch failed: it") }
}
