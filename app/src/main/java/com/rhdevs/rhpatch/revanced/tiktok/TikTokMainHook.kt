package com.rhdevs.rhpatch.revanced.tiktok

import android.app.Application
import android.content.Context
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import de.robv.android.xposed.XSharedPreferences
import com.wmods.wppenhacer.BuildConfig
import com.crossbowffs.remotepreferences.RemotePreferences

object TikTokMainHook {
    fun handleLoadPackage(lpparam: LoadPackageParam, prefs: android.content.SharedPreferences) {
        if (lpparam.packageName != "com.zhiliaoapp.musically" && lpparam.packageName != "com.ss.android.ugc.trill") return
        
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
                        val versionName = pInfo.versionName
                        
                        XposedBridge.log("Rhpatch TikTok: Detected version $versionName")
                        
                        // Limit to version 46.2.3 maximum
                        if (isVersionHigherThan(versionName ?: "", "46.2.3")) {
                            XposedBridge.log("Rhpatch TikTok: Version $versionName is higher than supported 46.2.3. Aborting hooks.")
                            return
                        }
                        
                        // Fallback to RemotePreferences because XSharedPreferences often fails due to SELinux
                        val xprefs = prefs as? XSharedPreferences
                        val actualPrefs = if (xprefs != null && xprefs.file.canRead()) {
                            xprefs
                        } else {
                            RemotePreferences(app, BuildConfig.APPLICATION_ID + ".preferences", "prefs")
                        }
                        
                        // Use app.classLoader because it contains the multidex classes
                        applyHooks(app.classLoader, actualPrefs, app)
                    } catch (e: Throwable) {
                        XposedBridge.log("Rhpatch TikTok Init Error: ${e.message}")
                    }
                }
            }
        )
    }
    
    private fun applyHooks(classLoader: ClassLoader, prefs: android.content.SharedPreferences, context: Context) {
        XposedBridge.log("Rhpatch TikTok: Applying hooks...")
        TikTokSimSpoofHook.apply(classLoader, prefs)
        TikTokMenuHook.apply(classLoader, prefs)
        TikTokFeedFilterHook.apply(classLoader, prefs)
        TikTokDownloadsHook.init(classLoader, prefs)
        TikTokInterfaceHook.apply(classLoader, prefs)
        TikTokBehaviorHook.apply(classLoader, prefs)
    }
    
    private fun isVersionHigherThan(current: String, target: String): Boolean {
        return try {
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            val targetParts = target.split(".").map { it.toIntOrNull() ?: 0 }
            
            for (i in 0 until maxOf(currentParts.size, targetParts.size)) {
                val c = currentParts.getOrElse(i) { 0 }
                val t = targetParts.getOrElse(i) { 0 }
                if (c > t) return true
                if (c < t) return false
            }
            false
        } catch (e: Exception) {
            false
        }
    }
}

