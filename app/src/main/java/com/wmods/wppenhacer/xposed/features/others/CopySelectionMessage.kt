package com.wmods.wppenhacer.xposed.features.others

import android.annotation.SuppressLint
import android.app.Activity
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.children
import androidx.core.widget.NestedScrollView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.wmods.wppenhacer.R
import com.wmods.wppenhacer.xposed.core.Feature
import com.wmods.wppenhacer.xposed.core.WppCore
import com.wmods.wppenhacer.xposed.core.components.AlertDialogWpp
import com.wmods.wppenhacer.xposed.core.components.FMessageWpp
import com.wmods.wppenhacer.xposed.core.db.MessageStore
import com.wmods.wppenhacer.xposed.core.devkit.Unobfuscator
import com.wmods.wppenhacer.xposed.features.listeners.ConversationItemListener
import com.wmods.wppenhacer.xposed.utils.DesignUtils
import com.wmods.wppenhacer.xposed.utils.ModuleContextWrapper
import com.wmods.wppenhacer.xposed.utils.Utils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.io.File
import java.lang.reflect.Field

class CopySelectionMessage(classLoader: ClassLoader, prefs: SharedPreferences) :
    Feature(classLoader, prefs) {

    override fun doHook() {
        val copyEnabled = prefs.getBoolean("copy_selection_message", false)
        val fakeEditEnabled = prefs.getBoolean("fake_chat_editor_enabled", false)
        if (!copyEnabled && !fakeEditEnabled) return

        val popupWindowMessage = Unobfuscator.loadPopupWindowMessageClass(classLoader)
        XposedBridge.hookAllConstructors(popupWindowMessage, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val activity = WppCore.getCurrentActivity() ?: run {
                    logDebug("CurrentActivity is null")
                    return
                }
                val mainPopupWindow = param.thisObject as PopupWindow
                val viewGroup = mainPopupWindow.contentView as? ViewGroup ?: return

                val fMessageObj = param.args?.filterIsInstance(FMessageWpp.TYPE)?.firstOrNull()
                    ?: return
                val fMessage = FMessageWpp(fMessageObj)
                val messageText = fMessage.messageStr ?: ""

                if (messageText.isEmpty()) return

                val layout = viewGroup.findViewById<LinearLayout>(
                    Utils.getID("reactions_tray_layout", "id")
                ) ?: return
                layout.orientation = LinearLayout.VERTICAL
                val parentItems = layout.children.toList()
                layout.removeAllViews()
                val newContainer = LinearLayout(viewGroup.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    parentItems.forEach { addView(it) }
                }
                layout.addView(newContainer)

                // 1. Add Copy Selection Button
                if (copyEnabled) {
                    val copyButton = buildActionPill(activity, Utils.getString(R.string.copy_selection_action))
                    copyButton.setOnClickListener {
                        try {
                            mainPopupWindow.dismiss()
                        } catch (_: Throwable) {
                        }
                        val view = ConversationItemListener.listItems.entries.firstOrNull {
                            it.value.messageId == fMessage.key.messageID &&
                                    ConversationItemListener.isViewBoundToMessage(
                                        it.key,
                                        fMessage.key.messageID
                                    )
                        }?.key
                        val textView = view?.findViewById<TextView>(Utils.getID("message_text", "id"))
                        showSelectionDialog(activity, textView?.text ?: messageText)
                    }
                    layout.addView(copyButton)
                }

                // 2. Add Fake Chat Editor Button
                if (fakeEditEnabled) {
                    val editButton = buildActionPill(activity, "✏ Edit Text (Fake)")
                    editButton.setOnClickListener {
                        try {
                            mainPopupWindow.dismiss()
                        } catch (_: Throwable) {
                        }
                        showFakeEditDialog(activity, fMessage)
                    }
                    layout.addView(editButton)
                }
            }
        })
    }

    private fun buildActionPill(activity: Activity, buttonText: String): MaterialButton {
        val ctx = ModuleContextWrapper(activity)
        val textColor = DesignUtils.getPrimaryTextColor()
        val strokeColor = Color.argb(
            80,
            Color.red(textColor),
            Color.green(textColor),
            Color.blue(textColor)
        )
        return MaterialButton(
            ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = buttonText
            setTextColor(textColor)
            setStrokeColor(ColorStateList.valueOf(strokeColor))
            cornerRadius = Utils.dipToPixels(50f)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showSelectionDialog(activity: Activity, messageText: CharSequence) {
        val d = activity.resources.displayMetrics.density
        fun Int.dp() = (this * d).toInt()

        val ctx = ModuleContextWrapper(activity)

        val textInputLayout = TextInputLayout(
            ctx,
            null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            isCounterEnabled = true
            hint = Utils.getString(R.string.message)
        }
        val editText = TextInputEditText(textInputLayout.context).apply {
            setText(messageText)
            textSize = 14f
            minLines = 3
            maxLines = 10
            gravity = Gravity.TOP
            setLineSpacing(0f, 1.4f)
            isVerticalScrollBarEnabled = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setSelection(0)
        }
        textInputLayout.addView(editText)

        val scrollView = NestedScrollView(ctx).apply {
            isVerticalScrollBarEnabled = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_INSET
            addView(textInputLayout)
        }

        val textColor = DesignUtils.getPrimaryTextColor()
        val outlineColor = Color.argb(
            60,
            Color.red(textColor),
            Color.green(textColor),
            Color.blue(textColor)
        )

        val closeButton = MaterialButton(
            ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = Utils.getString(R.string.close)
            setTextColor(textColor)
            setStrokeColor(ColorStateList.valueOf(outlineColor))
            cornerRadius = Utils.dipToPixels(50f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
                topMargin = 12.dp()
            }
        }

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(), 8.dp(), 16.dp(), 16.dp())
            addView(scrollView)
            addView(closeButton)
        }

        val dialog = AlertDialogWpp(activity)
            .setTitle(Utils.getString(R.string.copy_selection_dialog_title))
            .setView(container)
            .create()

        closeButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showFakeEditDialog(activity: Activity, fMessage: FMessageWpp) {
        val d = activity.resources.displayMetrics.density
        fun Int.dp() = (this * d).toInt()

        val ctx = ModuleContextWrapper(activity)
        val originalText = fMessage.messageStr ?: ""

        val textInputLayout = TextInputLayout(
            ctx,
            null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            isCounterEnabled = true
            hint = "Edit Message (Local Fake)"
        }
        val editText = TextInputEditText(textInputLayout.context).apply {
            setText(originalText)
            textSize = 14f
            minLines = 3
            maxLines = 10
            gravity = Gravity.TOP
            setLineSpacing(0f, 1.4f)
            isVerticalScrollBarEnabled = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setSelection(originalText.length)
        }
        textInputLayout.addView(editText)

        val scrollView = NestedScrollView(ctx).apply {
            isVerticalScrollBarEnabled = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_INSET
            addView(textInputLayout)
        }

        val textColor = DesignUtils.getPrimaryTextColor()
        val outlineColor = Color.argb(
            60,
            Color.red(textColor),
            Color.green(textColor),
            Color.blue(textColor)
        )

        val saveButton = MaterialButton(
            ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "Save (Fake)"
            setTextColor(textColor)
            setStrokeColor(ColorStateList.valueOf(outlineColor))
            cornerRadius = Utils.dipToPixels(50f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
                topMargin = 12.dp()
            }
        }

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(), 8.dp(), 16.dp(), 16.dp())
            addView(scrollView)
            addView(saveButton)
        }

        val dialog = AlertDialogWpp(activity)
            .setTitle("Fake Chat Editor")
            .setView(container)
            .create()

        saveButton.setOnClickListener {
            val newText = editText.text.toString()
            if (newText != originalText) {
                updateMessageTextFake(fMessage, newText)
                Toast.makeText(activity, "Message updated locally", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun updateMessageTextFake(fMessage: FMessageWpp, newText: String) {
        try {
            val originalObject = fMessage.getObject()
            val oldText = fMessage.messageStr ?: ""

            // 1. Update in-memory cached FMessage object fields dynamically via reflection
            var currentClass: Class<*>? = originalObject.javaClass
            var fieldUpdated = false
            while (currentClass != null) {
                for (field in currentClass.declaredFields) {
                    if (field.type == String::class.java) {
                        try {
                            field.isAccessible = true
                            val value = field.get(originalObject) as? String
                            if (value == oldText) {
                                field.set(originalObject, newText)
                                fieldUpdated = true
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
                currentClass = currentClass.superclass
            }

            // 2. Update persistent local SQLite database
            val db = MessageStore.getInstance().getDatabase()
            if (db != null) {
                db.execSQL(
                    "UPDATE message SET text_data = ? WHERE key_id = ?",
                    arrayOf(newText, fMessage.key.messageID)
                )
                // Also update full-text search index table if it exists
                try {
                    db.execSQL(
                        "UPDATE message_ftsv2_content SET c0content = ? WHERE docid = (SELECT _id FROM message WHERE key_id = ?)",
                        arrayOf(newText, fMessage.key.messageID)
                    )
                } catch (_: Exception) {}
            }

            // 3. Notify ListView/RecyclerView adapter to refresh and redraw screen
            ConversationItemListener.notifyDataSetChanged()

        } catch (e: Exception) {
            XposedBridge.log("Fake Chat Editor Error: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun getPluginName(): String = "Copy Selection Message"
}
