package com.rhdevs.rhpatch.revanced.meta.settings

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
