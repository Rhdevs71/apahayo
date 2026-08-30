package com.rhdevs.rhpatch.xposed.features.customization

import android.content.SharedPreferences
import android.content.res.Resources
import com.rhdevs.rhpatch.xposed.core.Feature
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

class BubbleTickStyles(classLoader: ClassLoader, prefs: SharedPreferences) : Feature(classLoader, prefs) {
    override fun getPluginName(): String {
        return "Bubble & Tick Styles"
    }

    override fun doHook() {
        val bubbleStyle = prefs.getString("pref_bubble_style", "default")
        
        if (bubbleStyle == "default") return
        
        val hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val res = param.thisObject as Resources
                val id = param.args[0] as Int
                try {
                    val name = res.getResourceEntryName(id)
                    val isIncoming = name == "balloon_incoming_normal" || name == "balloon_incoming_normal_ext" || name == "msg_in"
                    val isOutgoing = name == "balloon_outgoing_normal" || name == "balloon_outgoing_normal_ext" || name == "msg_out"
                    
                    if (isIncoming || isOutgoing) {
                        val suffix = if (isIncoming) "in" else "out"
                        val drawableName = "wae_bubble_${bubbleStyle}_${suffix}"
                        val hostPackage = res.getResourcePackageName(id)
                        
                        val customId = res.getIdentifier(drawableName, "drawable", hostPackage)
                        if (customId != 0) {
                            param.args[0] = customId
                        }
                    }
                } catch (e: Exception) {}
            }
        }

        XposedBridge.hookAllMethods(Resources::class.java, "getDrawable", hook)
        XposedBridge.hookAllMethods(Resources::class.java, "getDrawableForDensity", hook)
    }
}

