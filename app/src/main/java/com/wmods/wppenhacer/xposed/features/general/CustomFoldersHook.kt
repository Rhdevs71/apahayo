package com.wmods.wppenhacer.xposed.features.general

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.wmods.wppenhacer.xposed.core.Feature
import com.wmods.wppenhacer.xposed.core.components.WaContactWpp
import com.wmods.wppenhacer.xposed.features.listeners.ContactItemListener
import com.wmods.wppenhacer.xposed.utils.Utils
import de.robv.android.xposed.XposedBridge
import org.json.JSONArray
import org.json.JSONObject

class CustomFoldersHook(loader: ClassLoader, preferences: SharedPreferences) : Feature(loader, preferences) {

    override fun getPluginName(): String {
        return "CustomFoldersHook"
    }

    override fun doHook() {
        val enabled = prefs.getBoolean("custom_folders_enabled", false)
        if (!enabled) return

        XposedBridge.log("WaEnhancer CustomFoldersHook: Registering contact list binder hook")

        ContactItemListener.contactListeners.add(object : ContactItemListener.OnContactItemListener() {
            override fun onBind(waContact: WaContactWpp?, view: View?) {
                if (waContact == null || view == null) return
                val userJid = waContact.userJid
                val jidStr = userJid.phoneRawString ?: return

                // Run on UI thread to update views
                view.post {
                    try {
                        val folderInfo = getFolderForJid(jidStr)
                        updateFolderBadge(view, waContact.displayName ?: "", folderInfo)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    private fun getFolderForJid(jid: String): JSONObject? {
        val foldersJson = prefs.getString("custom_folders", "[]") ?: "[]"
        try {
            val array = JSONArray(foldersJson)
            for (i in 0 until array.length()) {
                val folder = array.getJSONObject(i)
                val contacts = folder.optString("contacts", "")
                if (contacts.split(",").map { it.trim() }.contains(jid)) {
                    return folder
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun updateFolderBadge(view: View, displayName: String, folderInfo: JSONObject?) {
        try {
            // Find name TextView recursively
            val nameTextView = findTextViewByText(view, displayName) ?: return
            val parent = nameTextView.parent as? ViewGroup ?: return

            // Remove existing badge if present
            val existingBadge = parent.findViewWithTag<View>("wpp_folder_badge")
            if (existingBadge != null) {
                parent.removeView(existingBadge)
            }

            if (folderInfo == null) return

            val folderName = folderInfo.optString("name", "")
            val folderColorStr = folderInfo.optString("color", "#ff4faf50")
            val folderColor = try {
                Color.parseColor(folderColorStr)
            } catch (e: Exception) {
                Color.parseColor("#ff4faf50")
            }

            val context = view.context
            val badge = TextView(context).apply {
                tag = "wpp_folder_badge"
                text = folderName
                textSize = 10f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(12, 4, 12, 4)

                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = Utils.dipToPixels(12f).toFloat()
                    setColor(folderColor)
                }
            }

            // Set small margins
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = Utils.dipToPixels(8f)
                gravity = Gravity.CENTER_VERTICAL
            }

            // Add next to name TextView
            val index = parent.indexOfChild(nameTextView)
            parent.addView(badge, index + 1, params)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun findTextViewByText(view: View, text: String): TextView? {
        if (text.isEmpty()) return null
        if (view is TextView && view.text.toString().equals(text, ignoreCase = true)) {
            return view
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val res = findTextViewByText(view.getChildAt(i), text)
                if (res != null) return res
            }
        }
        return null
    }
}
