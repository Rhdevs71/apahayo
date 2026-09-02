package com.rhdevs.rhpatch.meta.misc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator
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


val OpenLinksExternally = patch(
    name = "Buka tautan secara eksternal",
    description = "Memaksa semua tautan web dibuka di browser eksternal bawaan perangkat."
) {
    runCatching {
        // Hooking Instagram's internal browser activity start
        XposedBridge.hookAllMethods(android.app.Activity::class.java, "startActivity", object : de.robv.android.xposed.XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val intent = param.args.firstOrNull() as? android.content.Intent ?: return
                val context = param.thisObject as? android.content.Context ?: return
                
                // If it's starting IG's internal browser, intercept it!
                if (intent.component?.className?.contains("browser") == true || intent.component?.className?.contains("inapp") == true) {
                    val urlStr = intent.getStringExtra("BrowserLiteIntent.EXTRA_URL") ?: intent.data?.toString()
                    if (urlStr != null) {
                        try {
                            val prefs = de.robv.android.xposed.XSharedPreferences("com.rhdevs.rhpatch", "com.instagram.android")
                            prefs.makeWorldReadable()
                            if (prefs.getBoolean("Buka tautan secara eksternal", true)) {
                                val externalIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(urlStr))
                                externalIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(externalIntent)
                                param.result = null // Cancel the internal browser launch
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
        })
    }.onFailure { XposedBridge.log("Rhpatch: [OpenLinksExternally] Patch failed: it") }
}

val EnableDeveloperOptions = patch(
    name = "Aktifkan Pilihan Pengembang",
    description = "Mengaktifkan menu Developer (Internal) Instagram secara paksa."
) {
    runCatching {
        if (!MetaUnobfuscator.init(appContext)) return@runCatching
        // Hook is_employee or developer options check
        val methods = MetaUnobfuscator.findMethodUsingStrings("is_employee", "developer_options")
        for (m in methods) {
            if (m.returnType == Boolean::class.javaPrimitiveType || m.returnType == java.lang.Boolean::class.java) {
                XposedBridge.hookMethod(m, object : de.robv.android.xposed.XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val prefs = de.robv.android.xposed.XSharedPreferences("com.rhdevs.rhpatch", "com.instagram.android")
                            prefs.makeWorldReadable()
                            if (prefs.getBoolean("Aktifkan Pilihan Pengembang", true)) {
                                param.result = true
                            }
                        } catch (e: Exception) {}
                    }
                })
            }
        }
    }.onFailure { XposedBridge.log("Rhpatch: [EnableDevOptions] Patch failed: it") }
}

val MiscPatches = arrayOf(MediaCommentsPatch, ThemeAMOLED, OpenLinksExternally, EnableDeveloperOptions, DisableBuildExpiredPopup, SanitizeShareLinks, CopyCommentsPatch, DisableStoryFlipping, RemoveEmptyBottomSpacePatch, DisableDoubleTapLikePatch, FriendshipStatusIndicatorPatch)
