package com.rhdevs.rhpatch.tiktok

import android.app.Application
import android.content.Context
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import de.robv.android.xposed.XSharedPreferences
import com.rhdevs.rhpatch.BuildConfig
import com.crossbowffs.remotepreferences.RemotePreferences

object TikTokMainHook {
    fun handleLoadPackage(lpparam: LoadPackageParam, prefs: android.content.SharedPreferences) {
        val supportedPackages = setOf(
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.ss.android.ugc.aweme",
            "com.zhiliaoapp.musically.go",
            "com.ss.android.ugc.trill.go"
        )
        if (!supportedPackages.contains(lpparam.packageName)) return
        
        // TikTok requires the Application Context for most patches and version checking
        XposedHelpers.findAndHookMethod(
            Application::class.java,
            "onCreate",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val app = param.thisObject as Application
                        if (app.packageName != lpparam.packageName) return
                        
                        val pm = app.packageManager
                        val pInfo = pm.getPackageInfo(app.packageName, 0)
                        val versionName = pInfo.versionName ?: "unknown"
                        
                        XposedBridge.log("Rhpatch TikTok: Detected version $versionName for ${app.packageName}")
                        
                        // Fallback to RemotePreferences because XSharedPreferences often fails due to SELinux
                        val xprefs = prefs as? XSharedPreferences
                        val actualPrefs = if (xprefs != null && xprefs.file.canRead()) {
                            xprefs
                        } else {
                            RemotePreferences(app, BuildConfig.APPLICATION_ID + ".preferences", "prefs")
                        }
                        
                        // Apply hooks with app's classLoader (contains multidex)
                        applyHooks(app.classLoader, actualPrefs, app)
                        XposedBridge.log("Rhpatch TikTok: Successfully applied all hooks for ${app.packageName} (version $versionName)")
                    } catch (e: Throwable) {
                        XposedBridge.log("Rhpatch TikTok Init Error: ${e.message}")
                    }
                }
            }
        )
    }
    
    private fun applyHooks(classLoader: ClassLoader, prefs: android.content.SharedPreferences, context: Context) {
        XposedBridge.log("Rhpatch TikTok: Initializing modular hooks...")
        TikTokSimSpoofHook.apply(classLoader, prefs)
        TikTokMenuHook.apply(classLoader, prefs)
        TikTokFeedFilterHook.apply(classLoader, prefs)
        TikTokDownloadsHook.init(classLoader, prefs)
        TikTokInterfaceHook.apply(classLoader, prefs)
        TikTokBehaviorHook.apply(classLoader, prefs)
    }
}
