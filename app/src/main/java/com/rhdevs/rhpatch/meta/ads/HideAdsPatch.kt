package com.rhdevs.rhpatch.meta.ads

import android.os.Handler
import android.os.Looper
import android.view.View
import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Modifier

val HideAds = patch(
    name = "Sembunyikan Iklan & Konten Disarankan",
    description = "Blokir postingan bersponsor, stories iklan, dan konten/akun yang disarankan di beranda."
) {
    runCatching {
        if (!MetaUnobfuscator.init(appContext)) {
            XposedBridge.log("Rhpatch: [Ads] Failed to initialize MetaUnobfuscator")
            return@runCatching
        }

        // 1. Sembunyikan Iklan Berbayar: "Is ad pod"
        val adMethods = MetaUnobfuscator.findMethodUsingStrings("Is ad pod")
        if (adMethods.isNotEmpty()) {
            adMethods.forEach { method ->
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = true
                    }
                })
            }
            XposedBridge.log("Rhpatch: [Ads] Hooked " + adMethods.size + " ad methods")
        }

        // 2. Sembunyikan Konten Disarankan (Suggested Users, Stories, Clips Netego, Channels)
        val suggestedMethods = MetaUnobfuscator.findMethodUsingStrings(
            "suggested_businesses",
            "clips_netego",
            "stories_netego",
            "in_feed_survey",
            "bloks_netego",
            "suggested_igd_channels",
            "suggested_top_accounts",
            "suggested_users"
        )
        val parseMethods = suggestedMethods.filter { it.name.lowercase().contains("parsefromjson") }
        var parserHooked = false

        for (parseMethod in parseMethods) {
            XposedBridge.hookMethod(parseMethod, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (parserHooked) return
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
                            XposedBridge.log("Rhpatch: [Ads] Successfully hooked IG Suggested Items Parser!")
                        } catch (e: Exception) {
                            XposedBridge.log("Rhpatch: [Ads] Failed to hook suggested parser: " + e.message)
                        }
                    }
                }
            })
        }
    }.onFailure {
        XposedBridge.log("Rhpatch: [Ads] DexKit hook failed: " + it)
    }

    // Fallback TextView text detector
    runCatching {
        XposedHelpers.findAndHookMethod(
            android.widget.TextView::class.java,
            "setText",
            CharSequence::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val text = (param.args[0] as? CharSequence)?.toString()?.trim() ?: return
                    if (!isSponsoredLabel(text)) return

                    val view = param.thisObject as? View ?: return
                    Handler(Looper.getMainLooper()).post {
                        runCatching { hideRecyclerItemContaining(view) }
                    }
                }
            }
        )
    }.onFailure {}
}

fun isSponsoredLabel(text: String): Boolean =
    text.equals("Bersponsor", ignoreCase = true) ||
    text.equals("Sponsored", ignoreCase = true) ||
    text.equals("Promoted", ignoreCase = true) ||
    text.equals("Patrocinado", ignoreCase = true) ||
    text.equals("SponsorisAc", ignoreCase = true) ||
    text.equals("Gesponsert", ignoreCase = true)

fun hideRecyclerItemContaining(child: View) {
    var current: View? = child
    var candidate: View? = null

    repeat(20) {
        val parent = current?.parent ?: return
        if (parent.javaClass.name.contains("RecyclerView", ignoreCase = true)) {
            candidate?.let { item ->
                item.visibility = View.GONE
                item.layoutParams?.let { lp ->
                    lp.height = 0
                    item.layoutParams = lp
                }
                XposedBridge.log("Rhpatch: [Ads] Collapsed sponsored feed item")
            }
            return
        }
        candidate = current
        current = parent as? View
    }
}
