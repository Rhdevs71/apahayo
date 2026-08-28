package com.rhdevs.rhpatch.meta.devkit

import android.app.Application
import de.robv.android.xposed.XposedBridge
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Method

object MetaUnobfuscator {
    private lateinit var bridge: DexKitBridge
    private var isInitialized = false

    fun init(app: Application): Boolean {
        if (isInitialized) return true
        
        try {
            System.loadLibrary("dexkit")
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch: MetaUnobfuscator System.loadLibrary('dexkit') failed: ${e.message}")
            return false
        }

        return try {
            val sourceDir = app.applicationInfo.sourceDir
            bridge = DexKitBridge.create(sourceDir) ?: return false
            isInitialized = true
            true
        } catch (e: Exception) {
            XposedBridge.log("Rhpatch: MetaUnobfuscator DexKitBridge.create failed: ${e.message}")
            false
        }
    }

    fun findMethodUsingStrings(vararg strings: String, returnType: String? = null): List<Method> {
        if (!isInitialized) return emptyList()
        val result = bridge.findMethod {
            matcher {
                for (string in strings) {
                    addUsingString(string)
                }
                if (returnType != null) {
                    returnType(returnType)
                }
            }
        }
        val loader = Thread.currentThread().contextClassLoader ?: return emptyList()
        return result.mapNotNull { methodData ->
            try {
                val method = methodData.getMethodInstance(loader)
                // Filter out abstract methods to prevent Xposed hook crashes
                if (java.lang.reflect.Modifier.isAbstract(method.modifiers)) {
                    null
                } else {
                    method
                }
            } catch (e: Exception) {
                // Ignore <clinit> or other methods that fail to resolve
                null
            }
        }
    }
}
