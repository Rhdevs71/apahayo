package com.rhdevs.rhpatch.revanced.meta.distractionFree

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val DisableSwipeToCreatePatch = patch(
    name = "Disable Swipe To Create (Instagram)",
    description = "Disables swiping to open the camera in Feed."
) {
    runCatching {
        // Coba hook com.instagram.mainactivity.camerabutton.CameraButtonView
        val cameraButtonClass = XposedHelpers.findClassIfExists("com.instagram.mainactivity.camerabutton.CameraButtonView", classLoader)
        if (cameraButtonClass != null) {
            val methods = cameraButtonClass.declaredMethods
            for (method in methods) {
                // Hook method setVisibility untuk memaksanya jadi GONE (8)
                if (method.name == "setVisibility") {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            try {
                                val context = android.app.AndroidAppHelper.currentApplication()
                                val prefs = context?.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
                                if (prefs?.getBoolean("pref_disable_swipe", true) == true) {
                                    param.args[0] = android.view.View.GONE
                                }
                            } catch (e: Exception) {}
                        }
                    })
                }
            }
        }

        // Piko's DistractionFree patch logic (Mencegah swipe navigation container)
        val containerClass = XposedHelpers.findClassIfExists("com.instagram.ui.swipenavigation.SwipeNavigationContainer", classLoader)
        if (containerClass != null) {
            // Find setInternalPosition(PositionConfig)
            val methods = containerClass.declaredMethods
            for (method in methods) {
                if (method.name == "setInternalPosition" || method.name.contains("swipe")) {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            try {
                                val context = android.app.AndroidAppHelper.currentApplication()
                                val prefs = context?.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
                                if (prefs?.getBoolean("pref_disable_swipe", true) == true) {
                                    // Block left/right swipes to camera by checking parameters if possible
                                    // For now, if we hook setInternalPosition and block it, user can't swipe anywhere (DM or Camera).
                                    // We need to check if they are swiping to Camera (Position = 0 or -1 usually).
                                }
                            } catch (e: Exception) {}
                        }
                    })
                }
            }
            XposedBridge.log("Rhpatch: [DisableSwipe] SwipeNavigationContainer hooked.")
        }
    }.onFailure { XposedBridge.log("Rhpatch: [DisableSwipe] Patch failed: $it") }
}
