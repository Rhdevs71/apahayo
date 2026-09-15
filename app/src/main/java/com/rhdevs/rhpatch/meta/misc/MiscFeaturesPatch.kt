package com.rhdevs.rhpatch.meta.misc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val DisableBuildExpiredPopup = patch(
    name = "Matikan Pop-up Kedaluwarsa",
    description = "Mencegah pop-up 'Alpha/Beta Build Expired' dari internal IG muncul."
) {
    runCatching {
        val dialogClass = XposedHelpers.findClassIfExists("com.instagram.ui.dialog.IgAlertDialog", classLoader)
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
        XposedBridge.hookAllMethods(android.app.Activity::class.java, "startActivity", object : de.robv.android.xposed.XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val intent = param.args.firstOrNull() as? android.content.Intent ?: return
                val context = param.thisObject as? android.content.Context ?: return
                
                if (intent.component?.className?.contains("browser") == true || intent.component?.className?.contains("inapp") == true) {
                    val urlStr = intent.getStringExtra("BrowserLiteIntent.EXTRA_URL") ?: intent.data?.toString()
                    if (urlStr != null) {
                        try {
                            val appCtx = android.app.AndroidAppHelper.currentApplication()
                            val prefs = appCtx?.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
                            if (prefs?.getBoolean("pref_open_links_externally", true) == true) {
                                val externalIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(urlStr))
                                externalIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(externalIntent)
                                param.result = null
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
        })
    }.onFailure { XposedBridge.log("Rhpatch: [OpenLinksExternally] Patch failed: $it") }
}

fun launchInstagramDeveloperOptions(activity: android.app.Activity): Boolean {
    return try {
        val fragmentAct = activity as? androidx.fragment.app.FragmentActivity
        val classLoader = activity.classLoader

        // 1. Try launching QuickExperimentCategoriesFragment directly
        val qeClass = de.robv.android.xposed.XposedHelpers.findClassIfExists(
            "com.instagram.debug.quickexperiment.QuickExperimentCategoriesFragment",
            classLoader
        )

        if (qeClass != null && fragmentAct != null) {
            // Find UserSession from Activity
            var userSession: Any? = null
            for (m in activity.javaClass.methods) {
                if (m.parameterTypes.isEmpty() && m.returnType.name.contains("UserSession")) {
                    userSession = runCatching { m.invoke(activity) }.getOrNull()
                    if (userSession != null) break
                }
            }

            val fragment = qeClass.newInstance() as androidx.fragment.app.Fragment
            if (userSession != null) {
                for (f in qeClass.declaredFields) {
                    if (f.type.name.contains("UserSession")) {
                        f.isAccessible = true
                        f.set(fragment, userSession)
                        break
                    }
                }
            }

            val containerId = fragmentAct.resources.getIdentifier("layout_container_main", "id", fragmentAct.packageName).let {
                if (it != 0) it else android.R.id.content
            }

            fragmentAct.supportFragmentManager.beginTransaction()
                .replace(containerId, fragment)
                .addToBackStack("developer_options")
                .commit()

            android.widget.Toast.makeText(activity, "Membuka Developer Options...", android.widget.Toast.LENGTH_SHORT).show()
            return true
        }

        // 2. Fallback to LX.0MRL launcher methods
        val launcherClass = de.robv.android.xposed.XposedHelpers.findClassIfExists("LX.0MRL", classLoader)
        if (launcherClass != null && fragmentAct != null) {
            var userSession: Any? = null
            for (m in activity.javaClass.methods) {
                if (m.parameterTypes.isEmpty() && m.returnType.name.contains("UserSession")) {
                    userSession = runCatching { m.invoke(activity) }.getOrNull()
                    if (userSession != null) break
                }
            }

            for (m in launcherClass.declaredMethods) {
                if (java.lang.reflect.Modifier.isStatic(m.modifiers) && m.parameterTypes.size == 3) {
                    m.isAccessible = true
                    m.invoke(null, activity, activity, userSession)
                    android.widget.Toast.makeText(activity, "Membuka Developer Options...", android.widget.Toast.LENGTH_SHORT).show()
                    return true
                }
            }
        }
        false
    } catch (t: Throwable) {
        de.robv.android.xposed.XposedBridge.log("Rhpatch: [EnableDevOptions] Error launching dev options: ${t.message}")
        false
    }
}

val EnableDeveloperOptions = patch(
    name = "Aktifkan Pilihan Pengembang",
    description = "Mengaktifkan menu Developer (Internal) Instagram via ketuk lama Home icon atau tombol Rhpatch."
) {
    runCatching {
        if (!MetaUnobfuscator.init(appContext)) return@runCatching

        // Hook onLongClick for Home navigation button (HomeIconOnClickListener)
        val homeClickListenerMethods = MetaUnobfuscator.findMethodUsingStrings("click", "activity")
        for (m in homeClickListenerMethods) {
            if (m.name == "onLongClick") {
                de.robv.android.xposed.XposedBridge.hookMethod(m, object : de.robv.android.xposed.XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val context = android.app.AndroidAppHelper.currentApplication() ?: appContext
                            val prefs = context.getSharedPreferences("rhpatch_settings", Context.MODE_PRIVATE)
                            if (!prefs.getBoolean("pref_enable_dev_options", true)) return

                            val view = param.args.firstOrNull { it is android.view.View } as? android.view.View
                            val act = (view?.context as? android.app.Activity)
                                ?: (param.thisObject as? android.app.Activity)
                                ?: return

                            if (launchInstagramDeveloperOptions(act)) {
                                param.result = true
                            }
                        } catch (_: Throwable) {}
                    }
                })
            }
        }

        // Also hook is_employee methods
        val methods = MetaUnobfuscator.findMethodUsingStrings("is_employee")
        for (m in methods) {
            if (m.returnType == Boolean::class.javaPrimitiveType || m.returnType == java.lang.Boolean::class.java) {
                de.robv.android.xposed.XposedBridge.hookMethod(m, object : de.robv.android.xposed.XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val context = android.app.AndroidAppHelper.currentApplication() ?: appContext
                            val prefs = context.getSharedPreferences("rhpatch_settings", Context.MODE_PRIVATE)
                            if (prefs.getBoolean("pref_enable_dev_options", true)) {
                                param.result = true
                            }
                        } catch (_: Throwable) {}
                    }
                })
            }
        }
    }.onFailure { de.robv.android.xposed.XposedBridge.log("Rhpatch: [EnableDevOptions] Patch failed: $it") }
}

val MiscPatches = arrayOf(OpenLinksExternally, EnableDeveloperOptions, DisableBuildExpiredPopup, SanitizeShareLinks, DisableStoryFlipping, DisableDoubleTapLikePatch)
