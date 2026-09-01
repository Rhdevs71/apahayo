package com.rhdevs.rhpatch.meta.feed

import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val LikeAnimationPatch = patch(
    name = "Ubah Animasi Suka",
    description = "Mengganti animasi Like standar menjadi Rings atau gaya lain."
) {
    runCatching {
        val metadataClass = XposedHelpers.findClassIfExists("com.instagram.api.schemas.XDTUserActivationMetadataImpl", classLoader)
        if (metadataClass != null) {
            XposedBridge.hookAllConstructors(metadataClass, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val context = android.app.AndroidAppHelper.currentApplication()
                        val prefs = context?.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
                        val likeTypeIndex = prefs?.getInt("pref_like_animation_type", 0) ?: 0
                        val likeTypes = arrayOf("DEFAULT", "RINGS", "PRIDE", "SPARKLES")
                        val selectedType = likeTypes.getOrElse(likeTypeIndex) { "DEFAULT" }
                        
                        if (selectedType != "DEFAULT") {
                            val animEnum = param.args.firstOrNull()
                            if (animEnum != null && animEnum.javaClass.isEnum) {
                                val enumValues = animEnum.javaClass.enumConstants
                                val ringsEnum = enumValues?.find { it.toString().contains(selectedType, ignoreCase = true) }
                                if (ringsEnum != null) {
                                    param.args[0] = ringsEnum
                                }
                            }
                        }
                    } catch (e: Exception) {}
                }
            })
        }
    }.onFailure { XposedBridge.log("Rhpatch: [LikeAnimation] Patch failed: it") }
}
