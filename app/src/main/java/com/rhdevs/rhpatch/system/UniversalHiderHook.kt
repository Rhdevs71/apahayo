package com.rhdevs.rhpatch.system

import android.app.Application
import android.view.View
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import org.json.JSONObject
import java.io.File

object UniversalHiderHook {

    private var hasHookedView = false

    fun handleLoadPackage(lpparam: LoadPackageParam) {
        try {
            val prefs = de.robv.android.xposed.XSharedPreferences("com.rhdevs.rhpatch", "universal_hider")
            prefs.makeWorldReadable()
            val jsonText = prefs.getString("hider_data", "{}") ?: "{}"
            val json = JSONObject(jsonText)
            
            if (!json.has(lpparam.packageName)) return
            
            val arrayRules = json.getJSONArray(lpparam.packageName)
            val idsToHide = mutableSetOf<String>()
            for (i in 0 until arrayRules.length()) {
                idsToHide.add(arrayRules.getString(i))
            }
            
            if (idsToHide.isEmpty()) return

            XposedBridge.log("Rhpatch: UniversalHider active for " + lpparam.packageName + ". Targets: " + idsToHide.toString())

            XposedHelpers.findAndHookMethod(
                Application::class.java,
                "onCreate",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val app = param.thisObject as Application
                        if (app.packageName != lpparam.packageName) return
                        
                        val resolvedIds = mutableSetOf<Int>()
                        for (idName in idsToHide) {
                            val id = app.resources.getIdentifier(idName, "id", app.packageName)
                            if (id != 0) {
                                resolvedIds.add(id)
                            }
                        }

                        if (resolvedIds.isEmpty()) {
                            XposedBridge.log("Rhpatch: UniversalHider IDs not found in resources for " + lpparam.packageName)
                            return
                        }

                        if (!hasHookedView) {
                            hasHookedView = true
                            
                            // Hook 1: Ensure it's GONE when attached
                            XposedHelpers.findAndHookMethod(
                                View::class.java,
                                "onAttachedToWindow",
                                object : XC_MethodHook() {
                                    override fun afterHookedMethod(viewParam: MethodHookParam) {
                                        val view = viewParam.thisObject as View
                                        if (resolvedIds.contains(view.id)) {
                                            view.visibility = View.GONE
                                            view.layoutParams?.width = 0
                                            view.layoutParams?.height = 0
                                        }
                                    }
                                }
                            )
                            
                            // Hook 2: Prevent the app from making it visible again
                            XposedHelpers.findAndHookMethod(
                                View::class.java,
                                "setVisibility",
                                Int::class.javaPrimitiveType,
                                object : XC_MethodHook() {
                                    override fun beforeHookedMethod(viewParam: MethodHookParam) {
                                        val view = viewParam.thisObject as View
                                        if (resolvedIds.contains(view.id)) {
                                            viewParam.args[0] = View.GONE
                                        }
                                    }
                                }
                            )
                            
                            // Hook 3: Force dimensions to 0 to collapse the space
                            XposedHelpers.findAndHookMethod(
                                View::class.java,
                                "onMeasure",
                                Int::class.javaPrimitiveType,
                                Int::class.javaPrimitiveType,
                                object : XC_MethodHook() {
                                    override fun afterHookedMethod(viewParam: MethodHookParam) {
                                        val view = viewParam.thisObject as View
                                        if (resolvedIds.contains(view.id)) {
                                            XposedHelpers.callMethod(view, "setMeasuredDimension", 0, 0)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            )
            
        } catch (e: Exception) {
            XposedBridge.log("Rhpatch: Error in UniversalHiderHook for " + lpparam.packageName + " - " + e.message)
        }
    }
}
