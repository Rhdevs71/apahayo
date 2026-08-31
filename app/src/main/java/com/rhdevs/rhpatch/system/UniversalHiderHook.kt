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
        val hiderFile = File("/storage/emulated/0/Android/data/com.rhdevs.rhpatch/files/universal_hider.json")
        if (!hiderFile.exists()) return

        try {
            val jsonText = hiderFile.readText()
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

                        if (resolvedIds.isEmpty()) return

                        if (!hasHookedView) {
                            hasHookedView = true
                            XposedHelpers.findAndHookMethod(
                                View::class.java,
                                "onAttachedToWindow",
                                object : XC_MethodHook() {
                                    override fun afterHookedMethod(viewParam: MethodHookParam) {
                                        val view = viewParam.thisObject as View
                                        if (resolvedIds.contains(view.id)) {
                                            if (view.visibility != View.GONE) {
                                                view.visibility = View.GONE
                                            }
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
