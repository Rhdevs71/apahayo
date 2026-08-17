package com.rhdevs.rhpatch.services

import android.app.Notification
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.wmods.wppenhacer.database.AppDatabase
import com.wmods.wppenhacer.database.AutoReplyRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutoReplyService : NotificationListenerService() {

    companion object {
        private const val TAG = "RhpatchAutoReply"
        // List of supported messaging apps for auto-reply
        private val SUPPORTED_APPS = listOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
            "com.facebook.orca",
            "com.instagram.android",
            "com.discord"
        )
        // Cache to prevent duplicate replies for the same message
        private val lastProcessedMessages = java.util.concurrent.ConcurrentHashMap<String, Long>()
        // Per-sender cooldown to prevent rapid looping
        private val lastSenderProcessTime = java.util.concurrent.ConcurrentHashMap<String, Long>()
        // Cache to store recent sent replies to avoid replying to our own messages
        private val recentSentReplies = java.util.concurrent.ConcurrentHashMap<String, Long>()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        if (!SUPPORTED_APPS.contains(packageName)) return

        val notification = sbn.notification
        val extras = notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""

        if (text.isEmpty() || title.isEmpty()) return

        // Clean up old sent replies to avoid memory leaks
        val now = System.currentTimeMillis()
        recentSentReplies.entries.removeIf { now - it.value > 60000 }

        // Cek apakah pesan ini sebenarnya adalah pesan yang baru kita kirim (AI membalas dirinya sendiri).
        // Kita menggunakan endsWith karena notifikasi WA bisa berisi riwayat pesan. Jika diakhiri pesan kita, berarti itu pantulan (echo).
        if (recentSentReplies.keys.any { it.isNotBlank() && text.trim().endsWith(it.trim()) }) {
            return
        }

        val senderId = "$packageName:$title"
        val messageHash = "$senderId:$text"
        
        // Cooldown per pengirim (3 detik) untuk menghindari AI spam beruntun
        val lastSenderTime = lastSenderProcessTime[senderId] ?: 0L
        if (now - lastSenderTime < 3000) {
            return
        }

        // Debounce: prevent same exact message from triggering AI multiple times within 10 seconds
        val lastTime = lastProcessedMessages[messageHash] ?: 0L
        if (now - lastTime < 10000) {
            return
        }

        Log.d(TAG, "Received message from $title on $packageName: $text")

        // Find reply action
        val replyAction = findReplyAction(notification) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(applicationContext).autoReplyRuleDao()
            val activeRules = db.getActiveRules()
            
            for (rule in activeRules) {
                if (isMatch(text, rule)) {
                    // Mark as processed only if it matches a rule to avoid filling memory with ignored messages
                    lastProcessedMessages[messageHash] = System.currentTimeMillis()
                    lastSenderProcessTime[senderId] = System.currentTimeMillis()
                    
                    Log.d(TAG, "Matched rule: ${rule.keywords}")
                    val replyMsg = processAiIfNeeded(rule.replyText, rule, text, senderId)
                    if (replyMsg != null) {
                        if (replyMsg.isNotBlank()) {
                            recentSentReplies[replyMsg] = System.currentTimeMillis()
                        }
                        sendReply(replyAction, replyMsg)
                    }
                    break // Only reply once per message
                }
            }
        }
    }

    private fun isMatch(incomingText: String, rule: AutoReplyRule): Boolean {
        val keywords = rule.keywords.split(",").map { it.trim() }
        val textToMatch = if (rule.ignoreCase) incomingText.lowercase() else incomingText
        
        for (keyword in keywords) {
            val kw = if (rule.ignoreCase) keyword.lowercase() else keyword
            if (kw.isEmpty()) continue
            
            when (rule.matchingType) {
                "EXACT" -> if (textToMatch == kw) return true
                "CONTAINS" -> if (textToMatch.contains(kw)) return true
                "REGEX" -> {
                    try {
                        val regex = if (rule.ignoreCase) Regex(kw, RegexOption.IGNORE_CASE) else Regex(kw)
                        if (regex.containsMatchIn(textToMatch)) return true
                    } catch (e: Exception) {
                        Log.e(TAG, "Invalid Regex: $kw")
                    }
                }
            }
        }
        return false
    }

    private fun findReplyAction(notification: Notification): Notification.Action? {
        val actions = notification.actions ?: return null
        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            for (remoteInput in remoteInputs) {
                // Usually the reply action has a remote input that accepts free form text
                if (remoteInput.allowFreeFormInput) {
                    return action
                }
            }
        }
        return null
    }

    private fun sendReply(action: Notification.Action, replyText: String) {
        val remoteInputs = action.remoteInputs ?: return
        var remoteInput: RemoteInput? = null
        for (ri in remoteInputs) {
            if (ri.allowFreeFormInput) {
                remoteInput = ri
                break
            }
        }
        if (remoteInput == null) return

        val bundle = Bundle()
        bundle.putCharSequence(remoteInput.resultKey, replyText)

        val intent = Intent()
        RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, bundle)
        try {
            action.actionIntent.send(this, 0, intent)
            Log.d(TAG, "Auto-reply sent successfully: $replyText")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending auto-reply", e)
        }
    }

    private fun processAiIfNeeded(originalReply: String, rule: AutoReplyRule, incomingText: String, senderId: String): String? {
        if (rule.replyType == "AI" || rule.isAi) {
            val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val apiKeysRaw = prefs.getString("ai_api_key", "") ?: ""
            val apiKeys = apiKeysRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val apiModel = prefs.getString("ai_model", "llama3-8b-8192") ?: "llama3-8b-8192"
            val fallbackProvider = prefs.getString("ai_provider", "groq") ?: "groq"
            val aiProvider = if (!rule.aiProvider.isNullOrEmpty()) rule.aiProvider else fallbackProvider
            
            var aiResponse: String? = null
            for (key in apiKeys) {
                aiResponse = queryAiChatbot(key, incomingText, apiModel, aiProvider, senderId, originalReply)
                if (aiResponse != null) break
            }
            return aiResponse
        } else if (rule.replyType == "RANDOM") {
            val options = originalReply.split("|||")
            return options.random()
        }
        return originalReply
    }

    private fun queryAiChatbot(apiKey: String, messageText: String, model: String, provider: String, jid: String, userInstruction: String?): String? {
        var connection: java.net.HttpURLConnection? = null
        return try {
            val aiDb = com.wmods.wppenhacer.database.AiMemoryDatabase.getInstance(applicationContext)
            val dao = aiDb.aiMemoryDao()
            
            val sysInstruction = if (!userInstruction.isNullOrEmpty()) userInstruction else "Anda adalah asisten AI pribadi yang ramah, sopan, dan menganggap diri Anda sebagai manusia nyata. Bahasa Anda tidak kaku, menggunakan gaya kata sehari-hari namun tetap sopan dan membantu. Jawablah pesan pengguna dengan natural."

            dao.insertMemory(com.wmods.wppenhacer.database.AiMemoryEntity(jid = jid, role = "user", messageText = messageText, timestamp = System.currentTimeMillis()))
            if (dao.getMemoryCount(jid) > 15) {
                dao.deleteOldestMemory(jid)
            }
            
            val memories = dao.getMemoriesByJid(jid)
            
            val urlStr = when (provider) {
                "gemini" -> "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                "openai" -> "https://api.openai.com/v1/chat/completions"
                else -> "https://api.groq.com/openai/v1/chat/completions"
            }
            val url = java.net.URL(urlStr)
            connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            if (provider != "gemini") {
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
            }

            val payload = if (provider == "gemini") {
                val payloadObj = org.json.JSONObject()
                payloadObj.put("system_instruction", org.json.JSONObject().apply {
                    put("parts", org.json.JSONArray().apply { put(org.json.JSONObject().apply { put("text", sysInstruction) }) })
                })
                
                val contentsArray = org.json.JSONArray()
                for (mem in memories) {
                    contentsArray.put(org.json.JSONObject().apply {
                        put("role", if (mem.role == "user") "user" else "model")
                        put("parts", org.json.JSONArray().apply { put(org.json.JSONObject().apply { put("text", mem.messageText) }) })
                    })
                }
                payloadObj.put("contents", contentsArray)
                payloadObj
            } else {
                org.json.JSONObject().apply {
                    put("model", model)
                    val messages = org.json.JSONArray()
                    messages.put(org.json.JSONObject().apply {
                        put("role", "system")
                        put("content", sysInstruction)
                    })
                    for (mem in memories) {
                        messages.put(org.json.JSONObject().apply {
                            put("role", if (mem.role == "user") "user" else "assistant")
                            put("content", mem.messageText)
                        })
                    }
                    put("messages", messages)
                }
            }

            connection.outputStream.bufferedWriter().use { it.write(payload.toString()) }

            val responseCode = connection.responseCode
            if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val responseJson = org.json.JSONObject(response)
                val reply = if (provider == "gemini") {
                    val candidates = responseJson.getJSONArray("candidates")
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    parts.getJSONObject(0).getString("text").trim()
                } else {
                    val choices = responseJson.getJSONArray("choices")
                    val choice = choices.getJSONObject(0)
                    val messageObj = choice.getJSONObject("message")
                    messageObj.getString("content").trim()
                }
                
                if (reply.isNotEmpty()) {
                    dao.insertMemory(com.wmods.wppenhacer.database.AiMemoryEntity(jid = jid, role = "model", messageText = reply, timestamp = System.currentTimeMillis()))
                    if (dao.getMemoryCount(jid) > 15) {
                        dao.deleteOldestMemory(jid)
                    }
                }
                reply
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(applicationContext, "Rhpatch AI Error: HTTP $responseCode\n$errorStream", android.widget.Toast.LENGTH_LONG).show()
                }
                null
            }
        } catch (e: Exception) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(applicationContext, "Rhpatch AI Exception: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
            null
        } finally {
            connection?.disconnect()
        }
    }
}
