package com.rhdevs.rhpatch.tiktok

import android.content.Context
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

class TikTokDownloadsHook {

    companion object {
        fun init(classLoader: ClassLoader, prefs: android.content.SharedPreferences) {
            try {
                // 1. Hook Aweme class to prevent download restrictions
                val awemeClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.model.Aweme", classLoader)
                if (awemeClass != null) {
                    XposedBridge.hookAllMethods(awemeClass, "isPreventDownload", object : XC_MethodReplacement() {
                        override fun replaceHookedMethod(param: MethodHookParam): Any {
                            if (prefs.getBoolean("tiktok_force_download", false)) {
                                return false
                            }
                            return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                        }
                    })
                    
                    XposedBridge.hookAllMethods(awemeClass, "isProhibited", object : XC_MethodReplacement() {
                        override fun replaceHookedMethod(param: MethodHookParam): Any {
                            if (prefs.getBoolean("tiktok_force_download", false)) {
                                return false
                            }
                            return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                        }
                    })
                } else {
                    XposedBridge.log("Rhpatch TikTok: Aweme class NOT found")
                }

                // 2. Hook Aweme.getVideo() to swap watermark and no-watermark URLs like Morphe does
                if (awemeClass != null) {
                    XposedBridge.hookAllMethods(awemeClass, "getVideo", object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            if (!prefs.getBoolean("tiktok_download_watermark", false)) return
                            
                            try {
                                val video = param.result ?: return
                                // Get the playAddr (which is usually the watermark-free one)
                                val playAddr = XposedHelpers.getObjectField(video, "playAddr")
                                val h264PlayAddr = XposedHelpers.getObjectField(video, "h264PlayAddr")
                                
                                val fallback = playAddr ?: h264PlayAddr
                                if (fallback != null) {
                                    // Replace downloadNoWatermarkAddr with the playAddr model
                                    XposedHelpers.setObjectField(video, "downloadNoWatermarkAddr", fallback)
                                }
                            } catch (e: Throwable) {
                                XposedBridge.log("Rhpatch Video Hook Error: " + e.message)
                            }
                        }
                    })
                } else {
                    XposedBridge.log("Rhpatch TikTok: Aweme class NOT found for Video hook")
                }
                
                // 3. Fallback: Hook AwemeStatus to remove promotional music blocks
                val awemeStatusClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.model.AwemeStatus", classLoader)
                if (awemeStatusClass != null) {
                    XposedBridge.hookAllMethods(awemeStatusClass, "isWithPromotionalMusic", object : XC_MethodReplacement() {
                        override fun replaceHookedMethod(param: MethodHookParam): Any {
                            if (prefs.getBoolean("tiktok_force_download", false)) {
                                return false
                            }
                            return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                        }
                    })
                } else {
                    XposedBridge.log("Rhpatch TikTok: AwemeStatus class NOT found")
                }

                // 4. Hook ACLCommonShare to force the UI "Save Video" button to appear
                val aclCommonShareClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.model.ACLCommonShare", classLoader)
                if (aclCommonShareClass != null) {
                    // getCode() == 0 means allowed
                    XposedBridge.hookAllMethods(aclCommonShareClass, "getCode", object : XC_MethodReplacement() {
                        override fun replaceHookedMethod(param: MethodHookParam): Any {
                            if (prefs.getBoolean("tiktok_force_download", false)) {
                                return 0
                            }
                            return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                        }
                    })

                    // getShowType() == 2 means normal display in share menu
                    XposedBridge.hookAllMethods(aclCommonShareClass, "getShowType", object : XC_MethodReplacement() {
                        override fun replaceHookedMethod(param: MethodHookParam): Any {
                            if (prefs.getBoolean("tiktok_force_download", false)) {
                                return 2
                            }
                            return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                        }
                    })

                    // getTranscode() == 1 means download without watermark
                    XposedBridge.hookAllMethods(aclCommonShareClass, "getTranscode", object : XC_MethodReplacement() {
                        override fun replaceHookedMethod(param: MethodHookParam): Any {
                            if (prefs.getBoolean("tiktok_download_watermark", false)) {
                                return 1
                            }
                            return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                        }
                    })
                } else {
                    XposedBridge.log("Rhpatch TikTok: ACLCommonShare class NOT found")
                }

            } catch (e: Throwable) {
                XposedBridge.log("Rhpatch TikTok Downloads Hook Init Error: " + e.message)
            }
        }
    }
}

