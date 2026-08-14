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
    name = "Sembunyikan Konten Disarankan",
    description = "Hides suggested stories, reels, threads across all feeds.",
) {
    runCatching {
        // UI fallback approach is the safest to cover Reels, Feed, and Threads across all languages
        XposedHelpers.findAndHookMethod(
            android.widget.TextView::class.java,
            "setText",
            CharSequence::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val text = (param.args[0] as? CharSequence)?.toString()?.trim() ?: return
                    if (!isSuggestedLabel(text)) return

                    val view = param.thisObject as? View ?: return
                    
                    // Don't hide tabs (e.g., "For you" tab in Threads/IG)
                    // Tabs are usually small, we only want to hide large feed items
                    
                    Handler(Looper.getMainLooper()).post {
                        runCatching { hideRecyclerItemContaining(view) }
                    }
                }
            }
        )
        XposedBridge.log("Rhpatch: [Suggested] Global TextView hook installed for Hide Content")
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
           lower == "karena anda menonton" ||
           lower == "because you liked" ||
           lower == "karena anda menyukai" ||
           lower == "suggested reels" ||
           lower == "reels disarankan" ||
           lower == "based on your activity" ||
           lower == "berdasarkan aktivitas anda" ||
           lower == "for you" ||
           lower == "untuk anda" ||
           lower == "explore" ||
           lower == "jelajahi" ||
           lower == "more posts" ||
           lower == "postingan lainnya"
}

private fun hideRecyclerItemContaining(view: View) {
    var current: View? = view
    var highestContainer: View? = null
    
    // Find the direct child of the RecyclerView
    repeat(15) {
        val parent = current?.parent as? ViewGroup
        if (parent != null) {
            val parentName = parent.javaClass.name
            // Hindari menyembunyikan Tab atau Header utama
            if (parentName.contains("TabLayout") || parentName.contains("TabBar")) return
            
            if (parentName.contains("RecyclerView") || parentName.contains("LithoView")) {
                highestContainer?.let { container ->
                    // Make it invisible and take up no space
                    val params = container.layoutParams
                    if (params != null) {
                        params.height = 0
                        params.width = 0
                        if (params is ViewGroup.MarginLayoutParams) {
                            params.setMargins(0, 0, 0, 0)
                        }
                        container.layoutParams = params
                    }
                    container.visibility = View.GONE
                    container.setPadding(0, 0, 0, 0)
                    XposedBridge.log("Rhpatch: [Suggested] Hid suggested item via UI Hook")
                }
                return
            }
            highestContainer = current
            current = parent
        }
    }
}
