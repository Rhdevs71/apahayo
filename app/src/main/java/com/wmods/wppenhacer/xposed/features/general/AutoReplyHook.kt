package com.wmods.wppenhacer.xposed.features.general

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import com.wmods.wppenhacer.xposed.core.Feature
import com.wmods.wppenhacer.xposed.core.WppCore
import com.wmods.wppenhacer.xposed.core.components.FMessageWpp
import com.wmods.wppenhacer.xposed.core.devkit.Unobfuscator
import com.wmods.wppenhacer.xposed.utils.ReflectionUtils
import com.wmods.wppenhacer.xposed.utils.Utils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Collections

class AutoReplyHook(loader: ClassLoader, preferences: SharedPreferences) : Feature(loader, preferences) {

    override fun getPluginName(): String {
        return "AutoReplyHook"
    }

    override fun doHook() {
        val enabled = prefs.getBoolean("auto_reply_enabled", false)
        if (!enabled) return

        hookReceiveMessage()
    }

    private fun hookReceiveMessage() {
        val method = Unobfuscator.loadReceiptMethod(classLoader)

        XposedBridge.hookMethod(method, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                // Ignore outgoing message notifications or null args
                if (param.args[4] == "sender" || param.args[1] == null || param.args[3] == null) return

                val fMsg = FMessageWpp.Key(param.args[3]).fMessage ?: return
                val userJid = fMsg.key.remoteJid

                // Do not respond to status updates or our own messages
                if (userJid.isStatus || fMsg.key.isFromMe) return

                val messageText = fMsg.messageStr ?: return
                if (TextUtils.isEmpty(messageText)) return

                val number = userJid.phoneNumber ?: return
                val isGroup = userJid.isGroup

                // Check contacts vs groups vs non-contacts
                val isSavedContact = !WppCore.getSContactName(userJid, true).isNullOrEmpty()

                // Load rules from SharedPreferences (which is remote preferences linked to our app)
                val rulesJsonStr = prefs.getString("auto_reply_rules", "[]") ?: "[]"
                try {
                    val rulesArray = JSONArray(rulesJsonStr)
                    for (i in 0 until rulesArray.length()) {
                        val ruleObj = rulesArray.getJSONObject(i)
                        if (!ruleObj.optBoolean("isEnabled", true)) continue

                        // Target filtering
                        val targetType = ruleObj.optString("targetType", "ALL")
                        when (targetType) {
                            "GROUPS" -> if (!isGroup) continue
                            "CONTACTS" -> if (isGroup || !isSavedContact) continue
                            "NON_CONTACTS" -> if (isGroup || isSavedContact) continue
                        }

                        // Time window filtering (active hours)
                        val startHourStr = ruleObj.optString("activeHoursStart")
                        val endHourStr = ruleObj.optString("activeHoursEnd")
                        if (!startHourStr.isNullOrEmpty() && !endHourStr.isNullOrEmpty()) {
                            if (!isCurrentTimeInWindow(startHourStr, endHourStr)) {
                                continue
                            }
                        }

                        // Keywords checking
                        val keywordsStr = ruleObj.optString("keywords", "")
                        val matchingType = ruleObj.optString("matchingType", "EXACT")
                        val isMatched = checkKeywordMatch(messageText, keywordsStr, matchingType)

                        if (isMatched) {
                            val replyText = ruleObj.optString("replyText", "")
                            val delaySec = ruleObj.optInt("delaySeconds", 0)
                            val quoteOriginal = ruleObj.optBoolean("quoteOriginal", false)
                            val isForward = ruleObj.optBoolean("isForward", false)
                            val forwardJid = ruleObj.optString("forwardJid", "")

                            // Execute reply/forward with optional delay
                            val handler = Handler(Looper.getMainLooper())
                            handler.postDelayed({
                                if (isForward && forwardJid.isNotEmpty()) {
                                    val forwardText = "[Forwarded from +$number]: $messageText"
                                    sendAutoReply(jid = forwardJid, replyText = forwardText, quoteMessage = null)
                                } else {
                                    sendAutoReply(jid = userJid.phoneRawString ?: "", replyText = replyText, quoteMessage = if (quoteOriginal) fMsg else null)
                                }
                            }, delaySec * 1000L)

                            // Exit loop after first matching rule is processed
                            break
                        }
                    }
                } catch (e: Exception) {
                    XposedBridge.log("AutoReply Error parsing rules: ${e.message}")
                    e.printStackTrace()
                }
            }
        })
    }

    private fun checkKeywordMatch(message: String, keywordsCsv: String, matchingType: String): Boolean {
        val msgClean = message.trim().lowercase()
        val keywords = keywordsCsv.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        if (keywords.isEmpty()) return false

        for (keyword in keywords) {
            when (matchingType) {
                "EXACT" -> {
                    if (msgClean == keyword) return true
                }
                "CONTAINS" -> {
                    if (msgClean.contains(keyword)) return true
                }
                "REGEX" -> {
                    try {
                        val regex = Regex(keyword)
                        if (regex.containsMatchIn(msgClean)) return true
                    } catch (e: Exception) {
                        // Ignore malformed regex
                    }
                }
            }
        }
        return false
    }

    private fun isCurrentTimeInWindow(startStr: String, endStr: String): Boolean {
        try {
            val now = Calendar.getInstance()
            val nowHour = now.get(Calendar.HOUR_OF_DAY)
            val nowMin = now.get(Calendar.MINUTE)
            val currentMinutes = nowHour * 60 + nowMin

            val startParts = startStr.split(":")
            val startMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()

            val endParts = endStr.split(":")
            val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()

            return if (startMinutes <= endMinutes) {
                // Same day window, e.g. 09:00 - 17:00
                currentMinutes in startMinutes..endMinutes
            } else {
                // Overnight window, e.g. 22:00 - 06:00
                currentMinutes >= startMinutes || currentMinutes <= endMinutes
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return true // Fallback if time parse fails
        }
    }

    private fun sendAutoReply(jid: String, replyText: String, quoteMessage: FMessageWpp?) {
        try {
            val userJid = WppCore.createUserJid(jid)
            if (userJid == null) return

            val actionUser = WppCore.getActionUser() ?: return

            val senderMethod = ReflectionUtils.findMethodUsingFilterIfExists(actionUser.javaClass) { method ->
                List::class.java.isAssignableFrom(method.returnType) &&
                        ReflectionUtils.findIndexOfType(method.parameterTypes, String::class.java) != -1 &&
                        ReflectionUtils.findIndexOfType(method.parameterTypes, List::class.java) != -1
            }

            if (senderMethod == null) {
                XposedBridge.log("AutoReply Error: Text send method not found")
                return
            }

            val newObject = arrayOfNulls<Any>(senderMethod.parameterCount)
            for (i in newObject.indices) {
                val param = senderMethod.parameterTypes[i]
                newObject[i] = ReflectionUtils.getDefaultValue(param)
            }

            // Set reply text
            val textIndex = ReflectionUtils.findIndexOfType(senderMethod.parameterTypes, String::class.java)
            newObject[textIndex] = replyText

            // Set recipient
            val jidIndex = ReflectionUtils.findIndexOfType(senderMethod.parameterTypes, List::class.java)
            newObject[jidIndex] = Collections.singletonList(userJid)

            // Quote original message if provided
            if (quoteMessage != null) {
                val quotedIndex = ReflectionUtils.findIndexOfType(senderMethod.parameterTypes, FMessageWpp.TYPE)
                if (quotedIndex != -1) {
                    newObject[quotedIndex] = quoteMessage.getObject()
                }
            }

            senderMethod.invoke(actionUser, *newObject)
            XposedBridge.log("Auto Reply sent successfully to $jid")
        } catch (e: Exception) {
            XposedBridge.log("AutoReply Error sending reply: ${e.message}")
            e.printStackTrace()
        }
    }
}
