package com.rhdevs.rhpatch.tiktok

import android.view.View
import android.view.ViewGroup
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

object TikTokInterfaceHook {
    fun apply(classLoader: ClassLoader, prefs: android.content.SharedPreferences) {
        
        val hideTako = prefs.getBoolean("tiktok_hide_tako_ai", false) || prefs.getBoolean("tiktok_hide_tako", false)
        val hideStem = prefs.getBoolean("tiktok_hide_top_stem", false)
        val hideToko = prefs.getBoolean("tiktok_hide_top_toko", false)
        val hideExplore = prefs.getBoolean("tiktok_hide_top_explore", false)
        val hideLive = prefs.getBoolean("tiktok_hide_top_live", false)
        val hideKomunitas = prefs.getBoolean("tiktok_hide_top_komunitas", false)
        val hideLokasi = prefs.getBoolean("tiktok_hide_top_lokasi", false)
        val hideFloating = prefs.getBoolean("tiktok_hide_homepage_coin", false)
        val hideCaptcha = prefs.getBoolean("tiktok_hide_captcha_popups", false)
        
        if (!hideTako && !hideStem && !hideToko && !hideExplore && !hideLive && !hideKomunitas && !hideLokasi && !hideFloating && !hideCaptcha) return
        
        // 1. Hook TakoAssem controller directly if hideTako is active
        if (hideTako) {
            val takoClassNames = listOf(
                "com.ss.android.ugc.aweme.feed.assem.tikbot.TakoAssem",
                "com.ss.android.ugc.aweme.feed.assem.tikbot.TakoFeedButtonAssem",
                "com.ss.android.ugc.aweme.feed.assem.tikbot.TakoFloatingAssem"
            )
            for (className in takoClassNames) {
                try {
                    val takoClass = classLoader.loadClass(className)
                    for (method in takoClass.declaredMethods) {
                        if (method.name == "onViewCreated" && method.parameterTypes.size == 1 && View::class.java.isAssignableFrom(method.parameterTypes[0])) {
                            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                                override fun afterHookedMethod(param: MethodHookParam) {
                                    try {
                                        val v = param.args[0] as? View ?: return
                                        v.visibility = View.GONE
                                        v.layoutParams?.let { lp ->
                                            lp.width = 0
                                            lp.height = 0
                                            v.layoutParams = lp
                                        }
                                    } catch (_: Throwable) {}
                                }
                            })
                        }
                        
                        // Force visibility parameters to false
                        if (method.parameterTypes.size == 1 && (method.parameterTypes[0] == java.lang.Boolean.TYPE || method.parameterTypes[0] == java.lang.Boolean::class.java)) {
                            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                                override fun beforeHookedMethod(param: MethodHookParam) {
                                    param.args[0] = false
                                }
                            })
                        }
                    }
                } catch (_: Throwable) {}
            }
        }

        try {
            // 2. Hook ViewGroup.addView to intercept and hide unwanted UI elements
            XposedBridge.hookAllMethods(ViewGroup::class.java, "addView", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val child = param.args[0] as? View ?: return
                        
                        var shouldHide = false
                        
                        // Check resource ID name if available
                        if (child.id != View.NO_ID) {
                            try {
                                val idName = child.context.resources.getResourceEntryName(child.id)?.lowercase() ?: ""
                                if (hideTako && (idName.contains("tako") || idName.contains("bot_feed_button") || idName.contains("ai_chat") || idName.contains("tikbot"))) {
                                    shouldHide = true
                                }
                                if (hideFloating && (idName.contains("promo") || idName.contains("coin") || idName.contains("timer_banner") || idName.contains("floating_badge"))) {
                                    shouldHide = true
                                }
                                if (hideCaptcha && (idName.contains("captcha") || idName.contains("puzzle"))) {
                                    shouldHide = true
                                }
                            } catch (_: Throwable) {}
                        }
                        
                        // Check content description, tag, and text
                        val contentDesc = child.contentDescription?.toString()?.lowercase() ?: ""
                        val tagStr = child.tag?.toString()?.lowercase() ?: ""
                        val textStr = (child as? android.widget.TextView)?.text?.toString()?.lowercase()?.trim() ?: ""
                        
                        // 1. Hide Tako AI
                        if (hideTako && (contentDesc.contains("tako") || contentDesc.contains("ai assistant") || contentDesc.contains("tikbot") || tagStr.contains("tako") || tagStr.contains("tikbot"))) {
                            shouldHide = true
                        }
                        
                        // 2. Hide Top Tabs
                        if (hideStem && (contentDesc.contains("stem") || tagStr.contains("stem") || textStr == "stem")) {
                            shouldHide = true
                        }
                        if (hideToko && (contentDesc.contains("shop") || contentDesc.contains("toko") || contentDesc.contains("mall") || tagStr.contains("shop") || textStr == "shop" || textStr == "toko" || textStr == "mall" || textStr == "belanja")) {
                            shouldHide = true
                        }
                        if (hideExplore && (contentDesc.contains("explore") || contentDesc.contains("jelajah") || tagStr.contains("explore") || textStr == "explore" || textStr == "jelajah")) {
                            shouldHide = true
                        }
                        if (hideLive && (contentDesc.contains("live") || tagStr.contains("live") || textStr == "live")) {
                            shouldHide = true
                        }
                        if (hideKomunitas && (contentDesc.contains("komunitas") || contentDesc.contains("community") || contentDesc.contains("forum") || contentDesc.contains("topik") || tagStr.contains("komunitas") || textStr == "komunitas" || textStr == "community" || textStr == "topik")) {
                            shouldHide = true
                        }
                        if (hideLokasi && (
                            contentDesc.contains("lokasi") || contentDesc.contains("nearby") || contentDesc.contains("sekitar") || contentDesc.contains("tempatan") || contentDesc.contains("local") || contentDesc.contains("daerah") ||
                            contentDesc.contains("jakarta") || contentDesc.contains("bekasi") || contentDesc.contains("bandung") || contentDesc.contains("surabaya") || contentDesc.contains("medan") || contentDesc.contains("depok") || contentDesc.contains("tangerang") || contentDesc.contains("bogor") || contentDesc.contains("semarang") || contentDesc.contains("makassar") || contentDesc.contains("palembang") || contentDesc.contains("bali") || contentDesc.contains("jogja") || contentDesc.contains("yogyakarta") ||
                            tagStr.contains("nearby") || tagStr.contains("location") || tagStr.contains("lokasi") ||
                            textStr.contains("di sekitar") || textStr.contains("sekitar") || textStr == "lokasi" || textStr == "nearby" || textStr == "tempatan" || textStr == "local" ||
                            textStr == "jakarta" || textStr == "bekasi" || textStr == "bandung" || textStr == "surabaya" || textStr == "medan" || textStr == "depok" || textStr == "tangerang" || textStr == "bogor" || textStr == "semarang" || textStr == "makassar" || textStr == "palembang" || textStr == "bali" || textStr == "jogja" || textStr == "yogyakarta"
                        )) {
                            shouldHide = true
                        }
                        
                        if (shouldHide) {
                            child.visibility = View.GONE
                            // Safely collapse dimensions without ClassCastException
                            child.layoutParams?.let { lp ->
                                lp.width = 0
                                lp.height = 0
                                child.layoutParams = lp
                            }
                        }
                    } catch (_: Throwable) {}
                }
            })
            XposedBridge.log("Rhpatch TikTok: Interface Hooks (Safe) applied successfully.")
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch TikTok Interface Error: " + e.message)
        }
    }
}
