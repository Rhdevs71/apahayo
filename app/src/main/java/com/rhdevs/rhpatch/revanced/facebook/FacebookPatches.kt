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
        val viewGroupClass = XposedHelpers.findClass("android.view.ViewGroup", lpparam.classLoader)
        XposedBridge.hookAllMethods(viewGroupClass, "addView", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val child = param.args[0] as? View ?: return
                val contentDescription = child.contentDescription?.toString()?.lowercase() ?: ""
                if (contentDescription.contains("sponsored") || contentDescription.contains("bersponsor") || contentDescription.contains("ad")) {
                    child.visibility = View.GONE
                    val layoutParams = child.layoutParams
                    if (layoutParams != null) {
                        layoutParams.height = 0
                        layoutParams.width = 0
                        child.layoutParams = layoutParams
                    }
                }
            }
        })
        XposedBridge.log("Rhpatch: Successfully hooked Facebook Ads")
    } catch (e: Throwable) {
        XposedBridge.log("Rhpatch: Failed to hook Facebook Ads: ${e.message}")
    }
}

val FacebookPatches = arrayOf(HideFacebookAdsPatch)
