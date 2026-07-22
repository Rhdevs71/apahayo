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
        XposedBridge.log("Rhpatch: [Suggested] UI hooks installed successfully")
    }.onFailure {
        XposedBridge.log("Rhpatch: [Suggested] UI hook failed: $it")
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
