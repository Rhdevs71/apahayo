package com.rhdevs.rhpatch.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Base64
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.rhdevs.rhpatch.scheduler.db.UniversalTaskEntity
import com.rhdevs.rhpatch.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import android.app.Notification
import android.app.RemoteInput
import com.rhdevs.rhpatch.database.AutoReplyRule

class AutoSenderAccessibilityService : AccessibilityService() {

    data class Task(val id: Int, val phone: String, val message: String, val targetApp: String = "whatsapp", val mediaPath: String? = null, val mediaType: String? = null)

    companion object {
        private const val TAG = "RhpatchAutoSender"
        var instance: AutoSenderAccessibilityService? = null
        private val taskQueue = ConcurrentLinkedQueue<Task>()
        private var isProcessing = false
        private var wakeLock: PowerManager.WakeLock? = null

        fun isServiceRunning(): Boolean = instance != null

        fun enqueueTask(phone: String, message: String) {
            Log.d(TAG, "Legacy Task enqueued for $phone")
            taskQueue.add(Task(-1, phone, message, "whatsapp"))
            processNextTask()
        }

        fun enqueueUniversalTask(id: Int, targetApp: String, contact: String, messageText: String, mediaPath: String? = null, mediaType: String? = null) {
            Log.d(TAG, "Universal Task enqueued for $contact on $targetApp")
            taskQueue.add(Task(id, contact, messageText, targetApp, mediaPath, mediaType))
            processNextTask()
        }

        private fun processNextTask() {
            if (isProcessing || taskQueue.isEmpty()) return
            val task = taskQueue.poll() ?: return
            
            isProcessing = true
            instance?.executeTask(task) ?: run {
                // If service is null, it's not enabled
                isProcessing = false
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var currentTask: Task? = null
    private var step = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Toast.makeText(this, "Layanan Aksesibilitas Rhpatch Terhubung", Toast.LENGTH_SHORT).show()
        processNextTask()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }
    
    private fun getSavedPin(): String {
        val prefs = getSharedPreferences("screen_lock_prefs", Context.MODE_PRIVATE)
        val encoded = prefs.getString("saved_pin", "") ?: ""
        if (encoded.isEmpty()) return ""
        return try {
            String(Base64.decode(encoded, Base64.DEFAULT))
        } catch (e: Exception) {
            ""
        }
    }

    private fun executeTask(task: Task) {
        currentTask = task
        step = 0
        wakeUpScreen()
        
        handler.postDelayed({
            val kgm = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            if (kgm.isKeyguardLocked) {
                step = 1 // Unlock Step
                performGlobalAction(GLOBAL_ACTION_HOME)
                handler.postDelayed({
                    swipeUpToUnlock()
                }, 1000)
            } else {
                step = 2 // Launch App Step
                launchTargetApp(task)
            }
        }, 1500)
    }

    private fun wakeUpScreen() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            pm.isInteractive
        } else {
            pm.isScreenOn
        }

        if (!isScreenOn) {
            @Suppress("DEPRECATION")
            wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "Rhpatch:AutoSenderWakeLock"
            )
            wakeLock?.acquire(10 * 60 * 1000L)
        }
    }
    
    private fun swipeUpToUnlock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val metrics = resources.displayMetrics
            val path = Path()
            val startY = metrics.heightPixels * 0.8f
            val endY = metrics.heightPixels * 0.2f
            val x = metrics.widthPixels / 2f
            path.moveTo(x, startY)
            path.lineTo(x, endY)
            
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
                .build()
                
            dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    handler.postDelayed({ typePin() }, 1000)
                }
            }, null)
        }
    }
    
    private fun typePin() {
        val pin = getSavedPin()
        if (pin.isEmpty()) {
            // Pin not set, assume Swipe-to-unlock was enough
            handler.postDelayed({
                step = 2
                launchTargetApp(currentTask!!)
            }, 1000)
            return
        }
        
        for (i in pin.indices) {
            val digit = pin[i]
            val delay = (i * 350).toLong()
            handler.postDelayed({
                val rootNode = rootInActiveWindow
                if (rootNode != null) {
                    clickNodeByText(rootNode, digit.toString())
                }
            }, delay)
        }

        // Wait for all digits to be clicked, then press Enter / OK if needed
        val totalDelay = (pin.length * 350 + 800).toLong()
        handler.postDelayed({
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                clickNodeByKeywords(rootNode, "ok", "enter", "done", "selesai", "confirm", "check")
            }
            handler.postDelayed({
                step = 2
                launchTargetApp(currentTask!!)
            }, 800)
        }, totalDelay)
    }

    private fun clickNodeByText(node: AccessibilityNodeInfo?, text: String): Boolean {
        if (node == null) return false
        val nodeText = node.text?.toString()?.trim() ?: ""
        val nodeDesc = node.contentDescription?.toString()?.trim() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        
        val isDigitMatch = nodeText == text || nodeDesc == text || 
                           viewId.endsWith("key_$text") || viewId.endsWith("key$text") || 
                           viewId.endsWith("digit_$text") || viewId.endsWith("digit$text")

        if (isDigitMatch) {
            var target: AccessibilityNodeInfo? = node
            while (target != null && !target.isClickable) {
                target = target.parent
            }
            if (target != null && target.isClickable) {
                target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
            
            // If neither is clickable directly, click bounds via gesture
            val rect = android.graphics.Rect()
            node.getBoundsInScreen(rect)
            if (rect.width() > 0 && rect.height() > 0) {
                val cx = rect.centerX().toFloat()
                val cy = rect.centerY().toFloat()
                clickAtCoordinates(cx, cy)
                return true
            }
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return true
        }

        for (i in 0 until node.childCount) {
            if (clickNodeByText(node.getChild(i), text)) return true
        }
        return false
    }

    private fun clickNodeByKeywords(node: AccessibilityNodeInfo?, vararg keywords: String): Boolean {
        if (node == null) return false
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        for (kw in keywords) {
            val key = kw.lowercase()
            if (desc == key || text == key || viewId.contains(key)) {
                var target: AccessibilityNodeInfo? = node
                while (target != null && !target.isClickable) {
                    target = target.parent
                }
                if (target != null && target.isClickable) {
                    target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return true
                }
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
        }

        for (i in 0 until node.childCount) {
            if (clickNodeByKeywords(node.getChild(i), *keywords)) return true
        }
        return false
    }

    private fun clickAtCoordinates(x: Float, y: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path()
            path.moveTo(x, y)
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
                .build()
            dispatchGesture(gesture, null, null)
        }
    }

    private fun launchTargetApp(task: Task) {
        if (task.mediaPath != null) {
            val fileUri = Uri.parse(task.mediaPath)
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = if (task.mediaType == "IMAGE") "image/*" else if (task.mediaType == "VIDEO") "video/*" else if (task.mediaType == "AUDIO") "audio/*" else "*/*"
            intent.putExtra(Intent.EXTRA_STREAM, fileUri)
            if (task.message.isNotEmpty()) {
                intent.putExtra(Intent.EXTRA_TEXT, task.message)
            }
            val phoneJid = if (task.phone.contains("@")) task.phone else "${task.phone}@s.whatsapp.net"
            when (task.targetApp) {
                "whatsapp" -> {
                    intent.setPackage("com.whatsapp")
                    intent.putExtra("jid", phoneJid)
                }
                "whatsapp_business" -> {
                    intent.setPackage("com.whatsapp.w4b")
                    intent.putExtra("jid", phoneJid)
                }
                "telegram" -> intent.setPackage("org.telegram.messenger")
                "messenger" -> intent.setPackage("com.facebook.orca")
                // For direct targets using ACTION_SEND, some apps might require chooser or specific package
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            try {
                startActivity(intent)
                handler.postDelayed(timeoutRunnable, 15000)
            } catch (e: Exception) {
                finishTask(false)
            }
            return
        }

        val intent = when (task.targetApp) {
            "whatsapp", "whatsapp_business" -> {
                val pkg = if (task.targetApp == "whatsapp_business") "com.whatsapp.w4b" else "com.whatsapp"
                val i = Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://send?phone=${task.phone}&text=${Uri.encode(task.message)}"))
                i.setPackage(pkg)
                i
            }
            "telegram" -> {
                val encodedMsg = Uri.encode(task.message)
                val cleanPhone = task.phone.replace(" ", "")
                if (cleanPhone.matches(Regex("^[0-9+\\-]+$"))) {
                    Intent(Intent.ACTION_VIEW, Uri.parse("tg://msg?to=$cleanPhone&text=$encodedMsg"))
                } else {
                    val username = if (cleanPhone.startsWith("@")) cleanPhone.substring(1) else cleanPhone
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$username?text=$encodedMsg"))
                }
            }
            "telegram_group" -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=${task.phone}"))
            }
            "sms" -> {
                try {
                    val smsManager = android.telephony.SmsManager.getDefault()
                    smsManager.sendTextMessage(task.phone, null, task.message, null, null)
                    finishTask(true)
                    return
                } catch (e: Exception) {
                    Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${task.phone}")).apply {
                        putExtra("sms_body", task.message)
                    }
                }
            }
            "call" -> {
                try {
                    val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${task.phone}"))
                    callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(callIntent)
                    handler.postDelayed({ finishTask(true) }, 3000)
                    return
                } catch (e: Exception) {
                    return
                }
            }
            "email" -> {
                var subject = "Automated Message"
                var body = task.message
                if (task.message.contains("|||")) {
                    val parts = task.message.split("|||", limit = 2)
                    subject = parts[0]
                    body = parts[1]
                }
                val uriString = "mailto:${task.phone}?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"
                Intent(Intent.ACTION_SENDTO, Uri.parse(uriString))
            }
            "messenger" -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("fb-messenger://user-thread/${task.phone}"))
            }
            "instagram" -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://ig.me/m/${task.phone}"))
            }
            "discord" -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.com/users/${task.phone}"))
            }
            else -> return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        try {
            startActivity(intent)
            handler.postDelayed(timeoutRunnable, 15000)
        } catch (e: Exception) {
            finishTask(false)
        }
    }

    private val timeoutRunnable = Runnable {
        if (step in 1..3) {
            finishTask(false)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            val data = event.parcelableData
            if (data is Notification) {
                val packageName = event.packageName?.toString() ?: return
                val extras = data.extras
                val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
                val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
                if (text.isNotEmpty() && title.isNotEmpty()) {
                    handleAutoReplyViaNotification(packageName, title, text, data)
                }
            }
        }

        if (currentTask == null) return

        val packageName = event.packageName?.toString() ?: ""

        if (step == 2 || step == 3) {
            when (currentTask!!.targetApp) {
                "whatsapp" -> if (packageName.contains("whatsapp")) handleWhatsApp(event)
                "telegram", "telegram_group" -> if (packageName.contains("org.telegram.messenger") || packageName.contains("telegram")) handleTelegram(event)
                "sms" -> if (packageName.contains("mms") || packageName.contains("messaging")) handleSMS(event)
                "email" -> if (packageName.contains("gm") || packageName.contains("email")) handleEmail(event)
                "messenger" -> if (packageName.contains("orca") || packageName.contains("facebook")) handleFacebookMessenger(event)
                "instagram" -> if (packageName.contains("instagram")) handleInstagram(event)
                "discord" -> if (packageName.contains("discord")) handleDiscord(event)
            }
        }
    }
    
    private fun handleWhatsApp(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            handler.removeCallbacks(timeoutRunnable)
            handler.postDelayed({
                step = 3
                findAndClickSendButton("com.whatsapp:id/send")
            }, 1000)
        } else if (step == 3 && event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            findAndClickSendButton("com.whatsapp:id/send")
        }
    }
    
    private fun handleTelegram(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            handler.removeCallbacks(timeoutRunnable)
            handler.postDelayed({
                step = 3
                val root = rootInActiveWindow ?: return@postDelayed
                val inputFields = root.findAccessibilityNodeInfosByViewId("org.telegram.messenger:id/chat_message_edit")
                if (inputFields.isNotEmpty()) {
                    val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, currentTask!!.message) }
                    inputFields[0].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                }
                handler.postDelayed({ 
                    findAndClickSendButton("org.telegram.messenger:id/chat_send_button") 
                    handler.postDelayed({
                        if (step == 3) findAndClickSendButtonByDescOrText("send", "kirim")
                    }, 500)
                }, 500)
            }, 1500)
        } else if (step == 3 && event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
             findAndClickSendButton("org.telegram.messenger:id/chat_send_button")
             if (step == 3) findAndClickSendButtonByDescOrText("send", "kirim")
        }
    }

    private fun handleSMS(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            handler.removeCallbacks(timeoutRunnable)
            handler.postDelayed({
                step = 3
                // In generic SMS apps, the send button id varies. We'll search by description/text.
                findAndClickSendButtonByDescOrText("send", "kirim")
            }, 1500)
        } else if (step == 3 && event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
             findAndClickSendButtonByDescOrText("send", "kirim")
        }
    }

    private fun handleEmail(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            handler.removeCallbacks(timeoutRunnable)
            handler.postDelayed({
                step = 3
                // Gmail send button usually has contentDescription "Send" or id "send"
                findAndClickSendButton("com.google.android.gm:id/send")
                handler.postDelayed({
                    if (step == 3) findAndClickSendButtonByDescOrText("send", "kirim")
                }, 500)
            }, 1500)
        } else if (step == 3 && event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
             findAndClickSendButton("com.google.android.gm:id/send")
             if (step == 3) findAndClickSendButtonByDescOrText("send", "kirim")
        }
    }

    private fun handleFacebookMessenger(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            handler.removeCallbacks(timeoutRunnable)
            handler.postDelayed({
                step = 3
                val root = rootInActiveWindow ?: return@postDelayed
                findAndSetTextByContentDescOrText(root, currentTask!!.message, "Type a message", "Tulis pesan", "message", "pesan")
                handler.postDelayed({ findAndClickSendButtonByDescOrText("send", "kirim", "kirim pesan") }, 1000)
            }, 1500)
        } else if (step == 3 && event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            findAndClickSendButtonByDescOrText("send", "kirim", "kirim pesan")
        }
    }

    private fun handleInstagram(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            handler.removeCallbacks(timeoutRunnable)
            handler.postDelayed({
                step = 3
                val root = rootInActiveWindow ?: return@postDelayed
                findAndSetTextByContentDescOrText(root, currentTask!!.message, "Message", "Pesan", "Send message", "Kirim pesan")
                handler.postDelayed({ findAndClickSendButtonByDescOrText("send", "kirim") }, 1000)
            }, 1500)
        } else if (step == 3 && event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            findAndClickSendButtonByDescOrText("send", "kirim")
        }
    }

    private fun handleDiscord(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            handler.removeCallbacks(timeoutRunnable)
            handler.postDelayed({
                step = 3
                val root = rootInActiveWindow ?: return@postDelayed
                findAndSetTextByContentDescOrText(root, currentTask!!.message, "Message", "Pesan", "Send", "Kirim")
                handler.postDelayed({ findAndClickSendButtonByDescOrText("send", "kirim") }, 1000)
            }, 1500)
        } else if (step == 3 && event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            findAndClickSendButtonByDescOrText("send", "kirim")
        }
    }

    private fun findAndSetTextByContentDescOrText(node: AccessibilityNodeInfo, textToSet: String, vararg keywords: String): Boolean {
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""
        
        for (keyword in keywords) {
            if (desc.contains(keyword.lowercase()) || text.contains(keyword.lowercase())) {
                val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToSet) }
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                // Force an accessibility focus to sometimes trigger UI update
                node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
                return true
            }
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null && findAndSetTextByContentDescOrText(child, textToSet, *keywords)) return true
        }
        return false
    }

    private fun findAndClickSendButton(viewId: String) {
        if (step != 3) return
        val rootNode = rootInActiveWindow ?: return

        val sendNodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
        if (sendNodes.isNotEmpty()) {
            for (node in sendNodes) {
                if (node.isClickable || node.contentDescription?.toString()?.contains("Send", true) == true || node.contentDescription?.toString()?.contains("Kirim", true) == true) {
                    step = 4 // Prevent looping
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    handler.postDelayed({
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        finishTask(true)
                    }, 1000)
                    return
                }
            }
        }
    }

    private fun findAndClickSendButtonByDescOrText(vararg keywords: String) {
        if (step != 3) return
        var retries = 0
        val maxRetries = 5
        
        val searchRunnable = object : Runnable {
            override fun run() {
                if (step != 3) return
                val rootNode = rootInActiveWindow
                if (rootNode == null) {
                    if (retries < maxRetries) {
                        retries++
                        handler.postDelayed(this, 1000)
                    }
                    return
                }

                fun searchNode(node: AccessibilityNodeInfo): Boolean {
                    val desc = node.contentDescription?.toString()?.lowercase() ?: ""
                    val text = node.text?.toString()?.lowercase() ?: ""
                    
                    for (keyword in keywords) {
                        val key = keyword.lowercase()
                        if (desc == key || text == key || node.viewIdResourceName?.lowercase()?.contains(key) == true || desc.contains(key) || text.contains(key)) {
                            var clickableNode: AccessibilityNodeInfo? = node
                            while (clickableNode != null && !clickableNode.isClickable) {
                                clickableNode = clickableNode.parent
                            }
                            // Even if not technically "clickable", try to click the node itself if parent fails
                            val targetNode = clickableNode ?: node
                            step = 4 // Prevent looping
                            targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            handler.postDelayed({
                                performGlobalAction(GLOBAL_ACTION_HOME)
                                finishTask(true)
                            }, 1000)
                            return true
                        }
                    }
                    
                    for (i in 0 until node.childCount) {
                        val child = node.getChild(i)
                        if (child != null && searchNode(child)) return true
                    }
                    return false
                }
                
                if (!searchNode(rootNode)) {
                    if (retries < maxRetries) {
                        retries++
                        handler.postDelayed(this, 1000) // retry every 1 second if not found
                    } else {
                        // Max retries reached, could not find send button
                        finishTask(false)
                    }
                }
            }
        }
        
        handler.post(searchRunnable)
    }

    private fun finishTask(success: Boolean) {
        if (!isProcessing) return
        val taskId = currentTask?.id ?: -1
        val statusStr = if (success) "SUCCESS" else "FAILED"
        
        // Update database
        if (taskId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getInstance(applicationContext).universalSchedulerDao()
                val entity = db.getAllTasks().find { it.id == taskId }
                if (entity != null) {
                    db.updateTask(entity.copy(status = statusStr))
                }
            }
        }

        isProcessing = false
        currentTask = null
        step = 0
        handler.removeCallbacks(timeoutRunnable)
        
        // Release wakelock
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {}
        
        // Send Broadcast to close AutomationCountdownActivity
        val intent = Intent("com.rhdevs.rhpatch.AUTOMATION_COMPLETE")
        sendBroadcast(intent)
        
        // Remove device lock (user request)
        // try {
        //     val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        //     val adminComponent = android.content.ComponentName(this, com.rhdevs.rhpatch.receivers.WaDeviceAdminReceiver::class.java)
        //     if (dpm.isAdminActive(adminComponent)) {
        //         dpm.lockNow()
        //     }
        // } catch (e: Exception) {}
        
        processNextTask()
    }

    private val SUPPORTED_APPS = listOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
        "com.facebook.orca",
        "com.instagram.android",
        "com.discord"
    )

    private fun handleAutoReplyViaNotification(packageName: String, title: String, text: String, notification: Notification) {
        if (!SUPPORTED_APPS.contains(packageName)) return

        val replyAction = findReplyAction(notification) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(applicationContext).autoReplyRuleDao()
            val activeRules = db.getActiveRules()
            
            for (rule in activeRules) {
                if (isMatch(text, rule)) {
                    Log.d(TAG, "Matched rule: ${rule.keywords}")
                    val senderId = "$packageName:$title"
                    val replyMsg = processAiIfNeeded(rule.replyText, rule, text, senderId)
                    sendReply(replyAction, replyMsg)
                    break 
                }
            }
        }
    }

    private fun processAiIfNeeded(originalReply: String, rule: AutoReplyRule, incomingText: String, senderId: String): String {
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
            return aiResponse ?: "AI Responder failed to formulate reply."
        } else if (rule.replyType == "RANDOM") {
            val options = originalReply.split("|||")
            return options.random()
        }
        return originalReply
    }

    private fun queryAiChatbot(apiKey: String, messageText: String, model: String, provider: String, jid: String, userInstruction: String?): String? {
        var connection: java.net.HttpURLConnection? = null
        return try {
            val aiDb = com.rhdevs.rhpatch.database.AiMemoryDatabase.getInstance(applicationContext)
            val dao = aiDb.aiMemoryDao()
            
            val sysInstruction = if (!userInstruction.isNullOrEmpty()) userInstruction else "Anda adalah asisten AI pribadi yang ramah, sopan, dan menganggap diri Anda sebagai manusia nyata. Bahasa Anda tidak kaku, menggunakan gaya kata sehari-hari namun tetap sopan dan membantu. Jawablah pesan pengguna dengan natural."

            dao.insertMemory(com.rhdevs.rhpatch.database.AiMemoryEntity(jid = jid, role = "user", messageText = messageText, timestamp = System.currentTimeMillis()))
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
                    dao.insertMemory(com.rhdevs.rhpatch.database.AiMemoryEntity(jid = jid, role = "model", messageText = reply, timestamp = System.currentTimeMillis()))
                    if (dao.getMemoryCount(jid) > 15) {
                        dao.deleteOldestMemory(jid)
                    }
                }
                reply
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
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
                "STARTS_WITH" -> if (textToMatch.startsWith(kw)) return true
                "ENDS_WITH" -> if (textToMatch.endsWith(kw)) return true
                "REGEX" -> {
                    try {
                        val regex = if (rule.ignoreCase) Regex(kw, RegexOption.IGNORE_CASE) else Regex(kw)
                        if (regex.containsMatchIn(textToMatch)) return true
                    } catch (e: Exception) { }
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
            Log.d(TAG, "Auto-reply sent via AccessibilityService: $replyText")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send auto-reply", e)
        }
    }

    override fun onInterrupt() {
        isProcessing = false
        currentTask = null
        wakeLock?.release()
    }
}
