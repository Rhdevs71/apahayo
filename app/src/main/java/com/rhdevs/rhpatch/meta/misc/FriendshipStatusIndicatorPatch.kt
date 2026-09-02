package com.rhdevs.rhpatch.meta.misc

import android.graphics.Color
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
        val processName = android.app.Application.getProcessName()
        if (processName != appContext.packageName) return@runCatching

        if (!com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.init(appContext)) return@runCatching

        val bindInternalBadges = com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.findMethodUsingStrings("bindInternalBadges")
        val getMapMethod = com.rhdevs.rhpatch.meta.devkit.MetaUnobfuscator.getFriendshipMapMethod()

        if (bindInternalBadges.isNotEmpty() && getMapMethod != null) {
            XposedBridge.hookMethod(bindInternalBadges.first(), object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val prefs = de.robv.android.xposed.XSharedPreferences("com.rhdevs.rhpatch", "com.instagram.android")
                        prefs.makeWorldReadable()
                        if (!prefs.getBoolean("pref_colored_friendship", false)) return

                        val view = param.args.firstOrNull { it is View } as? View ?: return
                        val profileInfo = param.args.firstOrNull { it?.javaClass?.name?.contains("Profile", ignoreCase = true) == true || it?.javaClass?.name?.contains("UserInfo", ignoreCase = true) == true } ?: return

                        // Find UserData inside profileInfo
                        val userDataMethod = profileInfo.javaClass.methods.firstOrNull { it.returnType.name.contains("UserData", ignoreCase = true) } ?: return
                        val userData = userDataMethod.invoke(profileInfo) ?: return

                        // Find FriendshipStatus inside userData
                        val friendshipMethod = userData.javaClass.methods.firstOrNull { it.returnType.name == "com.instagram.user.model.FriendshipStatus" } ?: return
                        val friendshipStatus = friendshipMethod.invoke(userData) ?: return

                        val map = getMapMethod.invoke(null, friendshipStatus) as? Map<String, Boolean> ?: return
                        
                        val followedBy = map["is_follower"] == true || map["is_following_me"] == true || map["followed_by"] == true
                        val following = map["following"] == true

                        val parent = view.parent as? ViewGroup ?: return
                        if (parent.findViewWithTag<View>("rhpatch_friendship") != null) return

                        val context = view.context
                        val tv = TextView(context).apply {
                            tag = "rhpatch_friendship"
                            textSize = 12f
                            setPadding(16, 8, 16, 8)
                            
                            if (followedBy && following) {
                                text = "Following Each Other"
                                setTextColor(Color.parseColor("#3389DF"))
                            } else if (followedBy) {
                                text = "Follows You"
                                setTextColor(Color.parseColor("#3CC176"))
                            } else {
                                text = "Doesn't Follow You"
                                setTextColor(Color.parseColor("#EB4941"))
                            }
                        }

                        val lp = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        lp.setMargins(0, 8, 0, 8)
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
