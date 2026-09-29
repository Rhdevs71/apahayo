package com.rhdevs.rhpatch.xposed.features.general

import android.app.Activity
import android.content.SharedPreferences
import android.text.InputType
import android.view.Gravity
import android.view.Menu
import android.view.View
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
import com.rhdevs.rhpatch.xposed.core.components.FMessageWpp
import com.rhdevs.rhpatch.xposed.core.devkit.Unobfuscator
import com.rhdevs.rhpatch.xposed.utils.ReflectionUtils
import com.rhdevs.rhpatch.xposed.utils.Utils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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
                        if (!prefs.getBoolean("text_repeater_enabled", true)) return
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
            hint = try { Utils.getString(R.string.text_repeater_delay_hint) } catch (_: Throwable) { "Jeda delay (ms, default 150)" }
            inputType = InputType.TYPE_CLASS_NUMBER
            setText("150")
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

        val posBtn = if (!AlertDialogWpp.isSystemDialog) {
            createdDialog.findViewById<Button>(android.R.id.button1)
        } else {
            (createdDialog as? android.app.AlertDialog)?.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
        }

        posBtn?.setOnClickListener {
            if (activeJob?.isActive == true) {
                activeJob?.cancel()
                activeJob = null
                posBtn.text = "Mulai Kirim"
                tvStatus.text = "Pengiriman dihentikan."
                etMessage.isEnabled = true
                etCount.isEnabled = true
                etDelay.isEnabled = true
                return@setOnClickListener
            }

            val text = etMessage.text.toString().trim()
            if (text.isEmpty()) {
                etMessage.error = "Teks tidak boleh kosong"
                return@setOnClickListener
            }

            val count = etCount.text.toString().toIntOrNull() ?: 10
            val delayMs = (etDelay.text.toString().toLongOrNull() ?: 150L).coerceAtLeast(80L)

            posBtn.text = "Hentikan"
            etMessage.isEnabled = false
            etCount.isEnabled = false
            etDelay.isEnabled = false

            activeJob = CoroutineScope(Dispatchers.IO).launch {
                var sentCount = 0
                for (i in 1..count) {
                    if (!isActive || activity.isFinishing || activity.isDestroyed) break

                    val success = dispatchMessageToChat(activity, userJid, text)
                    if (success) {
                        sentCount++
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
                    } else if (sentCount == 0) {
                        tvStatus.text = "Gagal mengirim pesan. Pastikan keyboard obrolan aktif."
                    } else {
                        tvStatus.text = "Dihentikan. $sentCount / $count terkirim."
                    }
                }
            }
        }
    }

    private suspend fun dispatchMessageToChat(
        activity: Activity,
        userJid: FMessageWpp.UserJid,
        text: String
    ): Boolean {
        // 1. Direct UI Dispatch (native & 100% stable in Conversation screen)
        val deferred = CompletableDeferred<Boolean>()
        activity.runOnUiThread {
            try {
                val packageName = activity.packageName
                val entryId = activity.resources.getIdentifier("entry", "id", packageName)
                val entry = activity.findViewById<EditText>(entryId)

                if (entry != null) {
                    entry.setText(text)
                    entry.setSelection(text.length)

                    // Post to let WhatsApp's TextWatcher swap mic button to send button
                    entry.post {
                        try {
                            val sendId = activity.resources.getIdentifier("send", "id", packageName)
                            val sendBtn = activity.findViewById<View>(sendId)

                            if (sendBtn != null && sendBtn.isShown) {
                                sendBtn.performClick()
                                deferred.complete(true)
                            } else {
                                // Try finding any view with send contentDescription or id
                                val fallbackBtn = sendBtn ?: activity.findViewById<View>(
                                    activity.resources.getIdentifier("send_container", "id", packageName)
                                )
                                if (fallbackBtn != null) {
                                    fallbackBtn.performClick()
                                    deferred.complete(true)
                                } else {
                                    deferred.complete(false)
                                }
                            }
                        } catch (t: Throwable) {
                            deferred.complete(false)
                        }
                    }
                } else {
                    deferred.complete(false)
                }
            } catch (t: Throwable) {
                deferred.complete(false)
            }
        }

        val result = withTimeoutOrNull(400L) { deferred.await() } ?: false
        if (result) return true

        // 2. Fallback via ActionUser with robust reflection
        return try {
            val targetJid = userJid.userJid ?: userJid.phoneJid ?: userJid.phoneRawString ?: userJid.userRawString
            val actionUser = WppCore.getActionUser()
            val actionUserClass = WppCore.getActionUserClass()
            if (actionUser != null && actionUserClass != null) {
                val senderMethod = ReflectionUtils.findMethodUsingFilterIfExists(actionUserClass) { method ->
                    val params = method.parameterTypes
                    val hasString = ReflectionUtils.findIndexOfType(params, String::class.java) != -1
                    val hasJid = params.any { param ->
                        param.name.endsWith("Jid", ignoreCase = true) ||
                                param == FMessageWpp.UserJid.TYPE_JID ||
                                param == FMessageWpp.UserJid.TYPE_USERJID ||
                                (param.isInterface && !param.name.startsWith("java.") && !param.name.startsWith("android."))
                    }
                    hasString && hasJid && method.name != "toString"
                }
                if (senderMethod != null) {
                    val newObject = arrayOfNulls<Any>(senderMethod.parameterCount)
                    for (i in newObject.indices) {
                        newObject[i] = ReflectionUtils.getDefaultValue(senderMethod.parameterTypes[i])
                    }
                    val textIndex = ReflectionUtils.findIndexOfType(senderMethod.parameterTypes, String::class.java)
                    newObject[textIndex] = text
                    val jidIndex = senderMethod.parameterTypes.indexOfFirst { param ->
                        param.name.endsWith("Jid", ignoreCase = true) ||
                                param == FMessageWpp.UserJid.TYPE_JID ||
                                param == FMessageWpp.UserJid.TYPE_USERJID
                    }
                    if (jidIndex != -1) {
                        newObject[jidIndex] = targetJid
                    }
                    senderMethod.invoke(actionUser, *newObject)
                    true
                } else false
            } else false
        } catch (e: Throwable) {
            logDebug("Fallback ActionUser send error", e)
            false
        }
    }
}
