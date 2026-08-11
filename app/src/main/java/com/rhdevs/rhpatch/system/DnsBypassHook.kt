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
                                                        inetAddresses.add(java.net.InetAddress.getByName(ip))
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
                                                            inetAddresses.add(java.net.InetAddress.getByName(ip))
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
                // Not all Android versions have this method, ignore if not found
            }
            
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch: Failed to hook DNS Bypass for $packageName: ${e.message}")
        }
    }
}
