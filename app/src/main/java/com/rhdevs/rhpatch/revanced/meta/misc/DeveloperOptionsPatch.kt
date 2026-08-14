package com.rhdevs.rhpatch.revanced.meta.misc

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

val DeveloperOptionsPatch = patch(
    name = "Developer Options / Override Experiment",
    description = "Mengaktifkan menu Developer (tekan tahan tombol Home IG) dengan memalsukan status employee"
) {
    runCatching {
        if (!com.rhdevs.rhpatch.revanced.meta.devkit.MetaUnobfuscator.init(appContext)) return@runCatching

        val context = android.app.AndroidAppHelper.currentApplication()
        val prefs = context?.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
        val methodIndex = prefs?.getInt("pref_dev_method", 0) ?: 0
        val hookTracker = prefs?.getBoolean("pref_hook_tracker", false) ?: false

        val targetStrings = when (methodIndex) {
            1 -> listOf("developer_options", "quick_experiment", "employee_options") // Method B
            2 -> listOf("igds_dev_options", "is_developer", "developer_options_fragment") // Method C
            else -> listOf("is_employee", "is_developer", "employee_options") // Method A
        }
        
        var methods = emptyList<java.lang.reflect.Method>()
        for (str in targetStrings) {
            methods = com.rhdevs.rhpatch.revanced.meta.devkit.MetaUnobfuscator.findMethodUsingStrings(str)
            methods = methods.filter { it.returnType == Boolean::class.javaPrimitiveType || it.returnType == java.lang.Boolean::class.java }
            if (methods.isNotEmpty()) break
        }

        var hooked = false
        for (method in methods) {
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val ctx = android.app.AndroidAppHelper.currentApplication()
                        val pref = ctx?.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
                        if (pref?.getBoolean("pref_dev_options", true) == true) {
                            param.result = true
                            if (pref.getBoolean("pref_hook_tracker", false)) {
                                android.widget.Toast.makeText(ctx, "Rhpatch: Dev Mode Ter-hook! (Metode $methodIndex)", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {}
                }
            })
            hooked = true
            XposedBridge.log("Rhpatch: [DevOptions] Hooked employee checker (Method $methodIndex): ${method.declaringClass.name}.${method.name}")
        }
        
        if (!hooked) {
            XposedBridge.log("Rhpatch: [DevOptions] Failed to find employee checker (Method $methodIndex).")
            if (hookTracker) {
                android.widget.Toast.makeText(context, "Rhpatch: Gagal menemukan hook Dev Mode!", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }.onFailure { XposedBridge.log("Rhpatch: [DevOptions] Patch failed: $it") }
}
