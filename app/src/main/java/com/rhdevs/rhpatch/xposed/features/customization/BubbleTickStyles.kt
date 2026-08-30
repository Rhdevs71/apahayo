package com.rhdevs.rhpatch.xposed.features.customization

import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import com.rhdevs.rhpatch.xposed.core.Feature
import com.rhdevs.rhpatch.xposed.core.FeatureLoader
import com.rhdevs.rhpatch.xposed.core.devkit.Unobfuscator.loadBubbleDrawableMethod
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

class BubbleTickStyles(classLoader: ClassLoader, prefs: SharedPreferences) : Feature(classLoader, prefs) {
    override fun getPluginName(): String {
        return "Bubble & Tick Styles"
    }

    override fun doHook() {
        val bubbleStyle = prefs.getString("pref_bubble_style", "default")
        
        if (bubbleStyle == "default") return
        
        try {
            val bubbleDrawableMethod = loadBubbleDrawableMethod(classLoader)
            
            XposedBridge.hookMethod(bubbleDrawableMethod, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val position = param.args[0] as Int
                    val isOutgoing = (position == 3)
                    val suffix = if (isOutgoing) "out" else "in"
                    
                    val drawableName = "wae_bubble_${bubbleStyle}_${suffix}"
                    
                    val moduleContext = FeatureLoader.moduleContext
                    val moduleRes = moduleContext.resources
                    val customId = moduleRes.getIdentifier(drawableName, "drawable", moduleContext.packageName)
                    
                    if (customId != 0) {
                        @Suppress("DEPRECATION")
                        val customDrawable = moduleRes.getDrawable(customId)
                        if (customDrawable != null) {
                            // Replace the original WA bubble drawable with our custom shape
                            param.result = customDrawable
                        }
                    }
                }
            })
        } catch (e: Throwable) {
            XposedBridge.log("Failed to hook bubble method: " + e.message)
        }
    }
}
