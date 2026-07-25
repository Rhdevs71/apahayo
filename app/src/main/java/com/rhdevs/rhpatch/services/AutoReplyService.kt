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

        Log.d(TAG, "Received message from $title on $packageName: $text")

        // Find reply action
        val replyAction = findReplyAction(notification) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(applicationContext).autoReplyRuleDao()
            val activeRules = db.getActiveRules()
            
            for (rule in activeRules) {
                if (isMatch(text, rule)) {
                    Log.d(TAG, "Matched rule: ${rule.keywords}")
                    sendReply(replyAction, rule.replyText)
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
        val remoteInput = remoteInputs.firstOrNull { it.allowFreeFormInput } ?: return

        val intent = Intent()
        val bundle = Bundle()
        bundle.putCharSequence(remoteInput.resultKey, replyText)
        RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, bundle)

        try {
            action.actionIntent.send(this, 0, intent)
            Log.d(TAG, "Auto-reply sent successfully: $replyText")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send auto-reply", e)
        }
    }
}
