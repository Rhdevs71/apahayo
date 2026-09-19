package com.rhdevs.rhpatch.system

import android.content.Context
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XSharedPreferences
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.nio.ByteBuffer
import java.util.Random
import java.util.concurrent.ConcurrentHashMap

object DnsBypassHook {
    private val DNS_SERVERS = listOf("8.8.8.8", "1.1.1.1", "208.67.222.222")
    private val random = Random()
    private val dnsCache = ConcurrentHashMap<String, Array<InetAddress>>()
    
    @Volatile private var isWhitelistedCached: Boolean? = null
    @Volatile private var lastWhitelistCheckTime = 0L

    private fun getAppContext(): Context? {
        return try {
            val activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", null)
            val currentActivityThread = XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread")
            XposedHelpers.callMethod(currentActivityThread, "getApplication") as? Context
        } catch (_: Throwable) {
            null
        }
    }

    private fun isPackageWhitelisted(packageName: String, prefs: XSharedPreferences?): Boolean {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastWhitelistCheckTime < 2000L && isWhitelistedCached != null) {
            return isWhitelistedCached!!
        }
        lastWhitelistCheckTime = now

        // 1. Cek via XSharedPreferences
        try {
            prefs?.reload()
            if (prefs != null && prefs.getBoolean("dns_bypass_enabled", false)) {
                val whitelist = prefs.getString("dns_bypass_whitelist", "") ?: ""
                val allowed = whitelist.split(",").map { it.trim() }
                if (allowed.contains(packageName)) {
                    isWhitelistedCached = true
                    return true
                }
            }
        } catch (_: Throwable) {}

        // 2. Cek via RemotePreferences lintas proses (Bypass batasan SELinux Android 10-15)
        try {
            val ctx = getAppContext()
            if (ctx != null) {
                val remotePrefs = com.crossbowffs.remotepreferences.RemotePreferences(
                    ctx, "com.rhdevs.rhpatch.preferences", "prefs"
                )
                if (remotePrefs.getBoolean("dns_bypass_enabled", false)) {
                    val whitelist = remotePrefs.getString("dns_bypass_whitelist", "") ?: ""
                    val allowed = whitelist.split(",").map { it.trim() }
                    if (allowed.contains(packageName)) {
                        isWhitelistedCached = true
                        return true
                    }
                }
            }
        } catch (_: Throwable) {}

        // 3. Cek via ContentProvider SpamConfigProvider
        try {
            val ctx = getAppContext()
            if (ctx != null) {
                val uri = android.net.Uri.parse("content://com.rhdevs.rhpatch.spamconfig/dns_bypass_whitelist")
                ctx.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val whitelist = cursor.getString(1) ?: ""
                        val allowed = whitelist.split(",").map { it.trim() }
                        if (allowed.contains(packageName)) {
                            isWhitelistedCached = true
                            return true
                        }
                    }
                }
            }
        } catch (_: Throwable) {}

        isWhitelistedCached = false
        return false
    }

    fun hook(classLoader: ClassLoader, packageName: String, prefs: XSharedPreferences) {
        try {
            // Hook 1: InetAddress.getAllByName(String) -> Array<InetAddress>
            runCatching {
                XposedHelpers.findAndHookMethod(
                    InetAddress::class.java,
                    "getAllByName",
                    String::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            if (!isPackageWhitelisted(packageName, prefs)) return
                            val host = param.args[0] as? String ?: return
                            if (host.isEmpty() || host.matches(Regex("^[0-9.]+$")) || host.contains(":") || host == "localhost") return

                            val resolved = resolveHost(host)
                            if (resolved != null && resolved.isNotEmpty()) {
                                param.result = resolved
                            }
                        }
                    }
                )
            }

            // Hook 1b: InetAddress.getByName(String) -> InetAddress
            runCatching {
                XposedHelpers.findAndHookMethod(
                    InetAddress::class.java,
                    "getByName",
                    String::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            if (!isPackageWhitelisted(packageName, prefs)) return
                            val host = param.args[0] as? String ?: return
                            if (host.isEmpty() || host.matches(Regex("^[0-9.]+$")) || host.contains(":") || host == "localhost") return

                            val resolved = resolveHost(host)
                            if (resolved != null && resolved.isNotEmpty()) {
                                param.result = resolved[0]
                            }
                        }
                    }
                )
            }

            // Hook 1c: InetAddress.getAllByNameImpl (Android 10-15 internal method)
            runCatching {
                val implMethod = InetAddress::class.java.declaredMethods.firstOrNull { 
                    it.name == "getAllByNameImpl" && it.parameterTypes.isNotEmpty() && it.parameterTypes[0] == String::class.java 
                }
                if (implMethod != null) {
                    XposedBridge.hookMethod(implMethod, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            if (!isPackageWhitelisted(packageName, prefs)) return
                            val host = param.args[0] as? String ?: return
                            if (host.isEmpty() || host.matches(Regex("^[0-9.]+$")) || host.contains(":") || host == "localhost") return

                            val resolved = resolveHost(host)
                            if (resolved != null && resolved.isNotEmpty()) {
                                param.result = resolved
                            }
                        }
                    })
                }
            }

            XposedBridge.log("Rhpatch: DNS AdGuard/Bypass Engine terdaftar untuk: " + packageName)
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch: DNS Bypass gagal dimuat - " + e.message)
        }
    }

    private fun resolveHost(host: String): Array<InetAddress>? {
        // Cek In-Memory Cache (0ms response)
        dnsCache[host]?.let { return it }

        // 1. Coba DNS-over-HTTPS (DoH) via Cloudflare (Port 443 kebal blokir ISP dan AdGuard)
        try {
            val dohResult = resolveDohCloudflare(host)
            if (dohResult.isNotEmpty()) {
                val array = dohResult.toTypedArray()
                dnsCache[host] = array
                return array
            }
        } catch (_: Throwable) {}

        // 2. Coba DNS-over-HTTPS (DoH) via Google
        try {
            val dohResult = resolveDohGoogle(host)
            if (dohResult.isNotEmpty()) {
                val array = dohResult.toTypedArray()
                dnsCache[host] = array
                return array
            }
        } catch (_: Throwable) {}

        // 3. Fallback ke UDP Port 53
        try {
            val udpResult = resolveDnsUdp(host)
            if (udpResult.isNotEmpty()) {
                val array = udpResult.toTypedArray()
                dnsCache[host] = array
                return array
            }
        } catch (_: Throwable) {}

        return null
    }

    private fun resolveDohCloudflare(host: String): List<InetAddress> {
        val url = URL("https://1.1.1.1/dns-query?name=" + host + "&type=A")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/dns-json")
        conn.connectTimeout = 2000
        conn.readTimeout = 2000

        if (conn.responseCode == 200) {
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val answers = json.optJSONArray("Answer") ?: return emptyList()
            val list = mutableListOf<InetAddress>()
            for (i in 0 until answers.length()) {
                val item = answers.getJSONObject(i)
                if (item.optInt("type", 0) == 1) { // Type 1 = A record (IPv4)
                    val ipStr = item.optString("data", "")
                    if (ipStr.isNotEmpty() && ipStr.matches(Regex("^[0-9.]+$"))) {
                        val parts = ipStr.split(".").map { it.toInt().toByte() }.toByteArray()
                        list.add(InetAddress.getByAddress(host, parts))
                    }
                }
            }
            return list
        }
        return emptyList()
    }

    private fun resolveDohGoogle(host: String): List<InetAddress> {
        val url = URL("https://dns.google/resolve?name=" + host + "&type=A")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 2000
        conn.readTimeout = 2000

        if (conn.responseCode == 200) {
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val answers = json.optJSONArray("Answer") ?: return emptyList()
            val list = mutableListOf<InetAddress>()
            for (i in 0 until answers.length()) {
                val item = answers.getJSONObject(i)
                if (item.optInt("type", 0) == 1) {
                    val ipStr = item.optString("data", "")
                    if (ipStr.isNotEmpty() && ipStr.matches(Regex("^[0-9.]+$"))) {
                        val parts = ipStr.split(".").map { it.toInt().toByte() }.toByteArray()
                        list.add(InetAddress.getByAddress(host, parts))
                    }
                }
            }
            return list
        }
        return emptyList()
    }

    private fun resolveDnsUdp(host: String): List<InetAddress> {
        val queryId = random.nextInt(65535)
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        dos.writeShort(queryId)
        dos.writeShort(0x0100)
        dos.writeShort(1)
        dos.writeShort(0)
        dos.writeShort(0)
        dos.writeShort(0)

        for (part in host.split(".")) {
            if (part.isEmpty()) continue
            val bytes = part.toByteArray(Charsets.US_ASCII)
            dos.writeByte(bytes.size)
            dos.write(bytes)
        }
        dos.writeByte(0)

        dos.writeShort(1)
        dos.writeShort(1)
        dos.flush()

        val queryData = baos.toByteArray()
        val socket = DatagramSocket()
        socket.soTimeout = 2000

        for (dnsServer in DNS_SERVERS) {
            try {
                val serverAddr = InetAddress.getByName(dnsServer)
                val packet = DatagramPacket(queryData, queryData.size, serverAddr, 53)
                socket.send(packet)

                val buffer = ByteArray(1024)
                val responsePacket = DatagramPacket(buffer, buffer.size)
                socket.receive(responsePacket)

                val result = parseDnsResponse(buffer, responsePacket.length, host, queryId)
                if (result.isNotEmpty()) {
                    socket.close()
                    return result
                }
            } catch (_: Exception) {}
        }
        socket.close()
        return emptyList()
    }

    private fun parseDnsResponse(data: ByteArray, length: Int, host: String, expectedId: Int): List<InetAddress> {
        if (length < 12) return emptyList()
        val buffer = ByteBuffer.wrap(data, 0, length)
        val id = buffer.short.toInt() and 0xFFFF
        if (id != expectedId) return emptyList()

        val flags = buffer.short.toInt() and 0xFFFF
        val rcode = flags and 0x000F
        if (rcode != 0) return emptyList()

        val qdCount = buffer.short.toInt() and 0xFFFF
        val anCount = buffer.short.toInt() and 0xFFFF
        buffer.short
        buffer.short

        if (anCount == 0) return emptyList()

        for (i in 0 until qdCount) {
            skipDomainName(buffer)
            buffer.short
            buffer.short
        }

        val addresses = mutableListOf<InetAddress>()
        for (i in 0 until anCount) {
            if (buffer.remaining() < 10) break
            skipDomainName(buffer)
            val type = buffer.short.toInt() and 0xFFFF
            buffer.short
            buffer.int
            val rdLength = buffer.short.toInt() and 0xFFFF

            if (type == 1 && rdLength == 4 && buffer.remaining() >= 4) {
                val ipBytes = ByteArray(4)
                buffer.get(ipBytes)
                addresses.add(InetAddress.getByAddress(host, ipBytes))
            } else {
                if (buffer.remaining() >= rdLength) {
                    buffer.position(buffer.position() + rdLength)
                } else {
                    break
                }
            }
        }
        return addresses
    }

    private fun skipDomainName(buffer: ByteBuffer) {
        while (buffer.hasRemaining()) {
            val len = buffer.get().toInt() and 0xFF
            if (len == 0) break
            if ((len and 0xC0) == 0xC0) {
                if (buffer.hasRemaining()) buffer.get()
                break
            } else {
                if (buffer.remaining() >= len) {
                    buffer.position(buffer.position() + len)
                } else {
                    break
                }
            }
        }
    }
}
