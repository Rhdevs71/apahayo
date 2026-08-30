package com.rhdevs.rhpatch.xposed.features.customization

import android.content.SharedPreferences
import com.rhdevs.rhpatch.xposed.core.Feature
import de.robv.android.xposed.XposedBridge

class TickStyles(classLoader: ClassLoader, prefs: SharedPreferences) : Feature(classLoader, prefs) {
    override fun getPluginName(): String {
        return "Tick Styles"
    }

    override fun doHook() {
        val tickStyle = prefs.getString("pref_tick_style", "default")
        if (tickStyle != "default") {
            XposedBridge.log("Ticks are now replaced via XResources in WppXposed.kt")
        }
    }
}
