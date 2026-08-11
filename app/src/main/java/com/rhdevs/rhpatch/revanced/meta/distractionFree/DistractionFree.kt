package com.rhdevs.rhpatch.revanced.meta.distractionFree

import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.revanced.meta.devkit.MetaUnobfuscator
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge


val HideNotesTray = patch(
    name = "Hide Notes Tray (Instagram)",
    description = "Hides notes tray from Inbox."
) {
    runCatching {
        if (!MetaUnobfuscator.init(appContext)) return@runCatching
        val methods = MetaUnobfuscator.findMethodUsingStrings("MainFeedInboxNotesTrayBinderGroup", "direct_inbox_notes_tray")
        methods.forEach { method ->
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = null
                }
            })
        }
    }
}

val DisableScreenshotDetection = patch(
    name = "Disable Screenshot Detection (Instagram)",
    description = "Prevents Instagram from detecting screenshots in DMs."
) {
    runCatching {
        if (!MetaUnobfuscator.init(appContext)) return@runCatching
        val methods = MetaUnobfuscator.findMethodUsingStrings("is_screenshot_detected")
        methods.forEach { method ->
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = false
                }
            })
        }
        if (methods.isNotEmpty()) {
            XposedBridge.log("Rhpatch: [Instagram] Disable Screenshot Detection hooked successfully.")
        }
    }
}

val DisableSwipeToCreate = patch(
    name = "Disable Swipe To Create (Instagram)",
    description = "Disables swiping to open the camera in Feed."
) {
    runCatching {
        if (!MetaUnobfuscator.init(appContext)) return@runCatching
        val methods = MetaUnobfuscator.findMethodUsingStrings("direct_swipe_to_camera_container")
        methods.forEach { method ->
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = true // Consumes the touch event so it doesn't swipe
                }
            })
        }
    }
}

val DistractionFreePatches = arrayOf(HideNotesTray, DisableScreenshotDetection, DisableSwipeToCreate)
