package com.rhdevs.rhpatch.activity

object ThemeStateManager {
    data class ElementState(
        var isHidden: Boolean = false,
        var radius: Int? = null,
        var bgColor: String? = null,
        var iconTint: String? = null,
        var blur: Int? = null
    )

    val states = mutableMapOf<String, ElementState>()
    var wallpaperUri: String? = null
    var hideReadEnabled: Boolean = false
    var hideOnlineEnabled: Boolean = false
    var antiDeleteEnabled: Boolean = false

    fun getState(key: String): ElementState {
        return states.getOrPut(key) { ElementState() }
    }
}
