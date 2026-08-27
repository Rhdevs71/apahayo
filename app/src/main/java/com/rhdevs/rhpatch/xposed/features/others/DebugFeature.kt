package com.rhdevs.rhpatch.xposed.features.others

import com.rhdevs.rhpatch.xposed.core.Feature
import android.content.SharedPreferences 

class DebugFeature(classLoader: ClassLoader, preferences:SharedPreferences) :
    Feature(classLoader, preferences) {

    override fun doHook() {
    }


    override fun getPluginName(): String {
        return "Debug Feature"
    }
}
