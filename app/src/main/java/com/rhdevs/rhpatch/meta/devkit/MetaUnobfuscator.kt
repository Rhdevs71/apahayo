package com.rhdevs.rhpatch.meta.devkit

import android.app.Application
import de.robv.android.xposed.XposedBridge
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Method

object MetaUnobfuscator {
    private val bridges = mutableListOf<DexKitBridge>()
    private var isInitialized = false

    fun init(app: Application): Boolean {
        if (isInitialized) return true
        
        try {
            System.loadLibrary("dexkit")
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch: MetaUnobfuscator System.loadLibrary('dexkit') failed: {e.message}")
            return false
        }

        return try {
            val paths = mutableListOf<String>()
            paths.add(app.applicationInfo.sourceDir)
            app.applicationInfo.splitSourceDirs?.let { paths.addAll(it) }
            
            for (p in paths) {
                val b = DexKitBridge.create(p)
                if (b != null) {
                    bridges.add(b)
                }
            }
            
            if (bridges.isNotEmpty()) {
                isInitialized = true
                true
            } else {
                false
            }
        } catch (e: Exception) {
            XposedBridge.log("Rhpatch: MetaUnobfuscator DexKitBridge.create failed: {e.message}")
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
        
        val loader = Thread.currentThread().contextClassLoader ?: return emptyList()
        return results.mapNotNull { methodData ->
            try {
                val method = methodData.getMethodInstance(loader)
                if (java.lang.reflect.Modifier.isAbstract(method.modifiers)) {
                    null
                } else {
                    method
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
