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
    private val DNS_SERVERS = listOf("94.140.14.14", "1.1.1.1", "8.8.8.8")
    private val random = Random()

    fun hook(classLoader: ClassLoader, packageName: String, prefs: de.robv.android.xposed.XSharedPreferences) {
        try {
            prefs.reload()
            if (!prefs.getBoolean("dns_bypass_enabled", false)) return
            val whitelist = prefs.getString("dns_bypass_whitelist", "") ?: ""
            
            val allowedApps = whitelist.split(",").map { it.trim() }
            if (!allowedApps.contains(packageName)) return
            
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
                        } catch (e: Throwable) {
                            // Fallback to system default
                        }
                    }
                }
            )
            XposedBridge.log("Rhpatch: DNS AdGuard/Bypass Engine aktif untuk $packageName")
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch: DNS Bypass gagal dimuat - ${e.message}")
        }
    }

    private fun resolveDnsUdp(host: String): List<InetAddress> {
        val queryId = random.nextInt(65535)
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        // Header: ID, Flags (RD=1), QDCOUNT=1, ANCOUNT=0, NSCOUNT=0, ARCOUNT=0
        dos.writeShort(queryId)
        dos.writeShort(0x0100) // Standard query with recursion desired
        dos.writeShort(1)
        dos.writeShort(0)
        dos.writeShort(0)
        dos.writeShort(0)

        // QNAME
        for (part in host.split(".")) {
            if (part.isEmpty()) continue
            val bytes = part.toByteArray(Charsets.US_ASCII)
            dos.writeByte(bytes.size)
            dos.write(bytes)
        }
        dos.writeByte(0) // End of domain

        // QTYPE=A (1), QCLASS=IN (1)
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
            } catch (e: Exception) {
                // Try next DNS server
            }
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
        if (rcode != 0) return emptyList() // Error response

        val qdCount = buffer.short.toInt() and 0xFFFF
        val anCount = buffer.short.toInt() and 0xFFFF
        buffer.short // nsCount
        buffer.short // arCount

        if (anCount == 0) return emptyList()

        // Skip question section
        for (i in 0 until qdCount) {
            skipDomainName(buffer)
            buffer.short // qtype
            buffer.short // qclass
        }

        val addresses = mutableListOf<InetAddress>()
        for (i in 0 until anCount) {
            if (buffer.remaining() < 10) break
            skipDomainName(buffer)
            val type = buffer.short.toInt() and 0xFFFF
            buffer.short // class
            buffer.int   // ttl
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
                if (buffer.hasRemaining()) buffer.get() // Pointer, 1 additional byte
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
