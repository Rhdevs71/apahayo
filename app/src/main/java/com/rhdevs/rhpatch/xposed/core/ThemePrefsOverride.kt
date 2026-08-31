package com.rhdevs.rhpatch.xposed.core

import android.content.Context
import android.content.SharedPreferences
import com.rhdevs.rhpatch.xposed.utils.Utils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.util.Properties

object ThemePrefsOverride {
    private val rhpatchOverrides = HashMap<String, Any>()
    private val waOverrides = HashMap<String, Any>()

    fun initAndWrap(classLoader: ClassLoader, pref: SharedPreferences): SharedPreferences {
        try {
            val properties = Utils.getProperties(pref, "custom_css", "custom_filters")
            for ((k, v) in properties) {
                val key = k.toString()
                val value = v.toString()
                if (key.startsWith("rhpatch_")) {
                    rhpatchOverrides[key.removePrefix("rhpatch_")] = parseValue(value)
                } else if (key.startsWith("wa_")) {
                    waOverrides[key.removePrefix("wa_")] = parseValue(value)
                }
            }

            if (waOverrides.isNotEmpty()) {
                hookSharedPreferencesWA(classLoader)
            }

            if (rhpatchOverrides.isNotEmpty()) {
                return wrapXSharedPreferences(classLoader, pref)
            }
        } catch (e: Exception) {
            XposedBridge.log("ThemePrefsOverride Error: ${e.message}")
        }
        return pref
    }

    private fun parseValue(value: String): Any {
        return when (value.lowercase()) {
            "true" -> true
            "false" -> false
            else -> {
                value.toIntOrNull() ?: value
            }
        }
    }

    private fun wrapXSharedPreferences(classLoader: ClassLoader, original: SharedPreferences): SharedPreferences {
        return java.lang.reflect.Proxy.newProxyInstance(
            classLoader,
            arrayOf(SharedPreferences::class.java)
        ) { _, method, args ->
            val methodName = method.name
            if (methodName.startsWith("get") && args != null && args.isNotEmpty()) {
                val key = args[0] as? String
                if (key != null && rhpatchOverrides.containsKey(key)) {
                    val overrideVal = rhpatchOverrides[key]
                    if (methodName == "getBoolean" && overrideVal is Boolean) return@newProxyInstance overrideVal
                    if (methodName == "getInt" && overrideVal is Int) return@newProxyInstance overrideVal
                    if (methodName == "getString" && overrideVal is String) return@newProxyInstance overrideVal
                }
            }
            method.invoke(original, *args.orEmpty())
        } as SharedPreferences
    }

    private fun hookSharedPreferencesWA(classLoader: ClassLoader) {
        val contextImplClass = XposedHelpers.findClass("android.app.ContextImpl", classLoader)
        XposedBridge.hookAllMethods(contextImplClass, "getSharedPreferences", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val name = param.args[0] as? String ?: return
                val originalPrefs = param.result as? SharedPreferences ?: return

                if (name == "com.whatsapp_preferences") {
                    val proxy = java.lang.reflect.Proxy.newProxyInstance(
                        classLoader,
                        arrayOf(SharedPreferences::class.java)
                    ) { _, method, args ->
                        val methodName = method.name
                        if (methodName.startsWith("get") && args != null && args.isNotEmpty()) {
                            val key = args[0] as? String
                            if (key != null && waOverrides.containsKey(key)) {
                                val overrideVal = waOverrides[key]
                                if (methodName == "getBoolean" && overrideVal is Boolean) return@newProxyInstance overrideVal
                                if (methodName == "getInt" && overrideVal is Int) return@newProxyInstance overrideVal
                                if (methodName == "getString" && overrideVal is String) return@newProxyInstance overrideVal
                            }
                        }
                        method.invoke(originalPrefs, *args.orEmpty())
                    }
                    param.result = proxy
                }
            }
        })
    }
}
