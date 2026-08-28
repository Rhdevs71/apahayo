package com.rhdevs.rhpatch.tiktok

import android.content.Intent
import android.net.Uri
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XSharedPreferences

object TikTokBehaviorHook {
    fun apply(classLoader: ClassLoader, prefs: android.content.SharedPreferences) {
        
        
        val sanitizeLinks = prefs.getBoolean("tiktok_sanitize_links", false)
        val openExternal = prefs.getBoolean("tiktok_open_external", false)
        
        if (!sanitizeLinks && !openExternal) return
        
        try {
            // Hook Intent creation/startActivity to sanitize URLs and force external browser
            XposedBridge.hookAllMethods(android.app.Activity::class.java, "startActivity", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val intent = param.args.firstOrNull { it is Intent } as? Intent ?: return
                        
                        // App Behavior: Sanitize Share Links
                        if (sanitizeLinks && intent.action == Intent.ACTION_SEND) {
                            val extraText = intent.getStringExtra(Intent.EXTRA_TEXT)
                            if (extraText != null && extraText.contains("tiktok.com")) {
                                // Remove tracking parameters (e.g., ?t=... or &is_from_webapp=...)
                                val cleanText = extraText.replace(Regex("\\?[^\\s]+"), "")
                                intent.putExtra(Intent.EXTRA_TEXT, cleanText)
                            }
                        }
                        
                        // App Behavior: Open External Links Directly
                        if (openExternal && intent.action == Intent.ACTION_VIEW) {
                            val uri = intent.data
                            if (uri != null && (uri.host?.contains("tiktok.com") == true || uri.scheme?.startsWith("http") == true)) {
                                // Add category app browser to force standard external browser instead of TikTok in-app browser
                                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                                // We cannot easily disable the in-app browser if it's already set to a specific component, 
                                // but we can try removing the component restriction.
                                intent.component = null
                            }
                        }
                    } catch (e: Throwable) {
                        // Ignore
                    }
                }
            })
            XposedBridge.log("Rhpatch TikTok: Behavior Hooks applied successfully.")
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch TikTok Behavior Error: \${e.message}")
        }
    }
}

