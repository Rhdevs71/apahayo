package com.rhdevs.rhpatch.telegram

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Modifier

val TelegramMainPatch = patch(
    name = "Telegram Pro & Privacy Suite",
    description = "Anti-Hapus Pesan, Anti-Media Kadaluarsa (View Once), Bypass Larangan Salin/Teruskan/Screenshot, Blokir Iklan Sponsor, dan Buka Fitur Premium"
) {
    // 1. Bypass FLAG_SECURE (Izinkan Screenshot dan Screen Recording di semua chat / secret chat)
    runCatching {
        val windowClass = XposedHelpers.findClassIfExists("android.view.Window", classLoader)
        if (windowClass != null) {
            val clearFlagHook = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val flags = param.args[0] as? Int ?: return
                    val flagSecure = android.view.WindowManager.LayoutParams.FLAG_SECURE
                    if (flags and flagSecure != 0) {
                        param.args[0] = flags and flagSecure.inv()
                    }
                }
            }
            XposedBridge.hookAllMethods(windowClass, "setFlags", clearFlagHook)
            XposedBridge.hookAllMethods(windowClass, "addFlags", clearFlagHook)
        }
    }

    // 2. Blokir Iklan Sponsor (Sponsored Messages) di Channel & Search
    runCatching {
        val messagesControllerCls = XposedHelpers.findClassIfExists("org.telegram.messenger.MessagesController", classLoader)
        if (messagesControllerCls != null) {
            for (m in messagesControllerCls.declaredMethods) {
                val name = m.name.lowercase()
                if (name.contains("sponsored") || name.contains("promo")) {
                    if (m.returnType == java.util.ArrayList::class.java || m.returnType == java.util.List::class.java) {
                        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(ArrayList<Any>()))
                    } else if (m.returnType == Boolean::class.javaPrimitiveType) {
                        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(false))
                    } else if (!m.returnType.isPrimitive && m.returnType != Void.TYPE) {
                        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(null))
                    }
                }
            }
        }
    }

    // 3. Bypass Restriksi Salin, Teruskan, dan Simpan Media Terlindungi (NoForwards / Anti-Restricted)
    runCatching {
        val chatObjectCls = XposedHelpers.findClassIfExists("org.telegram.messenger.ChatObject", classLoader)
        if (chatObjectCls != null) {
            for (m in chatObjectCls.declaredMethods) {
                val name = m.name.lowercase()
                if (name.contains("hasnoforwards") || name.contains("ischannelandhasnoforwards") || name.contains("isrestricted")) {
                    XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(false))
                } else if (name.contains("cansavecontent") || name.contains("cancopy") || name.contains("canforward")) {
                    XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(true))
                }
            }
        }

        val messageObjectCls = XposedHelpers.findClassIfExists("org.telegram.messenger.MessageObject", classLoader)
        if (messageObjectCls != null) {
            for (m in messageObjectCls.declaredMethods) {
                val name = m.name.lowercase()
                if (name.contains("hasnoforwards") || name.contains("isrestricted")) {
                    XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(false))
                } else if (name.contains("canforward") || name.contains("cancopy") || name.contains("cansave")) {
                    XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(true))
                }
            }
        }
    }

    // 4. Anti-Media Kadaluarsa (Lihat Foto / Video Sekali Lihat Berkali-kali)
    runCatching {
        val messageObjectCls = XposedHelpers.findClassIfExists("org.telegram.messenger.MessageObject", classLoader)
        if (messageObjectCls != null) {
            for (m in messageObjectCls.declaredMethods) {
                val name = m.name.lowercase()
                if (name.contains("isexpired") || name.contains("issecretmediaexpired") || name.contains("isvoiceonceviewed") || name.contains("ismediaexpired")) {
                    XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(false))
                } else if (name.contains("getttl") || name.contains("getdestroystatustime")) {
                    XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(0))
                }
            }
        }
    }

    // 5. Anti-Hapus Pesan (Anti-Revoke Pesan Terhapus)
    runCatching {
        val messagesControllerCls = XposedHelpers.findClassIfExists("org.telegram.messenger.MessagesController", classLoader)
        if (messagesControllerCls != null) {
            for (m in messagesControllerCls.declaredMethods) {
                val name = m.name.lowercase()
                // Intercept delete message updates from network
                if (name == "processupdatearray" || name.contains("processupdates")) {
                    XposedBridge.hookMethod(m, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val list = param.args.firstOrNull { it is java.util.ArrayList<*> } as? java.util.ArrayList<*> ?: return
                            val iterator = list.iterator()
                            while (iterator.hasNext()) {
                                val item = iterator.next() ?: continue
                                val itemClassName = item.javaClass.simpleName
                                if (itemClassName.contains("DeleteMessages", ignoreCase = true) ||
                                    itemClassName.contains("DeleteChannelMessages", ignoreCase = true) ||
                                    itemClassName.contains("DeleteScheduledMessages", ignoreCase = true)) {
                                    // Remove deletion update so UI doesn't remove message
                                    iterator.remove()
                                }
                            }
                        }
                    })
                }
            }
        }

        val messagesStorageCls = XposedHelpers.findClassIfExists("org.telegram.messenger.MessagesStorage", classLoader)
        if (messagesStorageCls != null) {
            for (m in messagesStorageCls.declaredMethods) {
                val name = m.name.lowercase()
                if (name.contains("deletemessages") || name.contains("markmessagesasdeleted")) {
                    XposedBridge.hookMethod(m, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            // Don't purge messages from local database
                            param.result = null
                        }
                    })
                }
            }
        }
    }

    // 6. Buka Fitur Telegram Premium Client-Side & Download Boost
    runCatching {
        val userConfigCls = XposedHelpers.findClassIfExists("org.telegram.messenger.UserConfig", classLoader)
        if (userConfigCls != null) {
            for (m in userConfigCls.declaredMethods) {
                val name = m.name.lowercase()
                if (name == "ispremium" || name == "isclientpremium") {
                    XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(true))
                }
            }
        }

        val downloadControllerCls = XposedHelpers.findClassIfExists("org.telegram.messenger.DownloadController", classLoader)
        if (downloadControllerCls != null) {
            for (m in downloadControllerCls.declaredMethods) {
                val name = m.name.lowercase()
                if (name.contains("maxdownloadspeed") || name.contains("getdownloadspeedlimit")) {
                    XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(Int.MAX_VALUE))
                }
            }
        }
    }
}

val TelegramPatches = arrayOf(TelegramMainPatch)

