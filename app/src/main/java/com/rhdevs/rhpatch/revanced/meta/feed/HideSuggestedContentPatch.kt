package com.rhdevs.rhpatch.revanced.meta.feed

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val HideSuggestedContent = patch(
    name = "Hide Suggested Content",
    description = "Hides suggested stories, reels, threads (Suggested posts will still be shown).",
) {
    runCatching {
        if (!com.rhdevs.rhpatch.revanced.meta.devkit.MetaUnobfuscator.init(appContext)) return@runCatching
        
        val methods = com.rhdevs.rhpatch.revanced.meta.devkit.MetaUnobfuscator.findMethodUsingStrings(
            "suggested_businesses",
            "clips_netego",
            "stories_netego",
            "in_feed_survey",
            "bloks_netego",
            "suggested_igd_channels",
            "suggested_top_accounts",
            "suggested_users"
        )
        
        // Filter methods to only those containing "parsefromjson" in name (case insensitive)
        val parseMethods = methods.filter { it.name.lowercase().contains("parsefromjson") }
        
        parseMethods.forEach { method ->
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    // We can't easily parse and modify the JSON here without knowing the exact object type,
                    // but we can make it return null for the parsed item if it contains these flags
                    // Or we can just let DexKit do its thing. Actually, Piko inserts a check.
                    // For a simpler Xposed approach without bytecode manipulation, 
                    // if this method parses a JSON into an object, and we just want to hide suggested content,
                    // we can't easily drop it from the array here.
                }
            })
        }
        
        // As a fallback and safer approach since we can't do bytecode injection easily here,
        // we'll keep the UI hook but also add the Piko JSON hook skeleton for future expansion.
        XposedHelpers.findAndHookMethod(
            android.widget.TextView::class.java,
            "setText",
            CharSequence::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val text = (param.args[0] as? CharSequence)?.toString()?.trim() ?: return
                    if (!isSuggestedLabel(text)) return

                    val view = param.thisObject as? View ?: return
                    Handler(Looper.getMainLooper()).post {
                        runCatching { hideRecyclerItemContaining(view) }
                    }
                }
            }
        )
        XposedBridge.log("Rhpatch: [Suggested] Hooks installed successfully")
    }.onFailure {
        XposedBridge.log("Rhpatch: [Suggested] Hook failed: $it")
    }
}

private fun isSuggestedLabel(text: String): Boolean {
    val lower = text.lowercase()
    return lower == "suggested for you" || 
           lower == "disarankan untuk anda" ||
           lower == "disarankan" ||
           lower == "suggested" ||
           lower == "because you watched" ||
           lower == "karena anda menonton"
}

private fun hideRecyclerItemContaining(view: View) {
    var current: View? = view
    var highestContainer: ViewGroup? = null
    
    // Find the item container inside RecyclerView
    repeat(10) {
        val parent = current?.parent as? ViewGroup
        if (parent != null) {
            val parentName = parent.javaClass.name
            if (parentName.contains("RecyclerView")) {
                highestContainer?.let { container ->
                    if (container.layoutParams.height != 0) {
                        container.layoutParams = container.layoutParams.apply {
                            height = 0
                            width = 0
                        }
                        container.visibility = View.GONE
                        container.setPadding(0, 0, 0, 0)
                        XposedBridge.log("Rhpatch: [Suggested] Hid suggested item")
                    }
                }
                return
            }
            highestContainer = parent
            current = parent
        }
    }
}
