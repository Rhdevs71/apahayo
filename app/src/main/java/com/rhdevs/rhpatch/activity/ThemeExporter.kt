package com.rhdevs.rhpatch.activity

import android.content.Context
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ThemeExporter {
    fun export(
        context: Context,
        toolbarRadius: Int,
        showCamera: Boolean,
        hideRead: Boolean,
        antiDelete: Boolean
    ) {
        try {
            // Build the CSS content
            val cssBuilder = StringBuilder()

            // Header properties (Overrides)
            cssBuilder.appendLine("/*")
            cssBuilder.appendLine("change_colors = true")
            cssBuilder.appendLine("rhpatch_hideread = $hideRead")
            cssBuilder.appendLine("rhpatch_antidelete = $antiDelete")
            cssBuilder.appendLine("*/")
            cssBuilder.appendLine()

            // UI Adjustments
            if (!showCamera) {
                cssBuilder.appendLine("#menuitem_camera {")
                cssBuilder.appendLine("    display: none;")
                cssBuilder.appendLine("}")
            }

            if (toolbarRadius > 0) {
                // Apply radius to known toolbar IDs
                cssBuilder.appendLine("#toolbar, #whatsapp_toolbar, #home_toolbar, #action_mode_bar {")
                cssBuilder.appendLine("    border-radius: ${toolbarRadius}px;")
                cssBuilder.appendLine("}")
            }

            val cssContent = cssBuilder.toString()

            // Zip the theme
            // Define the export path on external storage or private dir
            // For now, let's use the app's files directory
            val themesDir = File(context.getExternalFilesDir(null), "exported_themes")
            if (!themesDir.exists()) themesDir.mkdirs()

            val zipFile = File(themesDir, "studio_theme.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zout ->
                val entry = ZipEntry("style.css")
                zout.putNextEntry(entry)
                zout.write(cssContent.toByteArray())
                zout.closeEntry()
            }

            Toast.makeText(context, "Theme saved to: ${zipFile.absolutePath}", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
