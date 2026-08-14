package com.wmods.wppenhacer.xposed.features.general

import android.app.Activity
import android.content.SharedPreferences
import android.view.Window
import android.view.WindowManager
import com.wmods.wppenhacer.xposed.core.Feature
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

class ScreenSecurityHook(loader: ClassLoader, preferences: SharedPreferences) : Feature(loader, preferences) {

    override fun getPluginName(): String {
        return "ScreenSecurityHook"
    }

    override fun doHook() {
        val antiScreenshot = prefs.getBoolean("anti_screenshot_enabled", false)
        val appSwitcherBlur = prefs.getBoolean("app_switcher_blur_enabled", false)

        if (antiScreenshot) {
            XposedBridge.log("Rhpatch: Anti-Screenshot is enabled, hooking Window flags")
            try {
                // Hook setFlags
                XposedHelpers.findAndHookMethod(
                    Window::class.java,
                    "setFlags",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val flags = param.args[0] as Int
                            val mask = param.args[1] as Int
                            if ((mask and WindowManager.LayoutParams.FLAG_SECURE) != 0) {
                                // Clear FLAG_SECURE from mask and flags
                                param.args[0] = flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                                param.args[1] = mask and WindowManager.LayoutParams.FLAG_SECURE.inv()
                            }
                        }
                    }
                )

                // Hook addFlags
                XposedHelpers.findAndHookMethod(
                    Window::class.java,
                    "addFlags",
                    Int::class.javaPrimitiveType,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val flags = param.args[0] as Int
                            if ((flags and WindowManager.LayoutParams.FLAG_SECURE) != 0) {
                                param.args[0] = flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                            }
                        }
                    }
                )
            } catch (e: Throwable) {
                XposedBridge.log("Rhpatch ScreenSecurityHook Error: ${e.message}")
            }
        }

        if (appSwitcherBlur) {
            XposedBridge.log("Rhpatch: App Switcher Blur is enabled, hooking Activity lifecycle")
            try {
                XposedHelpers.findAndHookMethod(
                    Activity::class.java,
                    "onPause",
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val activity = param.thisObject as Activity
                            // Apply secure flag to prevent recent apps screen preview leak
                            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                    }
                )

                XposedHelpers.findAndHookMethod(
                    Activity::class.java,
                    "onResume",
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val activity = param.thisObject as Activity
                            // Remove secure flag so the user can interact
                            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                    }
                )
            } catch (e: Throwable) {
                XposedBridge.log("Rhpatch AppSwitcherBlur Error: ${e.message}")
            }
        }
    }
}
