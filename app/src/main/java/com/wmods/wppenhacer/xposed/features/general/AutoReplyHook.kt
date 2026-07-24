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
import de.robv.android.xposed.XposedHelpers
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Calendar
import java.util.Collections
import java.util.concurrent.Executors

class AutoReplyHook(loader: ClassLoader, preferences: SharedPreferences) : Feature(loader, preferences) {

        private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val processedMessagesCache = Collections.synchronizedSet(LinkedHashSet<String>())

    private fun isAlreadyProcessed(rawJid: String, text: String): Boolean {
        val signature = "$rawJid:$text"
        synchronized(processedMessagesCache) {
            if (processedMessagesCache.contains(signature)) {
                return true
            }
            processedMessagesCache.add(signature)
            // Limit cache size to 100 items to prevent memory growth
            if (processedMessagesCache.size > 100) {
                val iterator = processedMessagesCache.iterator()
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }
            return false
        }
    }

    override fun getPluginName(): String {
        return "AutoReplyHook"
    }

    override fun doHook() {
        val enabled = prefs.getBoolean("auto_reply_enabled", false)
        if (!enabled) return

        hookReceiveMessage()
        hookNotificationManager()
    }

    private fun hookNotificationManager() {
        try {
            val notificationManagerClass = XposedHelpers.findClass("android.app.NotificationManager", classLoader)
            XposedBridge.hookAllMethods(notificationManagerClass, "notify", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val tag = param.args[0] as? String ?: return
                    val notification = param.args[2] as? android.app.Notification ?: return
                    
                    // We only process WhatsApp message notifications (e.g. jid ending with @s.whatsapp.net or @g.us)
                    if (!tag.contains("@s.whatsapp.net") && !tag.contains("@g.us")) return
                    
                    val extras = notification.extras ?: return
                    val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
                    val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
                    
                    if (title.isEmpty() || text.isEmpty()) return
                    
                    // Filter duplicate triggers
                    if (isAlreadyProcessed(tag, text)) {
                        return
                    }
                    
                    XposedBridge.log("WaEnhancer AutoReply notification received: tag=$tag, title=$title, text=$text")
                    processAutoReply(tag, text)
                }
            })
            XposedBridge.log("WaEnhancer AutoReply: Notification hook registered successfully")
        } catch (e: Exception) {
            XposedBridge.log("WaEnhancer AutoReply Error: Notification hook failed: ${e.message}")
        }
    }

    private fun hookReceiveMessage() {
        try {
            val method = Unobfuscator.loadReceiptMethod(classLoader)

            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.args[4] == "sender" || param.args[1] == null || param.args[3] == null) return

                    val fMsg = FMessageWpp.Key(param.args[3]).fMessage ?: return
                    val userJid = fMsg.key.remoteJid

                    if (userJid.isStatus || fMsg.key.isFromMe) return

                    val messageText = fMsg.messageStr ?: return
                    if (TextUtils.isEmpty(messageText)) return

                    val rawJid = userJid.phoneRawString ?: return
                    if (isAlreadyProcessed(rawJid, messageText)) return

                    processAutoReply(rawJid, messageText, fMsg)
                }
            })
        } catch (e: Exception) {
            XposedBridge.log("WaEnhancer AutoReply Error: hookReceiveMessage failed: ${e.message}")
        }
    }

    private fun processAutoReply(rawJid: String, messageText: String, quoteMessage: FMessageWpp? = null) {
        val userJid = FMessageWpp.UserJid(rawJid)
        val number = userJid.phoneNumber ?: return
        val isGroup = rawJid.contains("@g.us")
        val isSavedContact = !WppCore.getSContactName(userJid, true).isNullOrEmpty()

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
                    "SPECIFIC_CONTACTS" -> {
                        val targetContacts = ruleObj.optString("targetContacts", "")
                        if (targetContacts.isEmpty()) continue
                        val allowedList = targetContacts.split(",").map { it.trim() }
                        if (!allowedList.contains(rawJid)) {
                            continue
                        }
                    }
                }

                // Time window filtering
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
                val ignoreCase = ruleObj.optBoolean("ignoreCase", true)
                val isMatched = checkKeywordMatch(messageText, keywordsStr, matchingType, ignoreCase)

                if (isMatched) {
                    val replyText = ruleObj.optString("replyText", "")
                    val delaySec = ruleObj.optInt("delaySeconds", 0)
                    val quoteOriginal = ruleObj.optBoolean("quoteOriginal", false)
                    val isForward = ruleObj.optBoolean("isForward", false)
                    val forwardJid = ruleObj.optString("forwardJid", "")
                    val isAi = ruleObj.optBoolean("isAi", false)

                    executor.execute {
                        val replyContent = if (isAi) {
                            val apiKey = prefs.getString("ai_api_key", "") ?: ""
                            val apiModel = prefs.getString("ai_model", "llama3-8b-8192") ?: "llama3-8b-8192"
                            val aiProvider = prefs.getString("ai_provider", "groq") ?: "groq"
                            if (apiKey.isNotEmpty()) {
                                queryAiChatbot(apiKey, messageText, apiModel, aiProvider) ?: "AI Responder failed to formulate reply."
                            } else {
                                "AI API Key is missing in settings."
                            }
                        } else {
                            replyText
                        }

                        mainHandler.postDelayed({
                            if (isForward && forwardJid.isNotEmpty()) {
                                val forwardText = "[Forwarded from +$number]: $messageText"
                                sendAutoReply(jid = forwardJid, replyText = forwardText, quoteMessage = null)
                            } else {
                                sendAutoReply(jid = rawJid, replyText = replyContent, quoteMessage = if (quoteOriginal) quoteMessage else null)
                            }
                        }, delaySec * 1000L)
                    }
                    break
                }
            }
        } catch (e: Exception) {
            XposedBridge.log("AutoReply Error in processAutoReply: ${e.message}")
        }
    }

    private fun checkKeywordMatch(message: String, keywordsCsv: String, matchingType: String, ignoreCase: Boolean): Boolean {
        val msgClean = if (ignoreCase) message.trim().lowercase() else message.trim()
        val keywords = keywordsCsv.split(",").map { if (ignoreCase) it.trim().lowercase() else it.trim() }.filter { it.isNotEmpty() }
        if (keywords.isEmpty()) return false

        for (keyword in keywords) {
            when (matchingType) {
                "EXACT" -> if (msgClean == keyword) return true
                "CONTAINS" -> if (msgClean.contains(keyword)) return true
                "WILDCARD" -> {
                    try {
                        val regexStr = "^" + Regex.escape(keyword).replace("\\*", ".*") + "$"
                        val flags = if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
                        val regex = Regex(regexStr, flags)
                        if (regex.matches(message.trim())) return true
                    } catch (_: Exception) {}
                }
                "REGEX" -> {
                    try {
                        val flags = if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
                        val regex = Regex(keyword, flags)
                        if (regex.containsMatchIn(message.trim())) return true
                    } catch (_: Exception) {}
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
                currentMinutes in startMinutes..endMinutes
            } else {
                currentMinutes >= startMinutes || currentMinutes <= endMinutes
            }
        } catch (e: Exception) {
            return true
        }
    }

    private fun queryAiChatbot(apiKey: String, messageText: String, model: String, provider: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val urlStr = when (provider) {
                "gemini" -> "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                "openai" -> "https://api.openai.com/v1/chat/completions"
                else -> "https://api.groq.com/openai/v1/chat/completions"
            }
            val url = URL(urlStr)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            if (provider != "gemini") {
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
            }

            val payload = if (provider == "gemini") {
                val partsObj = JSONObject().apply {
                    put("text", "You are an automated WhatsApp chat responder. Formulate a short, friendly, and contextual reply for this incoming message: $messageText")
                }
                val partsArray = JSONArray().apply {
                    put(partsObj)
                }
                val contentObj = JSONObject().apply {
                    put("parts", partsArray)
                }
                val contentsArray = JSONArray().apply {
                    put(contentObj)
                }
                JSONObject().apply {
                    put("contents", contentsArray)
                }
            } else {
                JSONObject().apply {
                    put("model", model)
                    val messages = JSONArray().apply {
                        val msg = JSONObject().apply {
                            put("role", "user")
                            put("content", "You are an automated WhatsApp chat responder. Formulate a short, friendly, and contextual reply for this incoming message: $messageText")
                        }
                        put(msg)
                    }
                    put("messages", messages)
                }
            }

            connection.outputStream.bufferedWriter().use { it.write(payload.toString()) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val responseJson = JSONObject(response)
                if (provider == "gemini") {
                    val candidates = responseJson.getJSONArray("candidates")
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    parts.getJSONObject(0).getString("text").trim()
                } else {
                    val choices = responseJson.getJSONArray("choices")
                    val choice = choices.getJSONObject(0)
                    val message = choice.getJSONObject("message")
                    message.getString("content").trim()
                }
            } else {
                val err = connection.errorStream.bufferedReader().use { it.readText() }
                XposedBridge.log("AutoReply AI Error Response: $err")
                null
            }
        } catch (e: Exception) {
            XposedBridge.log("AutoReply AI Exception: ${e.message}")
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun sendAutoReply(jid: String, replyText: String, quoteMessage: FMessageWpp?) {
        try {
            val userJid = WppCore.createUserJid(jid)
            if (userJid == null) return

            if (quoteMessage == null) {
                XposedBridge.log("WaEnhancer AutoReplyHook: Calling WppCore.sendMessageToJid")
                WppCore.sendMessageToJid(userJid, replyText)
                return
            }

            val actionUser = WppCore.getActionUser()
            val actionUserClass = WppCore.getActionUserClass()
            if (actionUser == null || actionUserClass == null) return

            var senderMethod = ReflectionUtils.findMethodUsingFilterIfExists(actionUserClass) { method ->
                List::class.java.isAssignableFrom(method.returnType) &&
                        ReflectionUtils.findIndexOfType(method.parameterTypes, String::class.java) != -1
            }
            if (senderMethod == null) {
                senderMethod = ReflectionUtils.findMethodUsingFilterIfExists(actionUserClass) { method ->
                    val params = method.parameterTypes
                    ReflectionUtils.findIndexOfType(params, List::class.java) != -1 &&
                            ReflectionUtils.findIndexOfType(params, String::class.java) != -1 &&
                            method.name != "toString"
                }
            }
            if (senderMethod == null) {
                senderMethod = ReflectionUtils.findMethodUsingFilterIfExists(actionUserClass) { method ->
                    val params = method.parameterTypes
                    val hasString = ReflectionUtils.findIndexOfType(params, String::class.java) != -1
                    val hasJid = params.any { param ->
                        param.name.endsWith("Jid", ignoreCase = true) || 
                        param == FMessageWpp.UserJid.TYPE_JID || 
                        param == FMessageWpp.UserJid.TYPE_USERJID ||
                        (param.isInterface && !param.name.startsWith("java.") && !param.name.startsWith("android.") && param.isAssignableFrom(FMessageWpp.UserJid.TYPE_JID))
                    }
                    hasString && hasJid && method.name != "toString"
                }
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

            val textIndex = ReflectionUtils.findIndexOfType(senderMethod.parameterTypes, String::class.java)
            newObject[textIndex] = replyText

            val jidIndex = ReflectionUtils.findIndexOfType(senderMethod.parameterTypes, List::class.java)
            if (jidIndex != -1) {
                newObject[jidIndex] = Collections.singletonList(userJid)
            } else {
                val indexJid = senderMethod.parameterTypes.indexOfFirst { param ->
                    param == FMessageWpp.UserJid.TYPE_JID || param == FMessageWpp.UserJid.TYPE_USERJID || param == FMessageWpp.UserJid.TYPE_PHONEUSERJID
                }
                if (indexJid != -1) {
                    val expectedType = senderMethod.parameterTypes[indexJid]
                    val wrapped = FMessageWpp.UserJid(userJid)
                    newObject[indexJid] = when (expectedType) {
                        FMessageWpp.UserJid.TYPE_USERJID -> wrapped.userJid ?: userJid
                        FMessageWpp.UserJid.TYPE_PHONEUSERJID -> wrapped.phoneJid ?: userJid
                        FMessageWpp.UserJid.TYPE_DEVICEJID -> wrapped.deviceJid ?: userJid
                        else -> userJid
                    }
                }
            }

            val msgIndex = ReflectionUtils.findIndexOfType(senderMethod.parameterTypes, FMessageWpp.TYPE)
            if (msgIndex != -1) {
                newObject[msgIndex] = quoteMessage.getObject()
            }

            senderMethod.invoke(actionUser, *newObject)
            XposedBridge.log("Auto Reply sent successfully to $jid")
        } catch (e: Exception) {
            XposedBridge.log("AutoReply Error sending reply: ${e.message}")
            e.printStackTrace()
        }
    }
}
