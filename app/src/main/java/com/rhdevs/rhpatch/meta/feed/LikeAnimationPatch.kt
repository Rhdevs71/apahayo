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
        if (!com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.init(appContext)) return@runCatching

        val metadataClass = XposedHelpers.findClassIfExists("com.instagram.api.schemas.XDTUserActivationMetadataImpl", classLoader)
        if (metadataClass != null) {
            XposedBridge.hookAllConstructors(metadataClass, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val prefs = de.robv.android.xposed.XSharedPreferences("com.rhdevs.rhpatch", "com.instagram.android")
                        prefs.makeWorldReadable()
                        val likeTypeIndex = prefs.getInt("pref_like_animation_type", 0)
                        
                        val mappedType = when (likeTypeIndex) {
                            1 -> "RINGS_LIKE_ADRIAN"
                            2 -> "RINGS_LIKE_GABRIEL" // For PRIDE
                            3 -> "RINGS_LIKE_SEB"     // For SPARKLES
                            else -> return // DEFAULT
                        }
                        
                        if (param.args.isNotEmpty() && param.args[0] != null) {
                            val animEnumObj = param.args[0]
                            if (animEnumObj.javaClass.isEnum) {
                                val enumValues = animEnumObj.javaClass.enumConstants
                                val selectedEnum = enumValues?.find { it.toString() == mappedType }
                                if (selectedEnum != null) {
                                    param.args[0] = selectedEnum
                                }
                            }
                        }
                    } catch (e: Exception) {}
                }
            })
        }
    }.onFailure { XposedBridge.log("Rhpatch: [LikeAnimation] Patch failed: it") }
}
