package com.rhdevs.rhpatch.activity

object ThemeStateManager {
    data class ElementState(
        var isHidden: Boolean = false,
        var bgColor: String? = null,
        var textColor: String? = null,
        var radius: Int? = null,
        var iconTint: String? = null,
        var blur: Int? = null
    )

    val states = mutableMapOf<String, ElementState>()
    var wallpaperUri: String? = null
    
    // Theme Mod Features
    var hideReadEnabled: Boolean = false
    var antiDeleteEnabled: Boolean = false

    fun getState(id: String): ElementState {
        if (!states.containsKey(id)) {
            states[id] = ElementState()
        }
        return states[id]!!
    }
}
