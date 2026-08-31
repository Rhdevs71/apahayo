package com.rhdevs.rhpatch.activity

import android.content.Context
import android.net.Uri
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ThemeExporter {

    private val CSS_MAPPING = mapOf(
        "#toolbar" to "#toolbar, #home_toolbar, #action_mode_bar",
        "#chat_toolbar" to "#toolbar, #action_mode_bar",
        "#menuitem_camera" to "#menuitem_camera",
        "#menuitem_search" to "#menuitem_search",
        "#chat_list" to "#chat_list, #conversations_list, #list",
        "#main_layout" to "#main_layout, #content, #root_view",
        "#conversation_background" to "#conversation_background, #chat_wallpaper, #wallpaper, #conversation_wallpaper, #messages, #message_list, #messages_list, #conversation_list_view, #list, #recycler_view",
        "#bubble_left" to "#balloon_incoming_normal, #message_in",
        "#bubble_right" to "#balloon_outgoing_normal, #message_out",
        "#bottom_nav" to "#bottom_nav, #entry"
    )

    fun exportTheme(context: Context) {
        try {
            val themesDir = File(context.getExternalFilesDir(null), "themes")
            if (!themesDir.exists()) themesDir.mkdirs()

            val zipFile = File(themesDir, "studio_theme.zip")
            val zos = ZipOutputStream(FileOutputStream(zipFile))

            // Build CSS
            val cssBuilder = StringBuilder()

            ThemeStateManager.states.forEach { (key, state) ->
                val realSelectors = CSS_MAPPING[key] ?: key
                cssBuilder.append(realSelectors).append(" {\n")

                if (state.isHidden) {
                    cssBuilder.append("  display: none;\n")
                }
                if (state.bgColor != null) {
                    cssBuilder.append("  background-color: ").append(state.bgColor).append(";\n")
                }
                if (state.radius != null) {
                    cssBuilder.append("  border-radius: ").append(state.radius).append("px;\n")
                }
                cssBuilder.append("}\n\n")
            }

            // Handle Wallpaper
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
                        
                        // Inject background-image rule for chat backgrounds
                        val bgSelectors = CSS_MAPPING["#conversation_background"]
                        cssBuilder.append(bgSelectors).append(" {\n")
                        cssBuilder.append("  background-image: url(bg.png);\n")
                        cssBuilder.append("}\n\n")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Write style.css to zip
            val cssEntry = ZipEntry("style.css")
            zos.putNextEntry(cssEntry)
            zos.write(cssBuilder.toString().toByteArray())
            zos.closeEntry()

            zos.close()

            Toast.makeText(context, "Theme exported", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
        }
    }
}

