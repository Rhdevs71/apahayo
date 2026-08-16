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
                        if (host.isEmpty() || host.matches(Regex("^[0-9.]+$")) || host.contains(":") || host == "dns.google") return
                        
                        XposedBridge.log("Rhpatch DNS Bypass: Triggered for $host in $packageName")
                        try {
                            val future = java.util.concurrent.Executors.newSingleThreadExecutor().submit(java.util.concurrent.Callable<Array<java.net.InetAddress>?> {
                                val url = java.net.URL("https://dns.google/resolve?name=$host")
                                val connection = url.openConnection() as java.net.HttpURLConnection
                                connection.requestMethod = "GET"
                                connection.connectTimeout = 5000
                                connection.readTimeout = 5000
                                
                                if (connection.responseCode == 200) {
                                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                                    val json = org.json.JSONObject(response)
                                    val answerArray = json.optJSONArray("Answer")
                                    if (answerArray != null) {
                                        val inetAddresses = mutableListOf<java.net.InetAddress>()
                                        for (i in 0 until answerArray.length()) {
                                            val answer = answerArray.getJSONObject(i)
                                            val type = answer.optInt("type")
                                            if (type == 1 || type == 28) { // A (IPv4) or AAAA (IPv6)
                                                val ip = answer.optString("data")
                                                if (ip.isNotEmpty()) {
                                                    try {
                                                        val parts = ip.split(".")
                                                        if (parts.size == 4) {
                                                            val bytes = ByteArray(4)
                                                            for (j in 0..3) bytes[j] = parts[j].toInt().toByte()
                                                            inetAddresses.add(java.net.InetAddress.getByAddress(host, bytes))
                                                        } else {
                                                            inetAddresses.add(java.net.InetAddress.getByName(ip))
                                                        }
                                                    } catch (e: Exception) {
                                                        // ignore invalid IP format
                                                    }
                                                }
                                            }
                                        }
                                        if (inetAddresses.isNotEmpty()) {
                                            return@Callable inetAddresses.toTypedArray()
                                        }
                                    }
                                }
                                return@Callable null
                            })
                            
                            val result = future.get(5, java.util.concurrent.TimeUnit.SECONDS)
                            if (result != null) {
                                param.result = result
                                return
                            }
                        } catch (e: Exception) {
                            XposedBridge.log("Rhpatch DNS Bypass: Failed to resolve $host via DoH: ${e.message}")
                        }
                    }
                }
            )
            XposedBridge.log("Rhpatch: DNS Bypass hooked InetAddress for $packageName")
            
            // Also hook android.net.Network.getAllByName which is used by modern OkHttp
            try {
                XposedHelpers.findAndHookMethod(
                    "android.net.Network",
                    classLoader,
                    "getAllByName",
                    String::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val host = param.args[0] as? String ?: return
                            if (host.isEmpty() || host.matches(Regex("^[0-9.]+$")) || host.contains(":") || host == "dns.google") return
                            
                            try {
                                val future = java.util.concurrent.Executors.newSingleThreadExecutor().submit(java.util.concurrent.Callable<Array<java.net.InetAddress>?> {
                                    val url = java.net.URL("https://dns.google/resolve?name=$host")
                                    val connection = url.openConnection() as java.net.HttpURLConnection
                                    connection.requestMethod = "GET"
                                    connection.connectTimeout = 5000
                                    connection.readTimeout = 5000
                                    
                                    if (connection.responseCode == 200) {
                                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                                        val json = org.json.JSONObject(response)
                                        val answerArray = json.optJSONArray("Answer")
                                        if (answerArray != null) {
                                            val inetAddresses = mutableListOf<java.net.InetAddress>()
                                            for (i in 0 until answerArray.length()) {
                                                val answer = answerArray.getJSONObject(i)
                                                val type = answer.optInt("type")
                                                if (type == 1 || type == 28) {
                                                    val ip = answer.optString("data")
                                                    if (ip.isNotEmpty()) {
                                                        try {
                                                            val parts = ip.split(".")
                                                            if (parts.size == 4) {
                                                                val bytes = ByteArray(4)
                                                                for (j in 0..3) bytes[j] = parts[j].toInt().toByte()
                                                                inetAddresses.add(java.net.InetAddress.getByAddress(host, bytes))
                                                            } else {
                                                                inetAddresses.add(java.net.InetAddress.getByName(ip))
                                                            }
                                                        } catch (e: Exception) {}
                                                    }
                                                }
                                            }
                                            if (inetAddresses.isNotEmpty()) {
                                                return@Callable inetAddresses.toTypedArray()
                                            }
                                        }
                                    }
                                    return@Callable null
                                })
                                
                                val result = future.get(5, java.util.concurrent.TimeUnit.SECONDS)
                                if (result != null) {
                                    param.result = result
                                    return
                                }
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                    }
                )
                XposedBridge.log("Rhpatch: DNS Bypass hooked android.net.Network for $packageName")
            } catch (e: Throwable) {
                // Ignore
            }
            
            // Hook WebViewClient to intercept WebView requests
            try {
                XposedBridge.hookAllMethods(
                    XposedHelpers.findClass("android.webkit.WebView", classLoader),
                    "setWebViewClient",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val client = param.args[0] ?: return
                            val clientClass = client.javaClass
                            try {
                                // Prevent double hooking the same class
                                val fieldName = "rhpatch_hooked_webview"
                                try {
                                    val f = clientClass.getDeclaredField(fieldName)
                                    return
                                } catch (e: NoSuchFieldException) {
                                    // Not hooked yet, continue
                                }
                                
                                XposedBridge.hookAllMethods(clientClass, "shouldInterceptRequest", object : XC_MethodHook() {
                                    override fun beforeHookedMethod(innerParam: MethodHookParam) {
                                        try {
                                            val request = innerParam.args.firstOrNull { it?.javaClass?.name == "android.webkit.WebResourceRequest" } ?: return
                                            val urlObj = XposedHelpers.callMethod(request, "getUrl") as? android.net.Uri ?: return
                                            val urlStr = urlObj.toString()
                                            
                                            // Only intercept http/https
                                            if (!urlStr.startsWith("http")) return
                                            
                                            val host = urlObj.host ?: return
                                            if (host.isEmpty() || host.matches(Regex("^[0-9.]+$")) || host.contains(":") || host == "dns.google") return
                                            
                                            // Check if we can resolve it via DoH
                                            val future = java.util.concurrent.Executors.newSingleThreadExecutor().submit(java.util.concurrent.Callable<String?> {
                                                val url = java.net.URL("https://dns.google/resolve?name=$host")
                                                val connection = url.openConnection() as java.net.HttpURLConnection
                                                connection.requestMethod = "GET"
                                                connection.connectTimeout = 3000
                                                connection.readTimeout = 3000
                                                
                                                if (connection.responseCode == 200) {
                                                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                                                    val json = org.json.JSONObject(response)
                                                    val answerArray = json.optJSONArray("Answer")
                                                    if (answerArray != null) {
                                                        for (i in 0 until answerArray.length()) {
                                                            val answer = answerArray.getJSONObject(i)
                                                            val type = answer.optInt("type")
                                                            if (type == 1 || type == 28) {
                                                                val ip = answer.optString("data")
                                                                if (ip.isNotEmpty()) return@Callable ip
                                                            }
                                                        }
                                                    }
                                                }
                                                return@Callable null
                                            })
                                            
                                            val resolvedIp = future.get(3, java.util.concurrent.TimeUnit.SECONDS)
                                            if (resolvedIp != null) {
                                                // Manually fetch using OkHttp or HttpsURLConnection with the resolved IP
                                                // For simplicity, we just let the standard Java HttpsURLConnection do it, 
                                                // which will trigger our InetAddress hook above!
                                                val connection = java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection
                                                val method = XposedHelpers.callMethod(request, "getMethod") as String
                                                connection.requestMethod = method
                                                
                                                val headers = XposedHelpers.callMethod(request, "getRequestHeaders") as? Map<String, String>
                                                headers?.forEach { (k, v) -> connection.setRequestProperty(k, v) }
                                                
                                                val responseCode = connection.responseCode
                                                val responseMessage = connection.responseMessage
                                                val contentType = connection.contentType ?: "application/octet-stream"
                                                val encoding = connection.contentEncoding ?: "utf-8"
                                                
                                                val mimeType = contentType.split(";")[0]
                                                val inputStream = if (responseCode >= 400) connection.errorStream else connection.inputStream
                                                
                                                val webResourceResponseClass = XposedHelpers.findClass("android.webkit.WebResourceResponse", classLoader)
                                                val responseObj = XposedHelpers.newInstance(webResourceResponseClass, mimeType, encoding, responseCode, responseMessage, connection.headerFields.mapValues { it.value.joinToString(",") }, inputStream)
                                                
                                                innerParam.result = responseObj
                                            }
                                        } catch (e: Exception) {
                                            // Fallback to default
                                        }
                                    }
                                })
                                // Mark as hooked
                                // Since we can't easily add fields dynamically, we will just use a static Set of class names
                                hookedWebViewClients.add(clientClass.name)
                            } catch (e: Exception) {}
                        }
                    }
                )
                XposedBridge.log("Rhpatch: DNS Bypass hooked WebView for $packageName")
            } catch (e: Throwable) {
                // Ignore if WebView is not available
            }
            
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch: Failed to hook DNS Bypass for $packageName: ${e.message}")
        }
    }
    
    private val hookedWebViewClients = mutableSetOf<String>()
}
