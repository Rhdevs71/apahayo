package com.rhdevs.rhpatch.revanced.tiktok

import android.view.View
import android.view.ViewGroup
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XSharedPreferences

object TikTokInterfaceHook {
    fun apply(classLoader: ClassLoader, prefs: android.content.SharedPreferences) {
        
        
        val hideFloating = prefs.getBoolean("tiktok_hide_floating", false)
        val hideCaptcha = prefs.getBoolean("tiktok_hide_captcha", false)
        val hideTako = prefs.getBoolean("tiktok_hide_tako", false)
        
        if (!hideFloating && !hideCaptcha && !hideTako) return
        
        try {
            // Hook ViewGroup.addView to intercept and hide specific views dynamically
            XposedBridge.hookAllMethods(ViewGroup::class.java, "addView", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val child = param.args[0] as? View ?: return
                        val context = child.context
                        if (child.id != View.NO_ID) {
                            val idName = context.resources.getResourceEntryName(child.id) ?: return
                            
                            var shouldHide = false
                            
                            // Interface: Hide Floating Promotions (coins, badges, timer)
                            if (hideFloating && (idName.contains("promo") || idName.contains("coin") || idName.contains("timer_banner") || idName.contains("floating_badge"))) {
                                shouldHide = true
                            }
                            
                            // Interface: Hide CAPTCHA
                            if (hideCaptcha && (idName.contains("captcha") || idName.contains("puzzle"))) {
                                shouldHide = true
                            }
                            
                            // Feed Navigation: Hide Tako AI
                            if (hideTako && (idName.contains("tako") || idName.contains("bot_feed_button"))) {
                                shouldHide = true
                            }
                            
                            if (shouldHide) {
                                child.visibility = View.GONE
                                // Set layout params to 0 to completely collapse it
                                child.layoutParams = ViewGroup.LayoutParams(0, 0)
                            }
                        }
                    } catch (e: Throwable) {
                        // Ignore view lookup errors
                    }
                }
            })
            XposedBridge.log("Rhpatch TikTok: Interface Hooks applied successfully.")
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch TikTok Interface Error: \${e.message}")
        }
    }
}

