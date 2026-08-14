package com.rhdevs.rhpatch.revanced.facebook

import com.rhdevs.rhpatch.Patch
import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import android.view.View
import android.view.ViewGroup

val HideFacebookAdsPatch = patch("Hide Facebook Ads") {
    try {
        // PERBAIKAN: Hooking ViewGroup.addView menyebabkan crash pada Facebook (Litho UI).
        // Untuk sementara, patch ini dinonaktifkan hingga metode hook RecyclerView/Litho yang aman ditemukan.
        XposedBridge.log("Rhpatch: Facebook Ad blocking is temporarily disabled due to stability issues with Litho engine.")
    } catch (e: Throwable) {
        XposedBridge.log("Rhpatch: Failed to hook Facebook Ads: ${e.message}")
    }
}

val FacebookPatches = arrayOf(HideFacebookAdsPatch)
