package com.rhdevs.rhpatch.meta.privacy

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Method

object GhostModeState {
    var lastMarkSeenMethod: Method? = null
    var lastMarkSeenArgs: Array<Any?>? = null
    var lastMarkSeenInstance: Any? = null
    var forceMarkSeen = false
}

val GhostModePatch = patch(
    name = "Instagram Ghost Mode",
    description = "Sembunyikan status dilihat pada DM dan Story (Metode Rhpatch)"
) {
    runCatching {
        if (!com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.init(appContext)) return@runCatching

        val markThreadSeenMethods = com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.findMethodUsingStrings("mark_thread_seen-")

        if (markThreadSeenMethods.isNotEmpty()) {
            for (method in markThreadSeenMethods) {
                val isStatic = java.lang.reflect.Modifier.isStatic(method.modifiers)
                val isFinal = java.lang.reflect.Modifier.isFinal(method.modifiers)
                val isPublic = java.lang.reflect.Modifier.isPublic(method.modifiers)
                if (method.returnType == Void.TYPE && isStatic && isFinal && isPublic && method.parameterTypes.isNotEmpty()) {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            try {
                                if (GhostModeState.forceMarkSeen) {
                                    GhostModeState.forceMarkSeen = false
                                    return // Let it execute normally!
                                }

                                val context = android.app.AndroidAppHelper.currentApplication() ?: return
                                val prefs = context.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
                                if (prefs.getBoolean("pref_ghost_mode", true)) {
                                    val excludeChannels = prefs.getBoolean("pref_ghost_mode_channels_off", true)
                                    if (excludeChannels) {
                                        val threadArg = if (param.args.size > 1) param.args[1] else null
                                        if (threadArg != null) {
                                            var threadId: String? = null
                                            var isGroupFlag: Boolean? = null
                                            for (f in threadArg.javaClass.declaredFields) {
                                                try {
                                                    f.isAccessible = true
                                                    val v = f.get(threadArg)
                                                    if (v is Boolean) {
                                                        isGroupFlag = v
                                                    } else if (v is String && threadId == null) {
                                                        threadId = v
                                                    }
                                                } catch (_: Throwable) {}
                                            }
                                            // Non-1v1 chats (broadcast channels & groups) either have isGroupFlag=true OR threadId without a ":" (not user1:user2)
                                            val isChannelOrGroup = isGroupFlag == true || (threadId != null && !threadId.contains(":") && threadId.length > 5)
                                            if (isChannelOrGroup) {
                                                // Exclude broadcast channel / group from being blocked so membership & sync stay intact!
                                                return
                                            }
                                        }
                                    }

                                    // Save the attempt before blocking it
                                    GhostModeState.lastMarkSeenMethod = param.method as? Method
                                    GhostModeState.lastMarkSeenArgs = param.args
                                    GhostModeState.lastMarkSeenInstance = param.thisObject

                                    param.result = null
                                    
                                    if (prefs.getBoolean("pref_hook_tracker", false)) {
                                        android.widget.Toast.makeText(context, "Rhpatch: Ghost Mode DM Aktif!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {}
                        }
                    })
                    break
                }
            }
        }
    }.onFailure { XposedBridge.log("Rhpatch: [GhostMode] DMSeen hook failed: $it") }

    // Auto-confirm Join on Broadcast Channel Join Sheet (LX.0IFg / follow_to_join_chat_sheet)
    runCatching {
        val joinFragClass = XposedHelpers.findClassIfExists("LX.0IFg", classLoader)
        if (joinFragClass != null) {
            XposedBridge.hookAllMethods(joinFragClass, "onViewCreated", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val frag = param.thisObject ?: return
                        val joinBtn = runCatching { XposedHelpers.getObjectField(frag, "A0F") as? android.view.View }.getOrNull()
                        joinBtn?.post {
                            joinBtn.performClick()
                            XposedBridge.log("Rhpatch: [GhostMode] Auto-confirmed join on follow_to_join_chat_sheet")
                        }
                    } catch (_: Throwable) {}
                }
            })
        }
    }

    // Story Seen Hook (Layer 1)
    runCatching {
        val tigonClass = XposedHelpers.findClassIfExists("com.instagram.api.tigon.TigonServiceLayer", classLoader)
        if (tigonClass != null) {
            val startRequestMethods = tigonClass.declaredMethods.filter { it.name == "startRequest" }
            for (method in startRequestMethods) {
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            if (GhostModeState.forceMarkSeen) return
                            
                            val context = android.app.AndroidAppHelper.currentApplication()
                            val prefs = context?.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
                            if (prefs?.getBoolean("pref_ghost_mode", true) == true) {
                                val urlStr = extractUrlFromTigon(param.args)
                                if (urlStr != null) {
                                    if (urlStr.contains("/api/v2/media/seen/") || urlStr.contains("/api/v1/media/seen/")) {
                                        param.throwable = java.io.IOException("Rhpatch: Blocked Story Seen request")
                                    } else if (urlStr.contains("typing_status") || urlStr.contains("send_direct_typing")) {
                                        if (prefs.getBoolean("pref_disable_typing", true)) {
                                            param.throwable = java.io.IOException("Rhpatch: Blocked Typing Status request")
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {}
                    }
                    
                    private fun extractUrlFromTigon(args: Array<Any?>): String? {
                        for (arg in args) {
                            if (arg == null) continue
                            val str = arg.toString()
                            if (str.contains("media/seen") || str.contains("typing_status") || str.contains("unsend")) return str
                            try {
                                val fields = arg.javaClass.declaredFields
                                for (field in fields) {
                                    field.isAccessible = true
                                    val value = field.get(arg)
                                    if (value != null && value is java.net.URI) {
                                        return value.toString()
                                    }
                                    if (value != null && value is String && value.contains("media/seen")) {
                                        return value
                                    }
                                }
                            } catch (e: Exception) {}
                        }
                        return null
                    }
                })
            }
        }
    }.onFailure { XposedBridge.log("Rhpatch: [GhostMode] Tigon hook failed: $it") }

    // Story Seen Hook (Layer 2)
    runCatching {
        val methods = com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.findMethodUsingStrings("media/seen/?reel=%s&live_vod=0")
        val validMethods = methods.filter { it.returnType == Boolean::class.javaPrimitiveType || it.returnType == java.lang.Boolean::class.java }
        
        if (validMethods.isNotEmpty()) {
            val targetMethod = validMethods.last()
            if (!java.lang.reflect.Modifier.isAbstract(targetMethod.modifiers)) {
                XposedBridge.hookMethod(targetMethod, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            if (GhostModeState.forceMarkSeen) return
                            val context = android.app.AndroidAppHelper.currentApplication()
                            val prefs = context?.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
                            if (prefs?.getBoolean("pref_ghost_mode", true) == true) {
                                param.result = false 
                            }
                        } catch (e: Exception) {}
                    }
                })
            }
        }
    }.onFailure { XposedBridge.log("Rhpatch: [GhostMode] Story Seen hook failed: $it") }
}
