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
    var targetPackageName: String? = null

    fun shouldHook(packageName: String): Boolean {
        if (!patchesByPackage.containsKey(packageName)) return false
        if (targetPackageName == null) targetPackageName = packageName
        return targetPackageName == packageName
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (!lpparam.isFirstApplication) return

        if (lpparam.packageName == "com.rhdevs.rhpatch.pro" || lpparam.packageName == "io.github.chsbuffer.revancedxposed") {
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
    val className = lpparam.appInfo.className ?: "android.app.Application"
    val appClazz = XposedHelpers.findClass(className, lpparam.classLoader)
    XposedBridge.hookMethod(appClazz.getMethod("onCreate"), object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            try {
                val app = param.thisObject as Application
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
    })
}
