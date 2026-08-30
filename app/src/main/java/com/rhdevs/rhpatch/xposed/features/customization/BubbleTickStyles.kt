package com.rhdevs.rhpatch.xposed.features.customization

import android.content.SharedPreferences
import android.content.res.Resources
import android.util.TypedValue
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
        
        XposedBridge.hookAllMethods(Resources::class.java, "getDrawable", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val res = param.thisObject as Resources
                val id = param.args[0] as Int
                try {
                    val name = res.getResourceEntryName(id)
                    if (bubbleStyle == "ios") {
                        if (name == "balloon_incoming_normal" || name == "balloon_incoming_normal_ext" || name == "msg_in") {
                            val customId = res.getIdentifier("wae_bubble_ios_in", "drawable", "com.rhdevs.rhpatch")
                            if (customId != 0) {
                                param.args[0] = customId
                            }
                        } else if (name == "balloon_outgoing_normal" || name == "balloon_outgoing_normal_ext" || name == "msg_out") {
                            val customId = res.getIdentifier("wae_bubble_ios_out", "drawable", "com.rhdevs.rhpatch")
                            if (customId != 0) {
                                param.args[0] = customId
                            }
                        }
                    }
                } catch (e: Exception) {}
            }
        })
        
        XposedBridge.hookAllMethods(Resources::class.java, "getDrawableForDensity", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val res = param.thisObject as Resources
                val id = param.args[0] as Int
                try {
                    val name = res.getResourceEntryName(id)
                    if (bubbleStyle == "ios") {
                        if (name == "balloon_incoming_normal" || name == "balloon_incoming_normal_ext" || name == "msg_in") {
                            val customId = res.getIdentifier("wae_bubble_ios_in", "drawable", "com.rhdevs.rhpatch")
                            if (customId != 0) {
                                param.args[0] = customId
                            }
                        } else if (name == "balloon_outgoing_normal" || name == "balloon_outgoing_normal_ext" || name == "msg_out") {
                            val customId = res.getIdentifier("wae_bubble_ios_out", "drawable", "com.rhdevs.rhpatch")
                            if (customId != 0) {
                                param.args[0] = customId
                            }
                        }
                    }
                } catch (e: Exception) {}
            }
        })
    }
}

