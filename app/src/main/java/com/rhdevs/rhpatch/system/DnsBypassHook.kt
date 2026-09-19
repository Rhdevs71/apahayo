package com.rhdevs.rhpatch.system

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.Random

object DnsBypassHook {
    private val DNS_SERVERS = listOf("8.8.8.8", "1.1.1.1", "208.67.222.222")
    private val random = Random()

    fun hook(classLoader: ClassLoader, packageName: String, prefs: de.robv.android.xposed.XSharedPreferences) {
        try {
            prefs.reload()
            if (!prefs.getBoolean("dns_bypass_enabled", false)) return
            val whitelist = prefs.getString("dns_bypass_whitelist", "") ?: ""
            
            val allowedApps = whitelist.split(",").map { it.trim() }
            if (!allowedApps.contains(packageName)) return
            
            // Hook 1: InetAddress
            XposedHelpers.findAndHookMethod(
                InetAddress::class.java,
                "getAllByName",
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val host = param.args[0] as? String ?: return
                        if (host.isEmpty() || host.matches(Regex("^[0-9.]+\$")) || host.contains(":")) return
                        
                        try {
                            val resolved = resolveDnsUdp(host)
                            if (resolved.isNotEmpty()) {
                                param.result = resolved.toTypedArray()
                            }
                        } catch (e: Throwable) {}
                    }
                }
            )

            // Hook 2: OkHttp
            try {
                val okhttpDnsClass = XposedHelpers.findClassIfExists("okhttp3.Dns", classLoader)
                if (okhttpDnsClass != null) {
                    var systemDnsClass = XposedHelpers.findClassIfExists("okhttp3.Dns\$Companion\$SYSTEM\$1", classLoader)
                    if (systemDnsClass == null) {
                        systemDnsClass = XposedHelpers.findClassIfExists("okhttp3.Dns\$1", classLoader)
                    }
                    if (systemDnsClass != null) {
                        XposedHelpers.findAndHookMethod(
                            systemDnsClass,
                            "lookup",
                            String::class.java,
                            object : XC_MethodHook() {
                                override fun beforeHookedMethod(param: MethodHookParam) {
                                    val host = param.args[0] as? String ?: return
                                    if (host.isEmpty() || host.matches(Regex("^[0-9.]+\$")) || host.contains(":")) return
                                    
                                    try {
                                        val resolved = resolveDnsUdp(host)
                                        if (resolved.isNotEmpty()) {
                                            param.result = resolved
                                        }
                                    } catch (e: Throwable) {}
                                }
                            }
                        )
                    }
                }
            } catch (e: Throwable) {}

            // Hook 3: DnsResolver
            try {
                val dnsResolverClass = XposedHelpers.findClassIfExists("android.net.DnsResolver", classLoader)
                if (dnsResolverClass != null) {
                    val networkClass = XposedHelpers.findClassIfExists("android.net.Network", classLoader)
                    val executorClass = java.util.concurrent.Executor::class.java
                    val cancellationSignalClass = XposedHelpers.findClassIfExists("android.os.CancellationSignal", classLoader)
                    val callbackClass = XposedHelpers.findClassIfExists("android.net.DnsResolver\$Callback", classLoader)
                    
                    if (networkClass != null && cancellationSignalClass != null && callbackClass != null) {
                        XposedHelpers.findAndHookMethod(
                            dnsResolverClass,
                            "query",
                            networkClass,
                            String::class.java,
                            Int::class.java,
                            executorClass,
                            cancellationSignalClass,
                            callbackClass,
                            object : XC_MethodHook() {
                                override fun beforeHookedMethod(param: MethodHookParam) {
                                    val host = param.args[1] as? String ?: return
                                    if (host.isEmpty() || host.matches(Regex("^[0-9.]+\$")) || host.contains(":")) return
                                    
                                    try {
                                        val resolved = resolveDnsUdp(host)
                                        if (resolved.isNotEmpty()) {
                                            val callback = param.args[5]
                                            if (callback != null) {
                                                val onAnswerMethod = callback.javaClass.getMethod("onAnswer", Any::class.java, Int::class.java)
                                                onAnswerMethod.invoke(callback, resolved, 0)
                                                param.result = null
                                            }
                                        }
                                    } catch (e: Throwable) {}
                                }
                            }
                        )
                    }
                }
            } catch (e: Throwable) {}
            
            XposedBridge.log("Rhpatch: DNS AdGuard/Bypass Engine aktif untuk " + packageName)
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch: DNS Bypass gagal dimuat - " + e.message)
        }
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
            } catch (e: Exception) {}
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