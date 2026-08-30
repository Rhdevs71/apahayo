package com.rhdevs.rhpatch.xposed.features.customization

import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import com.rhdevs.rhpatch.xposed.core.Feature
import com.rhdevs.rhpatch.xposed.core.FeatureLoader
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

class TickStyles(classLoader: ClassLoader, prefs: SharedPreferences) : Feature(classLoader, prefs) {
    override fun getPluginName(): String {
        return "Tick Styles"
    }

    override fun doHook() {
        val tickStyle = prefs.getString("pref_tick_style", "default")
        
        if (tickStyle != "default") {
            try {
                // Hook getDrawable to intercept when WhatsApp requests the tick icons
                val getDrawableHook = object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val res = param.thisObject as Resources
                        val id = param.args[0] as Int
                        try {
                            val name = res.getResourceEntryName(id)
                            var customName: String? = null
                            
                            if (name == "msg_status_server" || name == "msg_status_client_read_any") {
                                customName = "wae_tick_${tickStyle}_server"
                            } else if (name == "msg_status_client_delivered" || name == "msg_status_client_read_all") {
                                customName = "wae_tick_${tickStyle}_delivered"
                            } else if (name == "msg_status_client_read") {
                                customName = "wae_tick_${tickStyle}_read"
                            }
                            
                            if (customName != null) {
                                val moduleContext = FeatureLoader.moduleContext
                                val moduleRes = moduleContext.resources
                                val customId = moduleRes.getIdentifier(customName, "drawable", moduleContext.packageName)
                                
                                if (customId != 0) {
                                    @Suppress("DEPRECATION")
                                    val customDrawable = moduleRes.getDrawable(customId)
                                    if (customDrawable != null) {
                                        param.result = customDrawable
                                    }
                                }
                            }
                        } catch (e: Exception) {}
                    }
                }
                
                XposedHelpers.findAndHookMethod(Resources::class.java, "getDrawable", Int::class.javaPrimitiveType, getDrawableHook)
                XposedHelpers.findAndHookMethod(Resources::class.java, "getDrawable", Int::class.javaPrimitiveType, Resources.Theme::class.java, getDrawableHook)
                
            } catch (e: Throwable) {
                XposedBridge.log("Failed to hook getDrawable for ticks: " + e.message)
            }
        }
    }
}
