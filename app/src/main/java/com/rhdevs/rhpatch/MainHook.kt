package com.rhdevs.rhpatch

import android.app.Application
import app.morphe.extension.shared.ResourceType
import app.morphe.extension.shared.ResourceUtils
import app.morphe.extension.shared.Utils
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import com.rhdevs.rhpatch.common.UpdateChecker
import com.rhdevs.rhpatch.morphe.ResourceFinder
import com.rhdevs.rhpatch.morphe.resourceMappings
import com.wmods.wppenhacer.BuildConfig

class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {
    lateinit var startupParam: StartupParam
    lateinit var lpparam: LoadPackageParam
    lateinit var app: Application

    fun shouldHook(packageName: String): Boolean {
        return patchesByPackage.containsKey(packageName)
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (!lpparam.isFirstApplication) return

        if (lpparam.packageName == "com.rhdevs.rhpatch" || lpparam.packageName == "com.wmods.wppenhacer" || lpparam.packageName == "com.rhdevs.rhpatch.pro" || lpparam.packageName == "io.github.chsbuffer.revancedxposed") {
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
                
                // Spoof system features to ensure Pixel features are unlocked
                XposedHelpers.findAndHookMethod(
                    "android.app.ApplicationPackageManager", 
                    lpparam.classLoader, 
                    "hasSystemFeature", 
                    String::class.java, 
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val feature = param.args[0] as? String ?: return
                            if (feature.startsWith("com.google.android.feature.PIXEL_")) {
                                param.result = true
                            }
                        }
                    }
                )
                
                XposedBridge.log("Rhpatch: Successfully spoofed SystemProperties, Build, and SystemFeatures for Google Photos")
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
                    f(app)
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
