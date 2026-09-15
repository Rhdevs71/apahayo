package com.rhdevs.rhpatch.meta.settings

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.RelativeLayout
import com.rhdevs.rhpatch.patch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

val ProfileSettingsPatch = patch(
    name = "Rhpatch Profile Settings",
    description = "Menyisipkan tombol pengaturan Rhpatch di halaman profil"
) {
    runCatching {
        val userDetailFragmentClass = XposedHelpers.findClassIfExists("com.instagram.profile.fragment.UserDetailFragment", classLoader)
        if (userDetailFragmentClass != null) {
            val injectSettingsButton = { root: ViewGroup ->
                try {
                    val context = root.context
                    val targetGroup = (root.parent as? ViewGroup) ?: root
                    if (targetGroup.findViewWithTag<View>("rhp_settings_btn") == null && root.findViewWithTag<View>("rhp_settings_btn") == null) {
                        val dp = context.resources.displayMetrics.density
                        val size = (40 * dp).toInt()

                        val fab = ImageButton(context).apply {
                            tag = "rhp_settings_btn"
                            setImageResource(android.R.drawable.ic_menu_preferences)
                            setBackgroundColor(Color.parseColor("#80000000"))
                            setColorFilter(Color.WHITE)
                            setOnClickListener {
                                RhpatchSettingsDialog.showSettingsDialog(context)
                            }
                        }

                        val lp: ViewGroup.LayoutParams = when (targetGroup) {
                            is FrameLayout -> FrameLayout.LayoutParams(size, size).apply {
                                gravity = Gravity.TOP or Gravity.START
                                topMargin = (16 * dp).toInt()
                                leftMargin = (16 * dp).toInt()
                            }
                            is RelativeLayout -> RelativeLayout.LayoutParams(size, size).apply {
                                addRule(RelativeLayout.ALIGN_PARENT_TOP)
                                addRule(RelativeLayout.ALIGN_PARENT_START)
                                topMargin = (16 * dp).toInt()
                                leftMargin = (16 * dp).toInt()
                            }
                            else -> ViewGroup.MarginLayoutParams(size, size).apply {
                                topMargin = (16 * dp).toInt()
                                leftMargin = (16 * dp).toInt()
                            }
                        }
                        targetGroup.addView(fab, lp)
                        XposedBridge.log("Rhpatch: [Settings] Berhasil menyuntikkan tombol pengaturan di profil")
                    }
                } catch (e: Throwable) {
                    XposedBridge.log("Rhpatch: [Settings] Gagal menyuntikkan tombol: $e")
                }
            }

            XposedBridge.hookAllMethods(userDetailFragmentClass, "onViewCreated", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val view = param.args[0] as? ViewGroup ?: return
                    view.post {
                        injectSettingsButton(view)
                    }
                }
            })

            XposedBridge.hookAllMethods(userDetailFragmentClass, "onResume", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val fragment = param.thisObject as? androidx.fragment.app.Fragment ?: return
                    val view = fragment.view as? ViewGroup ?: return
                    view.post {
                        injectSettingsButton(view)
                    }
                }
            })
        } else {
            XposedBridge.log("Rhpatch: [Settings] Kelas UserDetailFragment tidak ditemukan!")
        }
    }
}
