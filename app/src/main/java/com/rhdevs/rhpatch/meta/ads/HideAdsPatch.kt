package com.rhdevs.rhpatch.meta.ads

import android.os.Handler
import android.os.Looper
import android.view.View
import com.rhdevs.rhpatch.patch
import com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val HideAds = patch(
    name = "Hide Ads (Instagram)",
    description = "Block sponsored posts and stories using Rhpatch-style DexKit fingerprint"
) {

    runCatching {
        // Initialize DexKit for Instagram
        if (!MetaUnobfuscator.init(appContext)) {
            XposedBridge.log("Rhpatch: [Ads] Failed to initialize MetaUnobfuscator")
            return@runCatching
        }

        // Rhpatch Fingerprint for Disable Ads: "Is ad pod"
        val adMethods = MetaUnobfuscator.findMethodUsingStrings("Is ad pod")
        
        if (adMethods.isEmpty()) {
            XposedBridge.log("Rhpatch: [Ads] Could not find method containing 'Is ad pod'")
        } else {
            adMethods.forEach { method ->
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        // Immediately return true to signify 'is ad disabled' / 'is ad pod'
                        param.result = true
                    }
                })
            }
            XposedBridge.log("Rhpatch: [Ads] DexKit 'Is ad pod' hooks installed successfully on ${adMethods.size} methods")
        }
    }.onFailure {
        XposedBridge.log("Rhpatch: [Ads] DexKit hook failed: $it")
    }

    // ── Strategy 2: Hook TextView.setText for UI fallback (Retained as backup) ──
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
        XposedBridge.log("Rhpatch: [Ads] TextView.setText fallback hook installed")
    }.onFailure {
        XposedBridge.log("Rhpatch: [Ads] TextView.setText hook failed: $it")
    }
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

