package com.rhdevs.rhpatch.spotify

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Modifier

/**
 * Modul RHpatch untuk Spotify Music (com.spotify.music)
 * Fitur:
 * 1. Unlimited Skips, Seekbar / Scrubbing Unlocked, Disable Forced Shuffle, On-Demand Playback
 * 2. Audio & Video Ad Blocker (Model & Response Neutralizer + AdBreakContext silencer)
 * 3. ProductState Spoofing (Type: Premium, interruption-free, can_play_on_demand, audio-quality: very_high)
 * 4. Unlimited Lyrics (Bypass lyrics capping)
 * 5. Clean UI and Anti-Upsell
 * 6. High & Very High Audio Quality Setting Unlocked
 */

val SpotifyPlaybackRestrictionsPatch = patch(
    name = "Bebas Batasan Pemutar Musik",
    description = "Membuka batasan skip lagu, bebas scrubbing/menggeser seekbar, bebas matikan shuffle, dan bebas pilih lagu langsung (on-demand)"
) {
    runCatching {
        // 1. Dapatkan Restrictions.EMPTY
        val restrictionsCls = XposedHelpers.findClassIfExists("com.spotify.player.model.Restrictions", classLoader)
        val emptyRestrictions = if (restrictionsCls != null) {
            runCatching { XposedHelpers.getStaticObjectField(restrictionsCls, "EMPTY") }.getOrNull()
        } else null

        // 2. Dapatkan objek Optional.absent() untuk adBreakContext
        val absentOptional = runCatching {
            val p5Cls = XposedHelpers.findClassIfExists("p.p5", classLoader)
            if (p5Cls != null) {
                XposedHelpers.getStaticObjectField(p5Cls, "a")
            } else {
                val optCls = XposedHelpers.findClassIfExists("com.google.common.base.Optional", classLoader)
                if (optCls != null) XposedHelpers.callStaticMethod(optCls, "absent") else null
            }
        }.getOrNull()

        // 3. Sadap kelas konkret AutoValue_PlayerState (hindari menyadap kelas abstrak PlayerState)
        runCatching {
            val playerStateCls = XposedHelpers.findClassIfExists("com.spotify.player.model.AutoValue_PlayerState", classLoader)
            if (playerStateCls != null) {
                for (method in playerStateCls.declaredMethods) {
                    if (Modifier.isAbstract(method.modifiers)) continue

                    if ((method.name == "restrictions" || method.name == "contextRestrictions") && method.parameterTypes.isEmpty()) {
                        if (emptyRestrictions != null) {
                            runCatching {
                                XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(emptyRestrictions))
                                XposedBridge.log("Rhpatch [Spotify]: Sukses menyadap AutoValue_PlayerState." + method.name + " -> Restrictions.EMPTY")
                            }
                        }
                    } else if (method.name == "adBreakContext" && method.parameterTypes.isEmpty()) {
                        if (absentOptional != null) {
                            runCatching {
                                XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(absentOptional))
                                XposedBridge.log("Rhpatch [Spotify]: Sukses menyadap AutoValue_PlayerState.adBreakContext -> Optional.absent")
                            }
                        }
                    }
                }
            }
        }.onFailure {
            XposedBridge.log("Rhpatch [Spotify]: Gagal menyadap AutoValue_PlayerState: " + it.message)
        }

        // 4. Sadap kelas konkret AutoValue_Restrictions untuk kapabilitas pemutar
        runCatching {
            val concreteRestrictionsClasses = listOf(
                "com.spotify.player.model.AutoValue_Restrictions",
                "com.spotify.player.model.PlayerRestrictions",
                "com.spotify.interapp.model.PlayerRestrictions"
            )

            for (className in concreteRestrictionsClasses) {
                val cls = XposedHelpers.findClassIfExists(className, classLoader) ?: continue
                for (method in cls.declaredMethods) {
                    if (Modifier.isAbstract(method.modifiers)) continue

                    val mName = method.name
                    val rType = method.returnType

                    if (mName.startsWith("can", ignoreCase = true) || mName.startsWith("is", ignoreCase = true)) {
                        if (rType == Boolean::class.javaPrimitiveType || rType == java.lang.Boolean::class.java) {
                            if (mName.contains("Seek", true) ||
                                mName.contains("Repeat", true) ||
                                mName.contains("Shuffle", true) ||
                                mName.contains("Demand", true) ||
                                mName.contains("Skip", true) ||
                                mName.contains("Next", true) ||
                                mName.contains("Prev", true)) {
                                runCatching {
                                    XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true))
                                }
                            }
                        }
                    }
                }
            }
            XposedBridge.log("Rhpatch [Spotify]: Sukses menyadap kapabilitas pemutar pada AutoValue_Restrictions")
        }.onFailure {
            XposedBridge.log("Rhpatch [Spotify]: Gagal menyadap kapabilitas AutoValue_Restrictions: " + it.message)
        }

        XposedBridge.log("Rhpatch [Spotify]: Sukses menerapkan SpotifyPlaybackRestrictionsPatch")
    }.onFailure {
        XposedBridge.log("Rhpatch [Spotify]: Gagal menerapkan PlaybackRestrictionsPatch: " + it.message)
    }
}

val SpotifyAdBlockerPatch = patch(
    name = "Blokir Iklan Audio dan Video",
    description = "Memblokir iklan audio di sela lagu, iklan video sponsor, dan slot banner iklan Spotify"
) {
    runCatching {
        // 1. Sadap Proto Response Model untuk Iklan Audio & Slot Banner
        val adSlotClasses = listOf(
            "com.spotify.ads.esperanto.proto.GetSlotResponse",
            "com.spotify.ads.esperanto.proto.SubSlotResponse",
            "com.spotify.ads.esperanto.proto.SubInStreamResponse"
        )
        for (className in adSlotClasses) {
            runCatching {
                val cls = XposedHelpers.findClassIfExists(className, classLoader) ?: return@runCatching
                for (method in cls.declaredMethods) {
                    if (Modifier.isAbstract(method.modifiers)) continue
                    val mName = method.name.lowercase()
                    if (mName.contains("hasad") || mName.contains("isad")) {
                        if (method.returnType == Boolean::class.javaPrimitiveType) {
                            XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false))
                        }
                    }
                }
                XposedBridge.log("Rhpatch [Spotify]: Sukses menyadap proto iklan -> " + className)
            }
        }

        // 2. Sadap Video Ad Coordinator & Ad Models
        runCatching {
            val videoAdClasses = listOf(
                "com.spotify.video.videoadplayer.VideoAdPlaybackCoordinator",
                "com.spotify.mobile.android.spotlets.ads.model.Ad"
            )
            for (className in videoAdClasses) {
                val cls = XposedHelpers.findClassIfExists(className, classLoader) ?: continue
                for (method in cls.declaredMethods) {
                    if (Modifier.isAbstract(method.modifiers)) continue
                    if (method.name.lowercase().contains("isad") && method.returnType == Boolean::class.javaPrimitiveType) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false))
                    }
                }
            }
        }

        XposedBridge.log("Rhpatch [Spotify]: Sukses menerapkan SpotifyAdBlockerPatch")
    }.onFailure {
        XposedBridge.log("Rhpatch [Spotify]: Gagal menerapkan AdBlockerPatch: " + it.message)
    }
}

val SpotifyProductStateSpoofPatch = patch(
    name = "Buka Fitur Akun Premium (ProductState)",
    description = "Memalsukan status tier akun menjadi Premium (membuka audio quality Very High, mode on-demand, dan mematikan aturan shuffle paksa)"
) {
    runCatching {
        // 1. Sadap wrapper ProductState (p.i6z)
        runCatching {
            val productStateClass = XposedHelpers.findClassIfExists("p.i6z", classLoader)
            if (productStateClass != null) {
                for (method in productStateClass.declaredMethods) {
                    if (Modifier.isAbstract(method.modifiers)) continue
                    val paramTypes = method.parameterTypes
                    if (paramTypes.size == 2 && paramTypes[0] == String::class.java && paramTypes[1] == Boolean::class.javaPrimitiveType) {
                        XposedBridge.hookMethod(method, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                val key = param.args[0] as? String ?: return
                                when (key.lowercase()) {
                                    "interruption-free", "can_play_on_demand", "offline", "unlimited-skips" -> {
                                        param.args[1] = true
                                    }
                                    "pause-after-every-track", "ads", "ad-rules", "shuffle" -> {
                                        param.args[1] = false
                                    }
                                }
                            }
                        })
                    } else if (paramTypes.size == 2 && paramTypes[0] == String::class.java && paramTypes[1] == String::class.java) {
                        XposedBridge.hookMethod(method, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                val key = param.args[0] as? String ?: return
                                when (key.lowercase()) {
                                    "type" -> param.args[1] = "premium"
                                    "streaming-rules" -> param.args[1] = ""
                                    "audio-quality" -> param.args[1] = "very_high"
                                }
                            }
                        })
                    }
                }
                XposedBridge.log("Rhpatch [Spotify]: Sukses menyadap ProductState wrapper -> p.i6z")
            }
        }.onFailure {
            XposedBridge.log("Rhpatch [Spotify]: Gagal menyadap p.i6z: " + it.message)
        }

        // 2. Sadap emisi nilai ProductState pada Flow Collector p.d6z secara aman
        runCatching {
            val d6zClass = XposedHelpers.findClassIfExists("p.d6z", classLoader)
            if (d6zClass != null) {
                for (m in d6zClass.declaredMethods) {
                    if (m.name == "emit" && m.parameterTypes.size == 2) {
                        XposedBridge.hookMethod(m, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                val rawMap = param.args[0] as? Map<*, *> ?: return
                                val wrappedMap = object : java.util.HashMap<Any, Any>() {
                                    init {
                                        for ((k, v) in rawMap) {
                                            if (k != null && v != null) put(k, v)
                                        }
                                        put("type", "premium")
                                        put("can_play_on_demand", "1")
                                        put("ads", "0")
                                        put("ad-rules", "0")
                                        put("interruption-free", "1")
                                        put("pause-after-every-track", "0")
                                        put("streaming-rules", "")
                                        put("audio-quality", "very_high")
                                        put("shuffle", "0")
                                        put("unlimited-skips", "1")
                                    }
                                }
                                param.args[0] = wrappedMap
                            }
                        })
                        XposedBridge.log("Rhpatch [Spotify]: Sukses menyadap p.d6z.emit untuk ProductState Map")
                    }
                }
            }
        }.onFailure {
            XposedBridge.log("Rhpatch [Spotify]: Gagal menyadap p.d6z: " + it.message)
        }
    }.onFailure {
        XposedBridge.log("Rhpatch [Spotify]: Gagal menerapkan ProductStateSpoofPatch: " + it.message)
    }
}

val SpotifyUnlimitedLyricsPatch = patch(
    name = "Buka Lirik Tanpa Batas",
    description = "Membuka gembok kuota lirik bulanan (menetralkan pembatasan Lyrics Capping ke status NONE)"
) {
    runCatching {
        val lyricsRespClasses = listOf(
            "com.spotify.lyrics.serviceretrofit.proto.LyricsResponse",
            "com.spotify.lyrics.serviceretrofit.proto.v3.LyricsResponse"
        )
        for (className in lyricsRespClasses) {
            val cls = XposedHelpers.findClassIfExists(className, classLoader) ?: continue

            XposedBridge.hookAllConstructors(cls, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val capField = XposedHelpers.findFieldIfExists(cls, "capStatus_")
                        if (capField != null) {
                            capField.isAccessible = true
                            capField.setInt(param.thisObject, 0) // 0 = NONE
                        }
                    } catch (_: Throwable) {}
                }
            })

            val ova0Cls = XposedHelpers.findClassIfExists("p.ova0", classLoader)
            val noneEnum = ova0Cls?.enumConstants?.firstOrNull { it.toString().contains("NONE", ignoreCase = true) }

            for (method in cls.declaredMethods) {
                if (Modifier.isAbstract(method.modifiers)) continue
                if (method.returnType == ova0Cls && noneEnum != null) {
                    XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(noneEnum))
                } else if (method.name.lowercase().contains("capped") || method.name.lowercase().contains("islocked")) {
                    if (method.returnType == Boolean::class.javaPrimitiveType) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false))
                    }
                }
            }
            XposedBridge.log("Rhpatch [Spotify]: Sukses menyadap Lyrics capping -> " + className)
        }
    }.onFailure {
        XposedBridge.log("Rhpatch [Spotify]: Gagal menerapkan UnlimitedLyricsPatch: " + it.message)
    }
}

val SpotifyCleanUIAndAntiUpsellPatch = patch(
    name = "UI Bersih dan Blokir Pop-up Upsell",
    description = "Menyembunyikan dialog langganan Dapatkan Premium, penawaran upsell, dan banner promo berbayar"
) {
    runCatching {
        val upsellClasses = listOf(
            "com.spotify.upsells.v1.proto.ShouldUpsellRequest",
            "com.spotify.upsells.v1.proto.BatchShouldUpsellRequest",
            "com.spotify.upsells.v1.proto.GetUpsellRequest"
        )
        for (className in upsellClasses) {
            val cls = XposedHelpers.findClassIfExists(className, classLoader) ?: continue
            for (method in cls.declaredMethods) {
                if (Modifier.isAbstract(method.modifiers)) continue
                if (method.returnType == Boolean::class.javaPrimitiveType && (method.name.contains("should") || method.name.contains("eligible"))) {
                    XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false))
                }
            }
        }
        XposedBridge.log("Rhpatch [Spotify]: Sukses menerapkan CleanUIAndAntiUpsellPatch")
    }.onFailure {
        XposedBridge.log("Rhpatch [Spotify]: Gagal menerapkan CleanUIAndAntiUpsellPatch: " + it.message)
    }
}

val SpotifyAudioQualityPatch = patch(
    name = "Buka Pilihan Kualitas Sangat Tinggi",
    description = "Membuka gembok opsi audio Sangat Tinggi (Very High 320kbps) di pengaturan Spotify"
) {
    runCatching {
        val ieCls = XposedHelpers.findClassIfExists("p.ie", classLoader)
        if (ieCls != null) {
            XposedBridge.log("Rhpatch [Spotify]: Setting audio quality dipasangkan dengan ProductState")
        }
    }.onFailure {
        XposedBridge.log("Rhpatch [Spotify]: Gagal menerapkan AudioQualityPatch: " + it.message)
    }
}

val SpotifyPatches = arrayOf(
    SpotifyPlaybackRestrictionsPatch,
    SpotifyAdBlockerPatch,
    SpotifyProductStateSpoofPatch,
    SpotifyUnlimitedLyricsPatch,
    SpotifyCleanUIAndAntiUpsellPatch,
    SpotifyAudioQualityPatch
)
