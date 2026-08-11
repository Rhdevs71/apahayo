package com.rhdevs.rhpatch.system

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

object DnsBypassHook {
    fun hook(classLoader: ClassLoader, packageName: String, prefs: de.robv.android.xposed.XSharedPreferences) {
        prefs.reload()
        if (!prefs.getBoolean("dns_bypass_enabled", false)) return
        val whitelist = prefs.getString("dns_bypass_whitelist", "") ?: ""
        
        // Simple comma-separated check
        val allowedApps = whitelist.split(",").map { it.trim() }
        if (!allowedApps.contains(packageName)) return

        try {
            XposedHelpers.findAndHookMethod(
                "java.net.InetAddress",
                classLoader,
                "getAllByName",
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val host = param.args[0] as? String ?: return
                        if (host.isEmpty() || host.matches(Regex("^[0-9.]+$"))) return
                        
                        XposedBridge.log("Rhpatch DNS Bypass: Triggered for $host in $packageName")
                        // Full Custom DNS Resolver logic would go here, e.g. querying 8.8.8.8 via UDP 
                        // and returning the constructed InetAddress array via param.result
                    }
                }
            )
            XposedBridge.log("Rhpatch: DNS Bypass hooked for $packageName")
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch: Failed to hook DNS Bypass for $packageName: ${e.message}")
        }
    }
}
