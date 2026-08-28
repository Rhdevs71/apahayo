package com.rhdevs.rhpatch.discord.privacy

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val GhostMode = patch(
    name = "Ghost Mode (Hide Typing)",
    description = "Prevents Discord from sending typing indicators to channels and DMs."
) {
    runCatching {
        // Discord uses OkHttp which might be obfuscated.
        // The most resilient way to block a specific URL in an Xposed module without relying on OkHttp class names
        // is to hook java.net.URL constructor or java.net.SocketOutputStream.write if it's HTTPS.
        // However, we can try to hook okhttp3.Request$Builder.url(String) or okhttp3.OkHttpClient.newCall(Request)
        // Since Discord RN might package OkHttp without obfuscating the main classes.
        
        val okHttpClientClass = XposedHelpers.findClassIfExists("okhttp3.OkHttpClient", classLoader)
        if (okHttpClientClass != null) {
            // Hook newCall(Request)
            XposedBridge.hookAllMethods(okHttpClientClass, "newCall", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val request = param.args[0] ?: return
                        val urlObj = XposedHelpers.callMethod(request, "url")
                        val urlStr = urlObj.toString()
                        
                        if (urlStr.contains("/typing")) {
                            XposedBridge.log("Rhpatch: [Discord] Blocked typing indicator request to $urlStr (OkHttp)")
                            
                            val newBuilder = XposedHelpers.callMethod(request, "newBuilder")
                            XposedHelpers.callMethod(newBuilder, "url", "http://0.0.0.0/blocked_typing") // Fails instantly to prevent lag
                            val newRequest = XposedHelpers.callMethod(newBuilder, "build")
                            param.args[0] = newRequest
                        }
                    } catch (e: Throwable) {
                        // ignore
                    }
                }
            })
            XposedBridge.log("Rhpatch: [Discord] Ghost Mode (newCall hook) installed successfully")
        } else {
            XposedBridge.log("Rhpatch: [Discord] okhttp3.OkHttpClient not found, relying on React Native hooks.")
        }
        
        // Hook React Native NetworkingModule (used by newer Discord RN)
        val networkingModuleClass = XposedHelpers.findClassIfExists("com.facebook.react.modules.network.NetworkingModule", classLoader)
        if (networkingModuleClass != null) {
            XposedBridge.hookAllMethods(networkingModuleClass, "sendRequest", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        // sendRequest(String method, String url, int requestId, ...)
                        if (param.args.size >= 2) {
                            val urlStr = param.args[1] as? String ?: return
                            if (urlStr.contains("/typing")) {
                                XposedBridge.log("Rhpatch: [Discord] Blocked typing indicator request to $urlStr (React Native)")
                                param.result = null // Instantly cancel execution to prevent lag
                            }
                        }
                    } catch (e: Throwable) {
                        // ignore
                    }
                }
            })
            XposedBridge.log("Rhpatch: [Discord] Ghost Mode (NetworkingModule hook) installed successfully")
        }

        // Hook HttpURLConnection as a final fallback
        XposedBridge.hookAllConstructors(java.net.URL::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                try {
                    val urlStr = param.args[0] as? String ?: return
                    if (urlStr.contains("/typing") && urlStr.contains("discord")) {
                        XposedBridge.log("Rhpatch: [Discord] Blocked typing indicator request to $urlStr (java.net.URL)")
                        param.args[0] = "http://0.0.0.0/blocked_typing" // Fails instantly to prevent lag
                    }
                } catch (e: Throwable) {
                    // ignore
                }
            }
        })

    }.onFailure {
        XposedBridge.log("Rhpatch: [Discord] Ghost Mode initialization failed: \${it.message}")
    }
}
