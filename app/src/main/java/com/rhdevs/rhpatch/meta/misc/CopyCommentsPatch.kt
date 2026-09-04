package com.rhdevs.rhpatch.meta.misc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.TextView
import android.widget.Toast
import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

val CopyCommentsPatch = patch(
    name = "Salin Komentar (Global)",
    description = "Ketuk lama pada komentar untuk menyalin (Memaksa bypass sistem UI IG)"
) {
    runCatching {
        // Hook performLongClick directly on all Views! 
        // This executes right when a long click happens, bypassing any swallowed listeners.
        XposedBridge.hookAllMethods(android.view.View::class.java, "performLongClick", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                try {
                    val view = param.thisObject as? TextView ?: return
                    // Check if it's an IgTextView or similar class
                    val className = view.javaClass.name
                    if (!className.contains("IgTextView") && !className.contains("BouncyNativeTextView")) return
                    
                    val text = view.text?.toString()?.trim()
                    if (text.isNullOrEmpty() || text.length < 3) return // Ignore small UI labels

                    val context = view.context ?: return
                    val prefs = context.getSharedPreferences("rhpatch_settings", Context.MODE_PRIVATE)
                    if (prefs.getBoolean("pref_copy_comments", false)) {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        if (cm != null) {
                            cm.setPrimaryClip(ClipData.newPlainText("IG Comment", text))
                            Toast.makeText(context, "Rhpatch: Teks Disalin!", Toast.LENGTH_SHORT).show()
                            // param.result = true // Optional: we can consume the event, but we probably want IG's native menu to show too (like Reply).
                        }
                    }
                } catch (e: Exception) {}
            }
        })
    }.onFailure { XposedBridge.log("Rhpatch: [CopyCommentsPatch] Patch failed: $it") }
}
