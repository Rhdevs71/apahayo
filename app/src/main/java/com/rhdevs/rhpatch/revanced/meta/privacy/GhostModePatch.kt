package com.rhdevs.rhpatch.revanced.meta.privacy

import com.rhdevs.rhpatch.morphe.AccessFlags
import com.rhdevs.rhpatch.morphe.findMethodDirect
import com.rhdevs.rhpatch.morphe.fingerprint
import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.hookMethod
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.net.URI

val DMSeenFingerprint = findMethodDirect(
    fingerprint {
        returns("V")
        strings("mark_thread_seen-")
        accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL)
    }
)

val GhostModePatch = patch(
    name = "Instagram Ghost Mode",
    description = "Sembunyikan status dilihat pada DM dan Story (Fitur Piko)"
) {
    // 1. DM Seen Hook (mark_thread_seen-) dengan Filter Saluran (Channel)
    runCatching {
        ::DMSeenFingerprint.hookMethod(object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                // Ambil semua argumen sebagai String untuk mendeteksi apakah ini Saluran (Broadcast Channel)
                val argStr = param.args?.joinToString(",") { it?.toString() ?: "null" } ?: ""
                
                // Broadcast channels biasanya memiliki metadata spesifik, atau thread_id yang berbeda.
                // Jika argumen mengandung kata kunci saluran, kita izinkan (return / tidak diblokir).
                if (argStr.contains("broadcast", ignoreCase = true) || argStr.contains("channel", ignoreCase = true)) {
                    XposedBridge.log("Rhpatch: [GhostMode] DM Seen diizinkan untuk Saluran (Bypass Bug): $argStr")
                    return
                }

                // Block read receipts for standard DMs
                XposedBridge.log("Rhpatch: [GhostMode] Blocked mark_thread_seen")
                param.result = null
            }
        })
        XposedBridge.log("Rhpatch: [GhostMode] DM Seen (mark_thread_seen) hook installed")
    }.onFailure { XposedBridge.log("Rhpatch: [GhostMode] DMSeen hook failed: $it") }

    // 2. Story Seen (Network Interception seperti Piko)
    runCatching {
        val tigonClass = XposedHelpers.findClassIfExists("com.instagram.api.tigon.TigonServiceLayer", classLoader)
        if (tigonClass != null) {
            tigonClass.declaredMethods.filter { it.name == "startRequest" }.forEach { method ->
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            // Cari objek URI atau String URL di dalam argumen
                            var uriString: String? = null
                            for (arg in param.args) {
                                if (arg == null) continue
                                
                                // Coba cari field bertipe java.net.URI atau String yang berisi URL
                                for (field in arg.javaClass.declaredFields) {
                                    field.isAccessible = true
                                    val value = field.get(arg)
                                    if (value is URI) {
                                        uriString = value.toString()
                                        break
                                    } else if (value is String && value.startsWith("http")) {
                                        uriString = value
                                        break
                                    }
                                }
                                if (uriString != null) break
                            }

                            if (uriString != null) {
                                if (uriString.contains("media/seen/?reel=")) {
                                    // Piko menggagalkan jaringan dengan IOException, 
                                    // di Xposed kita bisa langsung me-return null (jika fungsi V) atau exception
                                    XposedBridge.log("Rhpatch: [GhostMode] Network Intercepted Story Seen: $uriString")
                                    param.result = null // Batalkan request
                                } else if (uriString.contains("/heartbeat_and_get_viewer_count/")) {
                                    XposedBridge.log("Rhpatch: [GhostMode] Network Intercepted Live Seen: $uriString")
                                    param.result = null
                                }
                            }
                        } catch (e: Throwable) {
                            // Abaikan error refleksi
                        }
                    }
                })
            }
            XposedBridge.log("Rhpatch: [GhostMode] Tigon Network Interceptor for Story Seen installed")
        } else {
            XposedBridge.log("Rhpatch: [GhostMode] TigonServiceLayer not found")
        }
    }.onFailure { XposedBridge.log("Rhpatch: [GhostMode] Story Seen Network hook failed: $it") }
}
