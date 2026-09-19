package com.rhdevs.rhpatch.meta.privacy

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val MarkAsReadPatch = patch(
    name = "Tandai Obrolan Sebagai Sudah Dibaca",
    description = "Fitur tambahan untuk menandai obrolan sebagai terbaca saat Ghost Mode aktif"
) {
    runCatching {
        // Hook LayoutInflater.inflate to intercept direct_thread_header
        XposedBridge.hookAllMethods(android.view.LayoutInflater::class.java, "inflate", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val resourceId = param.args[0] as? Int ?: return
                    val context = (param.thisObject as? android.view.LayoutInflater)?.context ?: return
                    
                    val resourceName = try {
                        context.resources.getResourceEntryName(resourceId)
                    } catch (e: Exception) { null }

                    if (resourceName == "direct_thread_header") {
                        val viewGroup = param.result as? ViewGroup ?: return
                        
                        // Prevent duplicate injection
                        if (viewGroup.findViewWithTag<View>("rhpatch_mark_read") != null) return
                        
                        val prefs = context.getSharedPreferences("rhpatch_settings", Context.MODE_PRIVATE)
                        if (!prefs.getBoolean("pref_mark_as_read", false)) return

                        val dp = context.resources.displayMetrics.density
                        
                        val btn = TextView(context).apply {
                            tag = "rhpatch_mark_read"
                            text = "👀"
                            textSize = 20f
                            gravity = Gravity.CENTER
                            setPadding((8 * dp).toInt(), (4 * dp).toInt(), (8 * dp).toInt(), (4 * dp).toInt())
                            
                            setOnClickListener {
                                try {
                                    val method = GhostModeState.lastMarkSeenMethod
                                    val args = GhostModeState.lastMarkSeenArgs
                                    val instance = GhostModeState.lastMarkSeenInstance
                                    
                                    if (method != null && args != null) {
                                        GhostModeState.forceMarkSeen = true
                                        method.invoke(instance, *args)
                                        Toast.makeText(context, "Rhpatch: Obrolan ditandai sebagai telah dibaca!", Toast.LENGTH_SHORT).show()
                                        alpha = 0.3f // Dim it out to show it's read
                                    } else {
                                        Toast.makeText(context, "Rhpatch: Belum ada pesan baru untuk ditandai.", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    GhostModeState.forceMarkSeen = false
                                    Toast.makeText(context, "Rhpatch Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        // direct_thread_header usually has a horizontal LinearLayout containing the call buttons on the right.
                        // We iterate to find a suitable parent or just add it to the root if it's a LinearLayout/RelativeLayout.
                        if (viewGroup is LinearLayout && viewGroup.orientation == LinearLayout.HORIZONTAL) {
                            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                            lp.gravity = Gravity.CENTER_VERTICAL
                            viewGroup.addView(btn, viewGroup.childCount - 1, lp)
                        } else {
                            // Fallback: just add it to the viewgroup, hopefully it aligns well
                            viewGroup.addView(btn)
                        }
                    }
                } catch (e: Exception) {
                    XposedBridge.log("Rhpatch: MarkAsRead UI error: ${e.message}")
                }
            }
        })
    }.onFailure { XposedBridge.log("Rhpatch: [MarkAsRead] Patch failed: $it") }
}
