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
        // Logika dipindah ke WppXposed.kt (InitPackageResources) untuk performa lebih baik
    }.onFailure { XposedBridge.log("Rhpatch: [ThemeAMOLED] Patch failed: it") }
}
