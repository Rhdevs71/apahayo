package com.rhdevs.rhpatch.meta.misc

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Method

val FriendshipStatusIndicatorPatch = patch(
    name = "Indikator Pertemanan Berwarna",
    description = "Menambahkan label status pertemanan (Follows You) di profil"
) {
    runCatching {
        if (!com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.init(appContext)) return@runCatching

        val bindInternalBadges = com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.findMethodUsingStrings("bindInternalBadges")
        val getMapMethod = com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.getFriendshipMapMethod()

        if (bindInternalBadges.isNotEmpty() && getMapMethod != null) {
            XposedBridge.hookMethod(bindInternalBadges.first(), object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val view = param.args.firstOrNull { it is View } as? View ?: return
                        val context = view.context
                        val prefs = context.getSharedPreferences("rhpatch_settings", Context.MODE_PRIVATE)
                        
                        if (!prefs.getBoolean("pref_colored_friendship", false)) return

                        val profileInfo = param.args.firstOrNull { it?.javaClass?.name?.contains("Profile", ignoreCase = true) == true || it?.javaClass?.name?.contains("UserInfo", ignoreCase = true) == true } ?: return

                        val userDataMethod = profileInfo.javaClass.methods.firstOrNull { it.returnType.name.contains("UserData", ignoreCase = true) } ?: return
                        val userData = userDataMethod.invoke(profileInfo) ?: return

                        val friendshipMethod = userData.javaClass.methods.firstOrNull { it.returnType.name == "com.instagram.user.model.FriendshipStatus" } ?: return
                        val friendshipStatus = friendshipMethod.invoke(userData) ?: return

                        val map = getMapMethod.invoke(null, friendshipStatus) as? Map<String, Boolean> ?: return
                        
                        val followedBy = map["is_follower"] == true || map["is_following_me"] == true || map["followed_by"] == true
                        val following = map["following"] == true

                        val parent = view.parent as? ViewGroup ?: return
                        if (parent.findViewWithTag<View>("rhpatch_friendship") != null) return

                        val isDarkMode = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                        val dp = context.resources.displayMetrics.density

                        val tv = TextView(context).apply {
                            tag = "rhpatch_friendship"
                            textSize = 12f
                            setPadding((12 * dp).toInt(), (4 * dp).toInt(), (12 * dp).toInt(), (4 * dp).toInt())
                            setTypeface(null, android.graphics.Typeface.BOLD)

                            background = GradientDrawable().apply {
                                setColor(if (isDarkMode) Color.parseColor("#333333") else Color.parseColor("#E5E5E5"))
                                cornerRadius = 50f * dp
                            }
                            
                            if (followedBy && following) {
                                text = "⇄ Mengikuti satu sama lain"
                                setTextColor(Color.parseColor("#3CC176"))
                            } else if (followedBy) {
                                text = "✓ Mengikuti Anda"
                                setTextColor(Color.parseColor("#3CC176"))
                            } else {
                                text = "✕ Tidak mengikuti Anda"
                                setTextColor(Color.parseColor("#EB4941"))
                            }

                            setOnClickListener {
                                val message = StringBuilder()
                                map.forEach { (k, v) ->
                                    message.append("$k: ${v.toString().uppercase()}\n\n")
                                }
                                AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                                    .setTitle("Status pertemanan")
                                    .setMessage(message.toString().trimEnd())
                                    .setPositiveButton("OKE", null)
                                    .show()
                            }
                        }

                        val lp = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        lp.setMargins(0, (8 * dp).toInt(), 0, (8 * dp).toInt())
                        parent.addView(tv, parent.indexOfChild(view) + 1, lp)
                    } catch (e: Exception) {
                        XposedBridge.log("Rhpatch: FriendshipStatusIndicator error: " + e.message)
                    }
                }
            })
        } else {
            XposedBridge.log("Rhpatch: FriendshipStatusIndicator missing methods")
        }
    }.onFailure { XposedBridge.log("Rhpatch: [FriendshipStatusIndicator] Patch failed: it") }
}
