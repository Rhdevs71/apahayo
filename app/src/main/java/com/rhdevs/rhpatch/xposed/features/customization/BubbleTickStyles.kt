package com.rhdevs.rhpatch.xposed.features.customization

import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import com.rhdevs.rhpatch.xposed.core.Feature
import com.rhdevs.rhpatch.xposed.core.FeatureLoader
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

class BubbleTickStyles(classLoader: ClassLoader, prefs: SharedPreferences) : Feature(classLoader, prefs) {
    override fun getPluginName(): String {
        return "Bubble & Tick Styles"
    }

    override fun doHook() {
        val bubbleStyle = prefs.getString("pref_bubble_style", "default")
        
        if (bubbleStyle == "default") return
        
        val hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                // Menentukan ID dan Resources berdasarkan fungsi mana yang sedang dihook
                val id = if (param.method.name == "loadDrawable") {
                    param.args[2] as Int // loadDrawable(wrapper, value, id, density, theme)
                } else {
                    param.args[0] as Int // getDrawable(id)
                }
                
                val res = if (param.method.name == "loadDrawable") {
                    param.args[0] as Resources
                } else {
                    param.thisObject as Resources
                }

                try {
                    val name = res.getResourceEntryName(id)
                    val isIncoming = name == "balloon_incoming_normal" || name == "balloon_incoming_normal_ext" || name == "msg_in"
                    val isOutgoing = name == "balloon_outgoing_normal" || name == "balloon_outgoing_normal_ext" || name == "msg_out"
                    
                    if (isIncoming || isOutgoing) {
                        val suffix = if (isIncoming) "in" else "out"
                        val drawableName = "wae_bubble_${bubbleStyle}_${suffix}"
                        
                        // Mengambil resource langsung dari konteks modul kita, BUKAN dari WhatsApp
                        val moduleContext = FeatureLoader.moduleContext
                        val moduleRes = moduleContext.resources
                        val customId = moduleRes.getIdentifier(drawableName, "drawable", moduleContext.packageName)
                        
                        if (customId != 0) {
                            @Suppress("DEPRECATION")
                            val customDrawable = moduleRes.getDrawable(customId)
                            if (customDrawable != null) {
                                // Memotong eksekusi asli dan mengembalikan drawable kustom kita
                                param.result = customDrawable
                            }
                        }
                    }
                } catch (e: Exception) {}
            }
        }

        // Hook untuk pemanggilan via kode (Programmatic)
        XposedBridge.hookAllMethods(Resources::class.java, "getDrawable", hook)
        XposedBridge.hookAllMethods(Resources::class.java, "getDrawableForDensity", hook)
        
        // Hook khusus untuk proses inflation dari XML layout (Penting untuk versi Android baru)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                // ClassLoader yang digunakan adalah sistem (null/boot) karena ResourcesImpl adalah internal framework
                val resImplClass = XposedHelpers.findClass("android.content.res.ResourcesImpl", null)
                XposedBridge.hookAllMethods(resImplClass, "loadDrawable", hook)
            } catch (e: Throwable) {
                XposedBridge.log("Failed to hook ResourcesImpl: " + e.message)
            }
        }
    }
}
