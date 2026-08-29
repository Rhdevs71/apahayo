package com.rhdevs.rhpatch.tiktok

import android.app.Activity
import android.graphics.Color
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.view.View
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XSharedPreferences
import android.content.Intent
import android.net.Uri

object TikTokMenuHook {
    fun apply(classLoader: ClassLoader, prefs: android.content.SharedPreferences) {
        try {
            XposedBridge.hookAllMethods(Activity::class.java, "onResume", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as Activity
                    val activityName = activity.javaClass.name
                    
                    // com.ss.android.ugc.aweme.setting.ui.SettingNewVersionActivity
                    if (activityName.contains("setting", ignoreCase = true) && activityName.contains("Activity")) {
                        try {
                            val decorView = activity.window.decorView as ViewGroup
                            val contentView = decorView.findViewById<ViewGroup>(android.R.id.content)
                            
                            if (contentView.findViewWithTag<View>("rhpatch_tiktok_btn") != null) return
                            
                            // Revert to Floating Button (as requested by user)
                            val fab = android.widget.ImageButton(activity).apply {
                                tag = "rhpatch_tiktok_btn"
                                layoutParams = android.widget.FrameLayout.LayoutParams(
                                    (56 * activity.resources.displayMetrics.density).toInt(),
                                    (56 * activity.resources.displayMetrics.density).toInt()
                                ).apply {
                                    gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
                                    bottomMargin = (80 * activity.resources.displayMetrics.density).toInt()
                                    marginEnd = (24 * activity.resources.displayMetrics.density).toInt()
                                }
                                setImageResource(android.R.drawable.ic_menu_manage) // Gear icon
                                setBackgroundColor(android.graphics.Color.parseColor("#FE2C55")) // TikTok Pink
                                
                                // Make it rounded
                                val shape = android.graphics.drawable.GradientDrawable()
                                shape.shape = android.graphics.drawable.GradientDrawable.OVAL
                                shape.setColor(android.graphics.Color.parseColor("#FE2C55"))
                                background = shape
                                
                                setPadding(30, 30, 30, 30)
                                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                                
                                setOnClickListener {
                                    val intent = Intent()
                                    intent.setClassName("com.rhdevs.rhpatch", "com.rhdevs.rhpatch.activity.TikTokSettingsActivity")
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    try {
                                        activity.startActivity(intent)
                                    } catch (e: Exception) {
                                        XposedBridge.log("Rhpatch TikTok: Failed to launch Settings - ${e.message}")
                                    }
                                }
                            }
                            
                            // Add floating button to content view
                            if (contentView is android.widget.FrameLayout) {
                                contentView.addView(fab)
                            } else {
                                decorView.addView(fab)
                            }
                            
                            XposedBridge.log("Rhpatch TikTok: Injected floating button into $activityName")
                        } catch (e: Throwable) {
                            XposedBridge.log("Rhpatch TikTok Layout Error: ${e.message}")
                        }
                    }
                }
            })
        } catch (e: Throwable) {
            XposedBridge.log("Rhpatch TikTok Menu Hook Error: \${e.message}")
        }
    }
    
    private fun findRootLinearLayout(viewGroup: ViewGroup): ViewGroup? {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is LinearLayout && child.orientation == LinearLayout.VERTICAL) {
                return child
            }
            if (child is ViewGroup) {
                val found = findRootLinearLayout(child)
                if (found != null) return found
            }
        }
        return null
    }
}

