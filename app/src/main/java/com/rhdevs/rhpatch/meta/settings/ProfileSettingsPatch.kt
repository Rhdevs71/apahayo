package com.rhdevs.rhpatch.meta.settings

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val ProfileSettingsPatch = patch(
    name = "Rhpatch Profile Settings",
    description = "Menyisipkan tombol pengaturan Rhpatch di halaman profil"
) {
    runCatching {
        // Menggunakan Fragment onViewCreated dari Profile untuk menyuntikkan tombol pengaturan
        // secara aman tanpa merusak RecyclerView ProfileUserInfoViewBinder
        val userDetailFragmentClass = XposedHelpers.findClassIfExists("com.instagram.profile.fragment.UserDetailFragment", classLoader)
        if (userDetailFragmentClass != null) {
            XposedBridge.hookAllMethods(userDetailFragmentClass, "onViewCreated", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val view = param.args[0] as? ViewGroup ?: return
                        val context = view.context
                        
                        if (view.findViewWithTag<View>("rhp_settings_btn") != null) return
                        
                        val dp = context.resources.displayMetrics.density
                        
                        // Membuat Floating Tombol Pengaturan Rhpatch
                        val fab = ImageButton(context).apply {
                            tag = "rhp_settings_btn"
                            setImageResource(android.R.drawable.ic_menu_preferences)
                            setBackgroundColor(Color.parseColor("#80000000")) // Semi-transparan hitam
                            setColorFilter(Color.WHITE)
                            val size = (40 * dp).toInt()
                            layoutParams = FrameLayout.LayoutParams(size, size).apply {
                                gravity = Gravity.TOP or Gravity.START
                                topMargin = (16 * dp).toInt()
                                leftMargin = (16 * dp).toInt()
                            }
                            setOnClickListener {
                                RhpatchSettingsDialog.showSettingsDialog(context)
                            }
                        }
                        
                        // Coba tambahkan ke root view profile
                        if (view is FrameLayout || view is android.widget.RelativeLayout) {
                            view.addView(fab)
                            XposedBridge.log("Rhpatch: [Settings] Berhasil menyuntikkan tombol pengaturan di UserDetailFragment")
                        } else {
                            // Jika bukan framelayout, cari induknya
                            val parent = view.parent as? ViewGroup
                            if (parent is FrameLayout || parent is android.widget.RelativeLayout) {
                                parent.addView(fab)
                            }
                        }
                        
                        // [FRIENDSHIP STATUS COLORED]
                        // Warnai tombol Follow / Following
                        try {
                            val prefs = context.getSharedPreferences("rhpatch_settings", android.content.Context.MODE_PRIVATE)
                            if (prefs.getBoolean("pref_colored_friendship", true)) {
                                val allViews = arrayListOf<View>()
                                fun findAllViews(v: View) {
                                    allViews.add(v)
                                    if (v is ViewGroup) {
                                        for (i in 0 until v.childCount) {
                                            findAllViews(v.getChildAt(i))
                                        }
                                    }
                                }
                                findAllViews(view)
                                
                                for (v in allViews) {
                                    if (v is android.widget.TextView) {
                                        val text = v.text.toString()
                                        if (text.equals("Following", true) || text.equals("Mengikuti", true)) {
                                            v.setTextColor(Color.parseColor("#4CAF50")) // Green
                                        } else if (text.equals("Follow Back", true) || text.equals("Ikuti Balik", true)) {
                                            v.setTextColor(Color.parseColor("#FF9800")) // Orange
                                        } else if (text.equals("Follow", true) || text.equals("Ikuti", true)) {
                                            v.setTextColor(Color.parseColor("#2196F3")) // Blue
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {}
                        
                    } catch (e: Exception) {
                        XposedBridge.log("Rhpatch: [Settings] Gagal menyuntikkan tombol: $e")
                    }
                }
            })
        } else {
            XposedBridge.log("Rhpatch: [Settings] Kelas UserDetailFragment tidak ditemukan!")
        }
    }
}
