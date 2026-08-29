package com.rhdevs.rhpatch

import android.app.Application
import com.rhdevs.rhpatch.youtube.extension.shared.ResourceType
import com.rhdevs.rhpatch.youtube.extension.shared.ResourceUtils
import com.rhdevs.rhpatch.youtube.extension.shared.Utils
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import com.rhdevs.rhpatch.common.UpdateChecker
import com.rhdevs.rhpatch.youtube.ResourceFinder
import com.rhdevs.rhpatch.youtube.resourceMappings
import com.rhdevs.rhpatch.BuildConfig

class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {
    lateinit var startupParam: StartupParam
    lateinit var lpparam: LoadPackageParam
    lateinit var app: Application

    fun shouldHook(packageName: String): Boolean {
        return patchesByPackage.containsKey(packageName)
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        XposedBridge.log("Rhpatch: handleLoadPackage for ${lpparam.packageName}")
        try {
            ResourceUtils.fallbackPackageName = BuildConfig.APPLICATION_ID
            val prefs = XSharedPreferences(BuildConfig.APPLICATION_ID, "prefs")
            com.rhdevs.rhpatch.system.DnsBypassHook.hook(lpparam.classLoader, lpparam.packageName, prefs)
            
            // System Anti-Spam Hooks
            val smsPackages = listOf(
                "com.android.phone",
                "com.android.providers.telephony", // Lapis 0 (SmsProvider)
                "com.google.android.apps.messaging", // Google Messages (Lapis 2)
                "com.samsung.android.messaging", // Samsung Messages (Lapis 2)
                "com.android.mms", // Xiaomi/AOSP Messages (Lapis 2)
                "com.miui.smsextra", // MIUI Messages (Lapis 2)
                "com.transsion.smartmessage", // Transsion Messages (Lapis 2)
                "android" // Lapis 3 (NotificationManager di system_server)
            )
            
            if (smsPackages.contains(lpparam.packageName)) {
                com.rhdevs.rhpatch.system.SystemAntiSpamHook.hookSms(lpparam.classLoader, prefs)
            }
            
// Removed Call hook due to CallScreeningService migration
            
            // WhatsApp Hooks
            if (lpparam.packageName == "com.whatsapp" || lpparam.packageName == "com.whatsapp.w4b") {
                com.rhdevs.rhpatch.system.WaMessageBlockerHook.hook(lpparam.classLoader, prefs)
            }
            
            // TikTok Hooks
            val tiktokPackages = setOf(
                "com.zhiliaoapp.musically",
                "com.ss.android.ugc.trill",
                "com.ss.android.ugc.aweme",
                "com.zhiliaoapp.musically.go",
                "com.ss.android.ugc.trill.go"
            )
            if (tiktokPackages.contains(lpparam.packageName)) {
                com.rhdevs.rhpatch.tiktok.TikTokMainHook.handleLoadPackage(lpparam, prefs)
            }
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch: Failed to init System Hooks for ${lpparam.packageName}: ${e.message}")
        }

        if (lpparam.packageName == "com.rhdevs.rhpatch" || lpparam.packageName == "com.rhdevs.rhpatch" || lpparam.packageName == "com.rhdevs.rhpatch.pro" || lpparam.packageName == "io.github.chsbuffer.revancedxposed") {
            runCatching {
                val clazz = lpparam.classLoader.loadClass("com.rhdevs.rhpatch.activity.SettingsActivity")
                XposedHelpers.findAndHookMethod(
                    clazz,
                    "isModuleActive",
                    de.robv.android.xposed.XC_MethodReplacement.returnConstant(true)
                )
            }
        }

        if (!shouldHook(lpparam.packageName)) return
        this.lpparam = lpparam

        // Run Google Photos spoof immediately before Application context is even created
        if (lpparam.packageName == "com.google.android.apps.photos") {
            try {
                // Set Build fields early before they are read by app code
                XposedHelpers.setStaticObjectField(android.os.Build::class.java, "BRAND", "google")
                XposedHelpers.setStaticObjectField(android.os.Build::class.java, "MANUFACTURER", "Google")
                XposedHelpers.setStaticObjectField(android.os.Build::class.java, "MODEL", "Pixel XL")
                XposedHelpers.setStaticObjectField(android.os.Build::class.java, "DEVICE", "marlin")
                XposedHelpers.setStaticObjectField(android.os.Build::class.java, "PRODUCT", "marlin")
                XposedHelpers.setStaticObjectField(android.os.Build::class.java, "FINGERPRINT", "google/marlin/marlin:10/QP1A.191005.007.A3/5972272:user/release-keys")

                val systemPropertiesClass = XposedHelpers.findClass("android.os.SystemProperties", lpparam.classLoader)
                val getHook = object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as String
                        when (key) {
                            "ro.product.brand" -> param.result = "google"
                            "ro.product.manufacturer" -> param.result = "Google"
                            "ro.product.model" -> param.result = "Pixel XL"
                            "ro.product.device" -> param.result = "marlin"
                            "ro.product.name" -> param.result = "marlin"
                            "ro.build.fingerprint" -> param.result = "google/marlin/marlin:10/QP1A.191005.007.A3/5972272:user/release-keys"
                        }
                    }
                }
                XposedHelpers.findAndHookMethod(systemPropertiesClass, "get", String::class.java, getHook)
                XposedHelpers.findAndHookMethod(systemPropertiesClass, "get", String::class.java, String::class.java, getHook)
                
                XposedBridge.log("Rhpatch: Successfully spoofed SystemProperties and Build for Google Photos")
            } catch (e: Throwable) {
                XposedBridge.log("Rhpatch: Failed to spoof Google Photos SystemProperties/Build: ${e.message}")
            }
        }

        inContext(lpparam) { app ->
            this.app = app
            if (isReVancedPatched(lpparam)) {
                Utils.showToastLong("Rhpatch module does not work with patched app")
                return@inContext
            }

            resourceMappings = object : ResourceFinder {
                override operator fun get(type: String, name: String): Int {
                    val id = ResourceUtils.getIdentifier(ResourceType.fromValue(type), name)
                    if (id == 0) throw Exception("Could not find resource type: $type name: $name")
                    return id
                }
            }

            try {
                val patches = patchesByPackage[lpparam.packageName] ?: return@inContext
                PatchExecutor(app, lpparam).applyPatches(patches)
            } catch (e: Throwable) {
                XposedBridge.log("Rhpatch: Error executing PatchExecutor for ${lpparam.packageName}: ${e.stackTraceToString()}")
                android.widget.Toast.makeText(app, "Rhpatch Fatal Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun isReVancedPatched(lpparam: LoadPackageParam): Boolean {
        return runCatching {
            lpparam.classLoader.loadClass("app.revanced.integrations.shared.Utils")
        }.isSuccess || runCatching {
            lpparam.classLoader.loadClass("app.revanced.integrations.shared.utils.Utils")
        }.isSuccess
    }

    override fun initZygote(startupParam: StartupParam) {
        this.startupParam = startupParam
        XposedInit = startupParam

        runCatching {
            XposedHelpers.findAndHookMethod(
                System::class.java,
                "getProperty",
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (param.args[0] == "rhpatch.active") {
                            param.result = "true"
                        }
                    }
                }
            )
        }
    }
}

fun inContext(lpparam: LoadPackageParam, f: (Application) -> Unit) {
    XposedHelpers.findAndHookMethod(
        Application::class.java,
        "onCreate",
        object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                try {
                    val app = param.thisObject as Application
                    if (app.packageName != lpparam.packageName) return
                    Utils.setContext(app)

                    var isPatched = false
                    val executePatch = {
                        if (!isPatched) {
                            isPatched = true
                            f(app)
                        }
                    }

                    // Execute patches immediately on Application start
                    executePatch()

                    // Fallback on Activity onCreate
                    XposedHelpers.findAndHookMethod(
                        android.app.Activity::class.java,
                        "onCreate",
                        android.os.Bundle::class.java,
                        object : XC_MethodHook() {
                            override fun beforeHookedMethod(actParam: MethodHookParam) {
                                executePatch()
                            }
                        }
                    )

                    if (XposedInit.modulePath.startsWith("/data/app/")) {
                        val prefs = XSharedPreferences(BuildConfig.APPLICATION_ID, "prefs")
                        if (!prefs.file.canRead() || !prefs.getBoolean("disable_auto_check_update", false)) {
                            UpdateChecker().hookNewActivity()
                        }
                    }
                } catch (e: Throwable) {
                    XposedBridge.log("Rhpatch: Error inside inContext onCreate hook: " + e.message)
                }
            }
        }
    )
}
