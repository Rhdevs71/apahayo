package com.rhdevs.rhpatch.revanced.meta.devkit

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
        return result.map { it.getMethodInstance(loader) }
    }
}
