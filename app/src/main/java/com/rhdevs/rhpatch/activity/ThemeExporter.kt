package com.rhdevs.rhpatch.activity

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.widget.Toast
import androidx.preference.PreferenceManager
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ThemeExporter {

    val CSS_MAPPING = mapOf(
        "#toolbar" to "#toolbar, #home_toolbar, #action_mode_bar, #toolbar_container, #app_bar_layout",
        "#chat_toolbar" to "#toolbar, #action_mode_bar, #toolbar_container",
        "#toolbar_logo" to "#toolbar_logo",
        "#menuitem_camera" to "#menuitem_camera",
        "#menuitem_search" to "#menuitem_search",
        "#menuitem_overflow" to "#menuitem_overflow",
        "#my_search_bar" to "#my_search_bar",
        "#chat_list" to "#chat_list, #conversations_list, #list, #conversation_list_view, #conversation_recycler_view",
        "#main_layout" to "#main_layout, #content, #root_view, #main_content",
        "#conversation_background" to "#conversation_background, #chat_wallpaper, #wallpaper, #conversation_wallpaper, #messages, #message_list, #messages_list, #conversation_list_view, #list, #recycler_view",
        "#bubble_left" to "#balloon_incoming_normal, #message_in, #bubble_left",
        "#bubble_right" to "#balloon_outgoing_normal, #message_out, #bubble_right",
        "#message_text" to "#message_text, #caption",
        "#date" to "#date, #date_wrapper",
        "#name_in_group" to "#name_in_group",
        "#reply_bar_background" to "#reply_bar_background",
        "#reactions_bubble_layout" to "#reactions_bubble_layout, #reactions_tray_layout",
        "#bottom_nav" to "#bottom_nav",
        "#bottom_nav_indicator" to "#navigation_bar_item_active_indicator_view",
        "#bottom_nav_item" to "#navigation_bar_item_icon_view, #navigation_bar_item_large_label_view, #navigation_bar_item_small_label_view, #navigation_bar_item_label_view",
        "#bottom_nav_divider" to "#bottom_nav_divider",
        "#entry" to "#entry",
        "#send" to "#send, #draft_send_v2",
        "#emoji_picker_btn" to "#emoji_picker_btn",
        "#input_attach_button" to "#input_attach_button",
        "#camera_btn" to "#camera_btn",
        "#voice_note_btn" to "#voice_note_btn, #voice_note_cancel_btn_v2, #voice_note_draft_stop_btn_v2",
        "#pin_indicator" to "#pin_indicator",
        "#mute_indicator" to "#mute_indicator",
        "#fab" to "#fab, #fab_second, #extended_mini_fab",
        "#edit_label" to "#edit_label",
        "#view_once_control_icon" to "#view_once_control_icon",
        "#fast_playback_overlay" to "#fast_playback_overlay",
        "#filter_chip" to "#filter_chip, #filter_tab, #chip, #chip_text, #chips_container TextView"
    )

    fun generateCss(): String {
        val cssBuilder = StringBuilder()

        // Mod Features Block
        if (ThemeStateManager.hideReadEnabled || ThemeStateManager.antiDeleteEnabled) {
            cssBuilder.append("@rhpatch_prefs {\n")
            if (ThemeStateManager.hideReadEnabled) cssBuilder.append("  hideread = true;\n")
            if (ThemeStateManager.antiDeleteEnabled) cssBuilder.append("  antidelete = true;\n")
            cssBuilder.append("}\n\n")
        }

        ThemeStateManager.states.forEach { (key, state) ->
            val realSelectors = CSS_MAPPING[key] ?: key
            cssBuilder.append(realSelectors).append(" {\n")

            if (state.isHidden) {
                cssBuilder.append("  display: none;\n")
            }
            if (state.bgColor != null) {
                cssBuilder.append("  background-color: ").append(state.bgColor).append(";\n")
            }
            if (state.textColor != null) {
                cssBuilder.append("  color: ").append(state.textColor).append(";\n")
            }
            if (state.iconTint != null) {
                cssBuilder.append("  color-tint: ").append(state.iconTint).append(";\n")
            }
            if (state.radius != null) {
                cssBuilder.append("  border-radius: ").append(state.radius).append("px;\n")
            }
            cssBuilder.append("}\n\n")
        }

        return cssBuilder.toString()
    }

    fun applyDirectlyToWhatsApp(context: Context): Boolean {
        return try {
            val generatedCss = generateCss()
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = prefs.edit()

            editor.putString("custom_css", generatedCss)
            editor.putBoolean("custom_filters", true)

            // Sinkronisasi warna gelembung bawaan jika disetel
            val rightColor = ThemeStateManager.states["#bubble_right"]?.bgColor
            val leftColor = ThemeStateManager.states["#bubble_left"]?.bgColor
            if (!rightColor.isNullOrEmpty() || !leftColor.isNullOrEmpty()) {
                editor.putBoolean("bubble_color", true)
                rightColor?.let {
                    try { editor.putInt("bubble_right", Color.parseColor(it)) } catch (_: Exception) {}
                }
                leftColor?.let {
                    try { editor.putInt("bubble_left", Color.parseColor(it)) } catch (_: Exception) {}
                }
            }

            // Sinkronisasi wallpaper jika ada
            ThemeStateManager.wallpaperUri?.let { uriStr ->
                editor.putBoolean("wallpaper", true)
                editor.putString("wallpaper_file", uriStr)
            }

            editor.apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun exportTheme(context: Context) {
        try {
            val themesDir = File(context.getExternalFilesDir(null), "themes")
            if (!themesDir.exists()) themesDir.mkdirs()

            val zipFile = File(themesDir, "studio_theme.zip")
            val zos = ZipOutputStream(FileOutputStream(zipFile))

            val cssBuilder = StringBuilder(generateCss())

            ThemeStateManager.wallpaperUri?.let { uriStr ->
                val uri = Uri.parse(uriStr)
                try {
                    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val imgEntry = ZipEntry("bg.png")
                        zos.putNextEntry(imgEntry)
                        inputStream.copyTo(zos)
                        zos.closeEntry()
                        inputStream.close()
                        
                        val bgSelectors = CSS_MAPPING["#conversation_background"] ?: "#conversation_background"
                        cssBuilder.append(bgSelectors).append(" {\n")
                        cssBuilder.append("  background-image: url(bg.png);\n")
                        cssBuilder.append("}\n\n")
                    }
                } catch (e: Exception) {}
            }

            val cssEntry = ZipEntry("style.css")
            zos.putNextEntry(cssEntry)
            zos.write(cssBuilder.toString().toByteArray())
            zos.closeEntry()
            zos.close()

            Toast.makeText(context, "Tema berhasil diekspor ke: ${zipFile.absolutePath}", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal mengekspor tema: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

