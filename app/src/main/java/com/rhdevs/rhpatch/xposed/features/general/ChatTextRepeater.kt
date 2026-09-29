package com.rhdevs.rhpatch.xposed.features.general

import android.app.Activity
import android.content.SharedPreferences
import android.text.InputType
import android.view.Gravity
import android.view.Menu
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.rhdevs.rhpatch.R
import com.rhdevs.rhpatch.xposed.core.Feature
import com.rhdevs.rhpatch.xposed.core.WppCore
import com.rhdevs.rhpatch.xposed.core.components.AlertDialogWpp
import com.rhdevs.rhpatch.xposed.core.devkit.Unobfuscator
import com.rhdevs.rhpatch.xposed.utils.Utils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatTextRepeater(loader: ClassLoader, preferences: SharedPreferences) : Feature(loader, preferences) {

    companion object {
        private const val MENU_TEXT_REPEATER_ID = 90214
    }

    private var activeJob: Job? = null

    override fun getPluginName(): String {
        return "ChatTextRepeater"
    }

    override fun doHook() {
        if (!prefs.getBoolean("text_repeater_enabled", true)) return

        try {
            val onCreateMenuMethod = Unobfuscator.loadOnCreatedMenuConversation(classLoader)
            XposedBridge.hookMethod(onCreateMenuMethod, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val menu = param.args[0] as? Menu ?: return
                        if (menu.findItem(MENU_TEXT_REPEATER_ID) != null) return

                        val title = try {
                            Utils.getString(R.string.text_repeater_title)
                        } catch (_: Throwable) {
                            "Text Repeater"
                        }

                        val menuItem = menu.add(0, MENU_TEXT_REPEATER_ID, 0, title)
                        menuItem.setOnMenuItemClickListener {
                            val activity = WppCore.getCurrentActivity() ?: return@setOnMenuItemClickListener false
                            showRepeaterDialog(activity)
                            true
                        }
                    } catch (e: Throwable) {
                        logDebug(e)
                    }
                }
            })
        } catch (e: Throwable) {
            logDebug("Error hooking Conversation menu for TextRepeater", e)
        }
    }

    private fun showRepeaterDialog(activity: Activity) {
        val userJid = WppCore.getCurrentUserJid()
        if (userJid == null || userJid.isNull) {
            Toast.makeText(activity, "Gagal mendapatkan target obrolan", Toast.LENGTH_SHORT).show()
            return
        }

        val padding = Utils.dipToPixels(16)
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding / 2, padding, padding / 2)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val etMessage = EditText(activity).apply {
            hint = try { Utils.getString(R.string.text_repeater_msg_hint) } catch (_: Throwable) { "Pesan yang ingin dikirim..." }
            minLines = 2
            maxLines = 4
            gravity = Gravity.TOP or Gravity.START
        }
        container.addView(etMessage)

        val etCount = EditText(activity).apply {
            hint = try { Utils.getString(R.string.text_repeater_count_hint) } catch (_: Throwable) { "Jumlah pesan (cth: 10)" }
            inputType = InputType.TYPE_CLASS_NUMBER
            setText("10")
        }
        container.addView(etCount)

        val etDelay = EditText(activity).apply {
            hint = try { Utils.getString(R.string.text_repeater_delay_hint) } catch (_: Throwable) { "Jeda delay (ms, default 100)" }
            inputType = InputType.TYPE_CLASS_NUMBER
            setText("100")
        }
        container.addView(etDelay)

        val tvStatus = TextView(activity).apply {
            text = "Siap mengirim"
            setPadding(0, Utils.dipToPixels(8), 0, Utils.dipToPixels(8))
            textSize = 13f
        }
        container.addView(tvStatus)

        val dialog = AlertDialogWpp(activity).apply {
            setTitle(try { Utils.getString(R.string.text_repeater_title) } catch (_: Throwable) { "Text Repeater" })
            setView(container)
            setNegativeButton("Tutup") { d, _ ->
                activeJob?.cancel()
                activeJob = null
                d.dismiss()
            }
            setPositiveButton("Mulai Kirim", null)
        }

        val createdDialog = dialog.create()
        createdDialog.show()

        // Handle positive button manually to keep dialog open during sending
        val posBtn = if (dialog is AlertDialogWpp && !AlertDialogWpp.isSystemDialog) {
            createdDialog.findViewById<Button>(android.R.id.button1)
        } else {
            (createdDialog as? android.app.AlertDialog)?.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
        }

        posBtn?.setOnClickListener {
            if (activeJob?.isActive == true) {
                // Cancel action
                activeJob?.cancel()
                activeJob = null
                posBtn.text = "Mulai Kirim"
                tvStatus.text = "Pengiriman dihentikan."
                return@setOnClickListener
            }

            val text = etMessage.text.toString().trim()
            if (text.isEmpty()) {
                etMessage.error = "Teks tidak boleh kosong"
                return@setOnClickListener
            }

            val count = etCount.text.toString().toIntOrNull() ?: 10
            val delayMs = (etDelay.text.toString().toLongOrNull() ?: 100L).coerceAtLeast(50L)

            posBtn.text = "Hentikan"
            etMessage.isEnabled = false
            etCount.isEnabled = false
            etDelay.isEnabled = false

            activeJob = CoroutineScope(Dispatchers.IO).launch {
                var sentCount = 0
                val targetJid = userJid.userJid ?: userJid.phoneJid ?: userJid.phoneRawString ?: userJid.userRawString
                for (i in 1..count) {
                    if (!isActive) break
                    try {
                        WppCore.sendMessageToJid(targetJid, text)
                        sentCount++
                    } catch (e: Throwable) {
                        logDebug("TextRepeater send error at $i", e)
                        try {
                            WppCore.sendMessage(userJid.phoneRawString ?: "", text)
                            sentCount++
                        } catch (_: Throwable) {}
                    }

                    withContext(Dispatchers.Main) {
                        tvStatus.text = "Mengirim: $sentCount / $count"
                    }

                    delay(delayMs)
                }

                withContext(Dispatchers.Main) {
                    etMessage.isEnabled = true
                    etCount.isEnabled = true
                    etDelay.isEnabled = true
                    posBtn.text = "Mulai Kirim"
                    if (sentCount >= count) {
                        tvStatus.text = "Selesai! $sentCount pesan berhasil dikirim."
                    } else {
                        tvStatus.text = "Dihentikan. $sentCount / $count terkirim."
                    }
                }
            }
        }
    }
}
