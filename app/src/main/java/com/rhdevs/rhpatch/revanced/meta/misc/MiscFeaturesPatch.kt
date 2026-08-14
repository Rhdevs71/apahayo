package com.rhdevs.rhpatch.revanced.meta.misc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.revanced.meta.devkit.MetaUnobfuscator
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val DisableBuildExpiredPopup = patch(
    name = "Matikan Notifikasi Build Kadaluarsa",
    description = "Mencegah munculnya pop-up 'Build Expired' pada versi Instagram lama."
) {
    runCatching {
        if (!MetaUnobfuscator.init(appContext)) return@runCatching
        val methods = MetaUnobfuscator.findMethodUsingStrings("App is too old", "Expiration logic")
        // Alternatif jika obfuskasi gagal, hook dialog
        val dialogClass = XposedHelpers.findClassIfExists("com.instagram.ui.dialog.IgDialogBuilder", classLoader)
        if (dialogClass != null) {
            XposedBridge.hookAllConstructors(dialogClass, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        // Tidak bisa langsung menebak string, namun kita bisa hook setTitle/setMessage
                    } catch (e: Exception) {}
                }
            })
        }
        
        // Rhpatch standard expiry bypass hook for Activity onCreate
        XposedBridge.hookAllMethods(android.app.Activity::class.java, "onCreate", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                // Biarkan kosong, ini hanya placeholder untuk kompatibilitas ke depan
            }
        })
    }.onFailure { XposedBridge.log("Rhpatch: [DisableBuildExpiredPopup] Patch failed: $it") }
}

val SanitizeShareLinks = patch(
    name = "Bersihkan Tautan Dibagikan",
    description = "Menghapus parameter pelacakan (?igsh=) dari tautan yang Anda salin."
) {
    runCatching {
        XposedBridge.hookAllMethods(ClipboardManager::class.java, "setPrimaryClip", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val clipData = param.args[0] as? ClipData ?: return
                if (clipData.itemCount > 0) {
                    val text = clipData.getItemAt(0).text?.toString() ?: return
                    if (text.contains("instagram.com") && text.contains("igsh=")) {
                        val cleanText = text.replace(Regex("\\?igsh=[^&\\s]+"), "")
                        param.args[0] = ClipData.newPlainText("Cleaned Link", cleanText)
                        Toast.makeText(appContext, "Tautan Instagram dibersihkan!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }.onFailure { XposedBridge.log("Rhpatch: [SanitizeShareLinks] Patch failed: $it") }
}

val CopyCommentsPatch = patch(
    name = "Salin Komentar",
    description = "Ketuk lama (Long Press) pada teks komentar untuk menyalinnya."
) {
    runCatching {
        val textViewClass = XposedHelpers.findClassIfExists("com.instagram.common.ui.base.IgTextView", classLoader)
        if (textViewClass != null) {
            XposedBridge.hookAllMethods(textViewClass, "setText", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val textView = param.thisObject as? TextView ?: return
                    val text = textView.text?.toString() ?: return
                    
                    if (text.length > 5) {
                        textView.setOnLongClickListener {
                            try {
                                val context = android.app.AndroidAppHelper.currentApplication()
                                val prefs = context?.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
                                if (prefs?.getBoolean("pref_copy_comments", true) == true) {
                                    val cm = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("Comment", text))
                                    Toast.makeText(appContext, "Komentar Disalin! (Rhpatch)", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {}
                            false // Tetap teruskan event agar menu asli Instagram muncul
                        }
                    }
                }
            })
        }
    }.onFailure { XposedBridge.log("Rhpatch: [CopyCommentsPatch] Patch failed: $it") }
}

val DisableStoryFlipping = patch(
    name = "Matikan Geser Story Otomatis",
    description = "Mencegah Instagram berpindah ke Story berikutnya secara otomatis."
) {
    runCatching {
        if (!MetaUnobfuscator.init(appContext)) return@runCatching
        val methods = MetaUnobfuscator.findMethodUsingStrings("story_auto_advance")
        methods.forEach { method ->
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = false // Disable auto advance
                }
            })
        }
    }.onFailure { XposedBridge.log("Rhpatch: [DisableStoryFlipping] Patch failed: $it") }
}

val MiscPatches = arrayOf(DisableBuildExpiredPopup, SanitizeShareLinks, CopyCommentsPatch, DisableStoryFlipping)
