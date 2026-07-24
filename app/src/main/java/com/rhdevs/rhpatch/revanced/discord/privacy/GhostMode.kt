package com.rhdevs.rhpatch.revanced.discord.privacy

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val GhostMode = patch(
    name = "Ghost Mode (Hide Typing)",
    description = "Prevents Discord from sending typing indicators to channels and DMs."
) {
    runCatching {
        // Discord (React Native) uses OkHttp for network requests.
        // We will hook the Builder.build() method to inject an Interceptor.
        val builderClass = XposedHelpers.findClassIfExists("okhttp3.OkHttpClient\$Builder", classLoader)
        if (builderClass != null) {
            XposedHelpers.findAndHookMethod(
                builderClass,
                "build",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val interceptors = XposedHelpers.getObjectField(param.thisObject, "interceptors") as? MutableList<Any>
                            
                            val interceptorInterface = XposedHelpers.findClass("okhttp3.Interceptor", classLoader)
                            val chainInterface = XposedHelpers.findClass("okhttp3.Interceptor\$Chain", classLoader)
                            val responseClass = XposedHelpers.findClass("okhttp3.Response", classLoader)
                            val requestClass = XposedHelpers.findClass("okhttp3.Request", classLoader)
                            
                            // Create a dynamic proxy for the Interceptor interface
                            val proxyInterceptor = java.lang.reflect.Proxy.newProxyInstance(
                                classLoader,
                                arrayOf(interceptorInterface)
                            ) { _, method, args ->
                                if (method.name == "intercept") {
                                    val chain = args[0]
                                    val request = XposedHelpers.callMethod(chain, "request")
                                    val urlObj = XposedHelpers.callMethod(request, "url")
                                    val urlString = urlObj.toString()

                                    // If this is a typing indicator request, we intercept it!
                                    if (urlString.contains("/typing")) {
                                        XposedBridge.log("Rhpatch: [Discord] Blocked typing indicator to $urlString")
                                        
                                        // We need to return an empty successful okhttp3.Response to avoid crashing the JS thread.
                                        // Creating a mock response using reflection:
                                        val protocolClass = XposedHelpers.findClass("okhttp3.Protocol", classLoader)
                                        val protocolHttp11 = XposedHelpers.getStaticObjectField(protocolClass, "HTTP_1_1")
                                        
                                        val responseBuilderClass = XposedHelpers.findClass("okhttp3.Response\$Builder", classLoader)
                                        val responseBuilder = XposedHelpers.newInstance(responseBuilderClass)
                                        
                                        XposedHelpers.callMethod(responseBuilder, "request", request)
                                        XposedHelpers.callMethod(responseBuilder, "protocol", protocolHttp11)
                                        XposedHelpers.callMethod(responseBuilder, "code", 204)
                                        XposedHelpers.callMethod(responseBuilder, "message", "No Content")
                                        
                                        return@newProxyInstance XposedHelpers.callMethod(responseBuilder, "build")
                                    }
                                    
                                    // Otherwise, let the request continue
                                    return@newProxyInstance XposedHelpers.callMethod(chain, "proceed", request)
                                }
                                null
                            }
                            
                            // Add our proxy interceptor to the start of the list
                            if (interceptors != null && !interceptors.contains(proxyInterceptor)) {
                                interceptors.add(0, proxyInterceptor)
                            }
                        } catch (e: Throwable) {
                            XposedBridge.log("Rhpatch: [Discord] Failed to inject GhostMode Interceptor: ${e.message}")
                        }
                    }
                }
            )
            XposedBridge.log("Rhpatch: [Discord] Ghost Mode (OkHttp Hook) installed successfully")
        } else {
            XposedBridge.log("Rhpatch: [Discord] okhttp3.OkHttpClient\$Builder not found!")
        }
    }.onFailure {
        XposedBridge.log("Rhpatch: [Discord] Ghost Mode initialization failed: $it")
    }
}
