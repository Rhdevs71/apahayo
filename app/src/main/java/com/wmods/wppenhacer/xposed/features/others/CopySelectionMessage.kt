package com.wmods.wppenhacer.xposed.features.others

import android.annotation.SuppressLint
import android.app.Activity
import android.app.TimePickerDialog
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
import com.wmods.wppenhacer.database.AppDatabase
import com.wmods.wppenhacer.database.FakeChatBackup
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
import de.robv.android.xposed.XposedHelpers
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CopySelectionMessage(classLoader: ClassLoader, prefs: SharedPreferences) :
    Feature(classLoader, prefs) {

    private val dbExecutor: ExecutorService = Executors.newSingleThreadExecutor()

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
        var editedTimestamp = fMessage.timeStamp

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

        // 1. Outlined button to edit time
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeButton = MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Time: " + sdf.format(Date(editedTimestamp))
            setTextColor(textColor)
            setStrokeColor(ColorStateList.valueOf(outlineColor))
            cornerRadius = Utils.dipToPixels(50f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 12.dp()
            }
        }

        timeButton.setOnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = editedTimestamp }
            TimePickerDialog(activity, { _, hourOfDay, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                cal.set(Calendar.MINUTE, minute)
                editedTimestamp = cal.timeInMillis
                timeButton.text = "Time: " + sdf.format(Date(editedTimestamp))
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        // 2. Outlined save button
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

        // 3. Outlined Undo button
        val undoButton = MaterialButton(
            ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "Undo (Original)"
            setTextColor(Color.RED)
            setStrokeColor(ColorStateList.valueOf(Color.RED))
            cornerRadius = Utils.dipToPixels(50f)
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.START
                topMargin = 12.dp()
            }
        }

        val buttonsLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            addView(undoButton)
            // Spacer to separate buttons
            val spacer = View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, 0, 1.0f)
            }
            addView(spacer)
            addView(saveButton)
        }

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(), 8.dp(), 16.dp(), 16.dp())
            addView(scrollView)
            addView(timeButton)
            addView(buttonsLayout)
        }

        val dialog = AlertDialogWpp(activity)
            .setTitle("Fake Chat Editor")
            .setView(container)
            .create()

        // Check if backup exists in background thread
        dbExecutor.execute {
            val db = AppDatabase.getInstance(activity)
            val backup = db.fakeChatBackupDao().getBackup(fMessage.key.messageID)
            if (backup != null) {
                activity.runOnUiThread {
                    undoButton.visibility = View.VISIBLE
                }
            }
        }

        undoButton.setOnClickListener {
            dbExecutor.execute {
                val db = AppDatabase.getInstance(activity)
                val backup = db.fakeChatBackupDao().getBackup(fMessage.key.messageID)
                if (backup != null) {
                    restoreOriginalMessage(fMessage, backup.originalText, backup.originalTimestamp, activity)
                    db.fakeChatBackupDao().deleteByMsgId(fMessage.key.messageID)
                    activity.runOnUiThread {
                        Toast.makeText(activity, "Message restored to original", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }
            }
        }

        saveButton.setOnClickListener {
            val newText = editText.text.toString()
            val newTime = editedTimestamp
            dbExecutor.execute {
                val db = AppDatabase.getInstance(activity)
                val backup = db.fakeChatBackupDao().getBackup(fMessage.key.messageID)
                if (backup == null) {
                    // Create first-time backup
                    val newBackup = FakeChatBackup(
                        messageId = fMessage.key.messageID,
                        originalText = originalText,
                        originalTimestamp = fMessage.timeStamp
                    )
                    db.fakeChatBackupDao().insert(newBackup)
                }

                updateMessageTextAndTimestampFake(fMessage, newText, newTime, activity)
                activity.runOnUiThread {
                    Toast.makeText(activity, "Message updated locally", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun updateMessageTextAndTimestampFake(fMessage: FMessageWpp, newText: String, newTimestamp: Long, activity: Activity) {
        try {
            val originalObject = fMessage.getObject()
            val oldText = fMessage.messageStr ?: ""

            // 1. Update in-memory cached FMessage object fields dynamically via reflection
            var currentClass: Class<*>? = originalObject.javaClass
            while (currentClass != null) {
                for (field in currentClass.declaredFields) {
                    if (field.type == String::class.java) {
                        try {
                            field.isAccessible = true
                            val value = field.get(originalObject) as? String
                            if (value == oldText) {
                                field.set(originalObject, newText)
                            }
                        } catch (_: Exception) {}
                    }
                }
                currentClass = currentClass.superclass
            }

            // Update in-memory timestamp field
            try {
                val timestampField = XposedHelpers.findField(originalObject.javaClass, "A0I") // Obfuscated or standard timestamp field in WhatsApp
                timestampField.isAccessible = true
                timestampField.setLong(originalObject, newTimestamp)
            } catch (_: Exception) {
                try {
                    // Fallback to searching all Long fields if obfuscated field changes
                    var cls: Class<*>? = originalObject.javaClass
                    var timestampFound = false
                    while (cls != null) {
                        for (field in cls.declaredFields) {
                            if (field.type == Long::class.javaPrimitiveType || field.type == Long::class.java) {
                                field.isAccessible = true
                                val value = field.getLong(originalObject)
                                if (value == fMessage.timeStamp) {
                                    field.setLong(originalObject, newTimestamp)
                                    timestampFound = true
                                    break
                                }
                            }
                        }
                        if (timestampFound) break
                        cls = cls.superclass
                    }
                } catch (_: Exception) {}
            }

            // 2. Update persistent local SQLite database
            val db = MessageStore.getInstance().getDatabase()
            if (db != null) {
                db.execSQL(
                    "UPDATE message SET text_data = ?, timestamp = ? WHERE key_id = ?",
                    arrayOf<Any?>(newText, newTimestamp, fMessage.key.messageID)
                )
                try {
                    db.execSQL(
                        "UPDATE message_ftsv2_content SET c0content = ? WHERE docid = (SELECT _id FROM message WHERE key_id = ?)",
                        arrayOf<Any?>(newText, fMessage.key.messageID)
                    )
                } catch (_: Exception) {}
            }

            // 3. Notify ListView/RecyclerView adapter to refresh and redraw screen
            activity.runOnUiThread {
                ConversationItemListener.notifyDataSetChanged()
            }

        } catch (e: Exception) {
            XposedBridge.log("Fake Chat Editor Error: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun restoreOriginalMessage(fMessage: FMessageWpp, originalText: String, originalTimestamp: Long, activity: Activity) {
        updateMessageTextAndTimestampFake(fMessage, originalText, originalTimestamp, activity)
    }

    override fun getPluginName(): String = "Copy Selection Message"
}
