package com.rhdevs.rhpatch.revanced.meta.ads

import android.os.Handler
import android.os.Looper
import android.view.View
import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val HideAds = patch(
    name = "Hide Ads (Instagram)",
    description = "Block sponsored posts and stories at parser and UI level"
) {

    // ── Strategy 1: Instagram Custom JSON Parser Key Renamer ─────────────────
    runCatching {
        val parserClass = findParserClass(classLoader)
            ?: throw ClassNotFoundException("Could not find Instagram JSON parser class dynamically")

        val adKeys = setOf(
            "injected",
            "ad_metadata",
            "ad_tag",
            "android_links",
            "ad_action",
            "is_sponsored",
            "sponsored",
            "is_ad",
            "ad",
            "sponsored_label",
            "sponsored_label_text",
            "label_type",
            "is_paid_partnership",
            "paid_partnership",
            "commerciality_status",
            "is_in_feed_ad",
            "feed_ads"
        )

        // Find all methods returning String and taking 0 parameters (concrete or abstract!)
        val stringMethods = parserClass.methods.filter {
            it.returnType == String::class.java &&
            it.parameterCount == 0 &&
            !java.lang.reflect.Modifier.isAbstract(it.modifiers)
        }

        if (stringMethods.isEmpty()) {
            throw NoSuchMethodException("No String-returning methods found on parser class")
        }

        stringMethods.forEach { method ->
            try {
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val fieldName = param.result as? String ?: return
                        if (adKeys.contains(fieldName.lowercase())) {
                            param.result = fieldName + "_blocked"
                        }
                    }
                })
            } catch (e: IllegalArgumentException) {
                // Ignore abstract methods that slip through the filter
            } catch (e: Exception) {
                XposedBridge.log("Rhpatch: [Ads] Failed to hook method ${method.name}: ${e.message}")
            }
        }

        XposedBridge.log("Rhpatch: [Ads] Dynamic JSON parser hooks installed successfully on class ${parserClass.name} (found ${stringMethods.size} methods)")
    }.onFailure {
        XposedBridge.log("Rhpatch: [Ads] Custom JSON parser hook failed: $it")
    }

    // ── Strategy 2: Hook TextView.setText for UI fallback ────────────────────
    runCatching {
        XposedHelpers.findAndHookMethod(
            android.widget.TextView::class.java,
            "setText",
            CharSequence::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val text = (param.args[0] as? CharSequence)?.toString()?.trim()
                        ?: return
                    if (!isSponsoredLabel(text)) return

                    val view = param.thisObject as? View ?: return
                    Handler(Looper.getMainLooper()).post {
                        runCatching { hideRecyclerItemContaining(view) }
                    }
                }
            }
        )
        XposedBridge.log("Rhpatch: [Ads] TextView.setText hook installed")
    }.onFailure {
        XposedBridge.log("Rhpatch: [Ads] TextView.setText hook failed: $it")
    }
}

private fun findParserClass(classLoader: ClassLoader): Class<*>? {
    // 1. Try DirectThreadThemeInfo (New Instagram versions like v438)
    runCatching {
        val themeInfoClass = classLoader.loadClass("com.instagram.direct.model.DirectThreadThemeInfo")

        val method = themeInfoClass.declaredMethods.find {
            it.returnType == themeInfoClass &&
            it.parameterCount == 1 &&
            it.parameterTypes[0].name.startsWith("X.")
        }
        val paramType = method?.parameterTypes?.first()
        if (paramType != null) {
            XposedBridge.log("Rhpatch: Found parser class via DirectThreadThemeInfo: ${paramType.name}")
            return paramType
        }
    }

    // 2. Try Skywalker helper list
    val helperClassNames = listOf(
        "com.instagram.realtimeclient.SkywalkerCommand__JsonHelper",
        "com.instagram.realtimeclient.RealtimeStoreKey_ShimValueWithId__JsonHelper",
        "com.instagram.realtimeclient.requeststream.IgnoredData__JsonHelper",
        "com.instagram.realtimeclient.requeststream.String__JsonHelper",
        "com.instagram.realtimeclient.DirectApiError__JsonHelper"
    )
    for (name in helperClassNames) {
        runCatching {
            val clazz = classLoader.loadClass(name)
            val method = clazz.declaredMethods.find {
                it.name == "parseFromJson" &&
                it.parameterCount == 1 &&
                !it.parameterTypes[0].isPrimitive &&
                it.parameterTypes[0] != String::class.java
            }
            val paramType = method?.parameterTypes?.first()
            if (paramType != null) {
                XposedBridge.log("Rhpatch: Found parser class via Skywalker helper: ${paramType.name}")
                return paramType
            }
        }
    }
    return null
}

fun isSponsoredLabel(text: String): Boolean =
    text.equals("Bersponsor", ignoreCase = true) ||
    text.equals("Sponsored", ignoreCase = true) ||
    text.equals("Promoted", ignoreCase = true) ||
    text.equals("Patrocinado", ignoreCase = true) ||
    text.equals("Sponsorisé", ignoreCase = true) ||
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
