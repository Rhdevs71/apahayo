package com.rhdevs.rhpatch.revanced.meta.privacy

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

val EphemeralMediaPatch = patch(
    name = "Make Ephemeral Permanent",
    description = "Ubah pesan View Once menjadi permanen agar bisa dilihat berkali-kali"
) {
    runCatching {
        if (!com.rhdevs.rhpatch.revanced.meta.devkit.MetaUnobfuscator.init(appContext)) return@runCatching

        // Mencari metode JSON parser untuk Ephemeral Media
        val targetStrings = listOf("url_expire_at_secs", "view_mode", "seen_count", "tap_models")
        var methods = com.rhdevs.rhpatch.revanced.meta.devkit.MetaUnobfuscator.findMethodUsingStrings(*targetStrings.toTypedArray())
        
        // Filter metode yang namanya mengandung "parseFromJson" atau mengembalikan Object
        methods = methods.filter { method ->
            method.name.contains("parseFromJson", ignoreCase = true) || method.returnType != Void.TYPE
        }

        var hooked = false
        for (method in methods) {
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val context = android.app.AndroidAppHelper.currentApplication()
                        val prefs = context?.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
                        if (prefs?.getBoolean("pref_ephemeral", true) != true) return

                        val resultObj = param.result ?: return
                        
                        // Cara Xposed tanpa bytecode analysis:
                        // Cari semua field bertipe String di objek kembalian, jika isinya "once" atau "replayable", ubah jadi "permanent"
                        var modified = false
                        for (field in resultObj.javaClass.declaredFields) {
                            if (field.type == String::class.java) {
                                field.isAccessible = true
                                val value = field.get(resultObj) as? String
                                if (value == "once" || value == "replayable") {
                                    field.set(resultObj, "permanent")
                                    modified = true
                                }
                            }
                        }
                        
                        if (modified) {
                            XposedBridge.log("Rhpatch: [EphemeralMedia] Sukses mengubah media menjadi permanen!")
                        }
                    } catch (e: Exception) {
                        XposedBridge.log("Rhpatch: [EphemeralMedia] Error during field modification: $e")
                    }
                }
            })
            hooked = true
            XposedBridge.log("Rhpatch: [EphemeralMedia] Hooked parser: ${method.declaringClass.name}.${method.name}")
        }
        
        if (!hooked) {
            XposedBridge.log("Rhpatch: [EphemeralMedia] Failed to find EphemeralMedia parser method.")
        }
    }.onFailure { XposedBridge.log("Rhpatch: [EphemeralMedia] Patch failed: $it") }
}
