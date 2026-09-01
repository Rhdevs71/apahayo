package com.rhdevs.rhpatch.meta.feed

import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Modifier

val HideSuggestedContent = patch(
    name = "Sembunyikan Konten Disarankan",
    description = "Hides suggested stories, reels, threads across all feeds.",
) {
    runCatching {
        if (!MetaUnobfuscator.init(appContext)) return@runCatching

        // Find FeedItem parseFromJson method which contains all these strings
        val methods = MetaUnobfuscator.findMethodUsingStrings(
            "suggested_businesses",
            "clips_netego",
            "stories_netego",
            "in_feed_survey",
            "bloks_netego",
            "suggested_igd_channels",
            "suggested_top_accounts",
            "suggested_users"
        )
        
        val parseMethods = methods.filter { it.name.lowercase().contains("parsefromjson") }
        
        var parserHooked = false

        for (parseMethod in parseMethods) {
            XposedBridge.hookMethod(parseMethod, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (parserHooked) return
                    
                    val prefs = de.robv.android.xposed.XSharedPreferences("com.rhdevs.rhpatch", "com.instagram.android")
                    prefs.makeWorldReadable()
                    if (!prefs.getBoolean("Sembunyikan Konten Disarankan", true)) return

                    val parserObj = param.args.firstOrNull() ?: return
                    val parserClass = parserObj.javaClass
                    
                    synchronized(this) {
                        if (parserHooked) return
                        parserHooked = true
                        
                        try {
                            var currentClass: Class<*>? = parserClass
                            while (currentClass != null && currentClass != Any::class.java) {
                                val stringMethods = currentClass.declaredMethods.filter { 
                                    it.returnType == String::class.java && 
                                    it.parameterTypes.isEmpty() && 
                                    !Modifier.isAbstract(it.modifiers)
                                }
                                
                                for (m in stringMethods) {
                                    XposedBridge.hookMethod(m, object : XC_MethodHook() {
                                        override fun afterHookedMethod(p: MethodHookParam) {
                                            val res = p.result as? String ?: return
                                            if (res == "suggested_users" || res == "clips_netego" || 
                                                res == "stories_netego" || res == "in_feed_survey" || 
                                                res == "bloks_netego" || res == "suggested_igd_channels" || 
                                                res == "suggested_top_accounts" || res == "suggested_businesses") {
                                                p.result = "rhpatch_ignored_suggested"
                                            }
                                        }
                                    })
                                }
                                currentClass = currentClass.superclass
                            }
                            XposedBridge.log("Rhpatch: [Suggested] Successfully hooked IG Streaming Parser!")
                        } catch (e: Exception) {
                            XposedBridge.log("Rhpatch: [Suggested] Failed to hook parser: e")
                        }
                    }
                }
            })
            XposedBridge.log("Rhpatch: [Suggested] Hooked parseFromJson: {parseMethod.declaringClass.name}.{parseMethod.name}")
        }

    }.onFailure {
        XposedBridge.log("Rhpatch: [Suggested] Patch failed: it")
    }
}
