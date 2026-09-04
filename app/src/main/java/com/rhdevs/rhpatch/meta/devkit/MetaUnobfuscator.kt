package com.rhdevs.rhpatch.meta.devkit

import android.app.Application
import de.robv.android.xposed.XposedBridge
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Method

object MetaUnobfuscator {
    private val bridges = mutableListOf<DexKitBridge>()
    private var isInitialized = false
    private var appClassLoader: ClassLoader? = null

    fun init(app: Application): Boolean {
        if (isInitialized) return true
        
        appClassLoader = app.classLoader

        try {
            System.loadLibrary("dexkit")
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch: MetaUnobfuscator System.loadLibrary('dexkit') failed: ${e.message}")
            return false
        }

        return try {
            val sourceDir = app.applicationInfo.sourceDir
            val b = DexKitBridge.create(sourceDir) ?: return false
            bridges.clear()
            bridges.add(b)
            isInitialized = true
            true
        } catch (e: Exception) {
            XposedBridge.log("Rhpatch: MetaUnobfuscator DexKitBridge.create failed: ${e.message}")
            false
        }
    }

    fun findMethodUsingStrings(vararg strings: String, returnType: String? = null): List<Method> {
        if (!isInitialized) return emptyList()
        val results = mutableListOf<org.luckypray.dexkit.result.MethodData>()
        
        for (bridge in bridges) {
            val res = bridge.findMethod {
                matcher {
                    for (string in strings) {
                        addUsingString(string)
                    }
                    if (returnType != null) {
                        returnType(returnType)
                    }
                }
            }
            results.addAll(res)
        }
        
        val loader = appClassLoader ?: Thread.currentThread().contextClassLoader ?: return emptyList()
        XposedBridge.log("Rhpatch: [MetaUnobfuscator] Found ${results.size} raw DexKit results for strings: ${strings.joinToString()}")
        
        return results.mapNotNull { methodData ->
            try {
                val method = methodData.getMethodInstance(loader)
                if (java.lang.reflect.Modifier.isAbstract(method.modifiers)) {
                    null
                } else {
                    method
                }
            } catch (e: Exception) {
                XposedBridge.log("Rhpatch: [MetaUnobfuscator] getMethodInstance error: ${e.message}")
                null
            }
        }
    }

    fun getFriendshipMapMethod(): Method? {
        if (!isInitialized) return null
        val results = mutableListOf<org.luckypray.dexkit.result.MethodData>()
        for (bridge in bridges) {
            val res = bridge.findMethod {
                matcher {
                    returnType("java.util.Map")
                    paramTypes("com.instagram.user.model.FriendshipStatus")
                    modifiers(java.lang.reflect.Modifier.STATIC or java.lang.reflect.Modifier.PUBLIC)
                }
            }
            results.addAll(res)
        }
        val loader = appClassLoader ?: Thread.currentThread().contextClassLoader ?: return null
        return results.firstOrNull()?.getMethodInstance(loader)
    }
}

