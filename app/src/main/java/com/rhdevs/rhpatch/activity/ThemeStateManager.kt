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

    fun resetAll() {
        states.clear()
        wallpaperUri = null
        hideReadEnabled = false
        antiDeleteEnabled = false
    }

    fun applyPreset(presetName: String) {
        resetAll()
        when (presetName) {
            "ios" -> {
                getState("#toolbar").apply { bgColor = "#1C1C1E"; textColor = "#FFFFFF" }
                getState("#chat_toolbar").apply { bgColor = "#1C1C1E"; textColor = "#FFFFFF" }
                getState("#toolbar_logo").apply { textColor = "#FFFFFF" }
                getState("#main_layout").apply { bgColor = "#000000" }
                getState("#conversation_background").apply { bgColor = "#000000" }
                getState("#bottom_nav").apply { bgColor = "#1C1C1E"; textColor = "#8E8E93" }
                getState("#bottom_nav_indicator").apply { bgColor = "#007AFF"; radius = 16 }
                getState("#bubble_left").apply { bgColor = "#2C2C2E"; textColor = "#FFFFFF"; radius = 18 }
                getState("#bubble_right").apply { bgColor = "#007AFF"; textColor = "#FFFFFF"; radius = 18 }
                getState("#message_text").apply { textColor = "#FFFFFF" }
                getState("#date").apply { textColor = "#8E8E93" }
                getState("#name_in_group").apply { textColor = "#007AFF" }
                getState("#reply_bar_background").apply { bgColor = "#3A3A3C"; radius = 8 }
                getState("#entry").apply { bgColor = "#1C1C1E"; textColor = "#FFFFFF"; radius = 24 }
                getState("#send").apply { bgColor = "#007AFF"; iconTint = "#FFFFFF"; radius = 24 }
                getState("#fab").apply { bgColor = "#007AFF"; iconTint = "#FFFFFF" }
            }
            "amoled" -> {
                getState("#toolbar").apply { bgColor = "#000000"; textColor = "#FFFFFF" }
                getState("#chat_toolbar").apply { bgColor = "#000000"; textColor = "#FFFFFF" }
                getState("#main_layout").apply { bgColor = "#000000" }
                getState("#conversation_background").apply { bgColor = "#000000" }
                getState("#bottom_nav").apply { bgColor = "#000000"; textColor = "#8696A0" }
                getState("#bottom_nav_indicator").apply { bgColor = "#00A884"; radius = 16 }
                getState("#bubble_left").apply { bgColor = "#121212"; textColor = "#E0E0E0"; radius = 16 }
                getState("#bubble_right").apply { bgColor = "#005C4B"; textColor = "#FFFFFF"; radius = 16 }
                getState("#message_text").apply { textColor = "#FFFFFF" }
                getState("#date").apply { textColor = "#757575" }
                getState("#name_in_group").apply { textColor = "#00A884" }
                getState("#reply_bar_background").apply { bgColor = "#1E1E1E"; radius = 8 }
                getState("#entry").apply { bgColor = "#121212"; textColor = "#FFFFFF"; radius = 20 }
                getState("#send").apply { bgColor = "#00A884"; iconTint = "#FFFFFF"; radius = 24 }
                getState("#fab").apply { bgColor = "#00A884"; iconTint = "#000000" }
            }
            "cyberpunk" -> {
                getState("#toolbar").apply { bgColor = "#0D0221"; textColor = "#00F0FF" }
                getState("#chat_toolbar").apply { bgColor = "#0D0221"; textColor = "#00F0FF" }
                getState("#main_layout").apply { bgColor = "#050014" }
                getState("#conversation_background").apply { bgColor = "#050014" }
                getState("#bottom_nav").apply { bgColor = "#0D0221"; textColor = "#7122FA" }
                getState("#bottom_nav_indicator").apply { bgColor = "#00F0FF"; radius = 16 }
                getState("#bubble_left").apply { bgColor = "#1A0836"; textColor = "#00F0FF"; radius = 12 }
                getState("#bubble_right").apply { bgColor = "#7122FA"; textColor = "#FFFFFF"; radius = 12 }
                getState("#message_text").apply { textColor = "#E0F7FA" }
                getState("#date").apply { textColor = "#FF007F" }
                getState("#name_in_group").apply { textColor = "#00F0FF" }
                getState("#reply_bar_background").apply { bgColor = "#25004D"; radius = 8 }
                getState("#entry").apply { bgColor = "#1A0836"; textColor = "#00F0FF"; radius = 16 }
                getState("#send").apply { bgColor = "#FF007F"; iconTint = "#FFFFFF"; radius = 24 }
                getState("#fab").apply { bgColor = "#FF007F"; iconTint = "#FFFFFF" }
            }
            "emerald" -> {
                getState("#toolbar").apply { bgColor = "#1F2C34"; textColor = "#FFFFFF" }
                getState("#chat_toolbar").apply { bgColor = "#1F2C34"; textColor = "#FFFFFF" }
                getState("#main_layout").apply { bgColor = "#0B141A" }
                getState("#conversation_background").apply { bgColor = "#0B141A" }
                getState("#bottom_nav").apply { bgColor = "#1F2C34"; textColor = "#8696A0" }
                getState("#bottom_nav_indicator").apply { bgColor = "#00A884"; radius = 16 }
                getState("#bubble_left").apply { bgColor = "#202C33"; textColor = "#E9EDEF"; radius = 10 }
                getState("#bubble_right").apply { bgColor = "#005C4B"; textColor = "#E9EDEF"; radius = 10 }
                getState("#message_text").apply { textColor = "#E9EDEF" }
                getState("#date").apply { textColor = "#8696A0" }
                getState("#name_in_group").apply { textColor = "#25D366" }
                getState("#reply_bar_background").apply { bgColor = "#182229"; radius = 8 }
                getState("#entry").apply { bgColor = "#2A3942"; textColor = "#E9EDEF"; radius = 24 }
                getState("#send").apply { bgColor = "#00A884"; iconTint = "#FFFFFF"; radius = 24 }
                getState("#fab").apply { bgColor = "#00A884"; iconTint = "#0B141A" }
            }
            "pastel" -> {
                getState("#toolbar").apply { bgColor = "#FFDEE9"; textColor = "#4A4A4A" }
                getState("#chat_toolbar").apply { bgColor = "#FFDEE9"; textColor = "#4A4A4A" }
                getState("#main_layout").apply { bgColor = "#FFF5F5" }
                getState("#conversation_background").apply { bgColor = "#FFF5F5" }
                getState("#bottom_nav").apply { bgColor = "#FFDEE9"; textColor = "#757575" }
                getState("#bottom_nav_indicator").apply { bgColor = "#B5FFFC"; radius = 16 }
                getState("#bubble_left").apply { bgColor = "#FFFFFF"; textColor = "#4A4A4A"; radius = 18 }
                getState("#bubble_right").apply { bgColor = "#FFD1DC"; textColor = "#4A4A4A"; radius = 18 }
                getState("#message_text").apply { textColor = "#4A4A4A" }
                getState("#date").apply { textColor = "#999999" }
                getState("#name_in_group").apply { textColor = "#D87093" }
                getState("#reply_bar_background").apply { bgColor = "#F0E6EF"; radius = 8 }
                getState("#entry").apply { bgColor = "#FFFFFF"; textColor = "#4A4A4A"; radius = 24 }
                getState("#send").apply { bgColor = "#FFB6C1"; iconTint = "#FFFFFF"; radius = 24 }
                getState("#fab").apply { bgColor = "#FFB6C1"; iconTint = "#FFFFFF" }
            }
        }
    }
}

