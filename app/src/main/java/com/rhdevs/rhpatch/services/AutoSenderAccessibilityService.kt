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
import java.util.concurrent.ConcurrentLinkedQueue

class AutoSenderAccessibilityService : AccessibilityService() {

    data class Task(val phone: String, val message: String, val targetApp: String = "whatsapp")

    companion object {
        private const val TAG = "WaEnhancerAccessibility"
        private var instance: AutoSenderAccessibilityService? = null
        private val taskQueue = ConcurrentLinkedQueue<Task>()
        private var isProcessing = false
        private var wakeLock: PowerManager.WakeLock? = null

        fun enqueueTask(phone: String, message: String) {
            Log.d(TAG, "Task enqueued for $phone")
            taskQueue.add(Task(phone, message, "whatsapp"))
            processNextTask()
        }

        fun enqueueUniversalTask(id: Int, targetApp: String, contact: String, messageText: String) {
            Log.d(TAG, "Universal Task enqueued for $contact on $targetApp")
            taskQueue.add(Task(contact, messageText, targetApp))
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
        Toast.makeText(this, "Rhpatch Universal Auto Sender Connected", Toast.LENGTH_SHORT).show()
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
            wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "WaEnhancer:AutoSenderWakeLock"
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
            Toast.makeText(this, "PIN belum dikonfigurasi!", Toast.LENGTH_SHORT).show()
            finishTask()
            return
        }
        
        val root = rootInActiveWindow
        if (root != null) {
            val pinField = root.findAccessibilityNodeInfosByViewId("com.android.systemui:id/pinEntry")
            if (pinField.isNotEmpty()) {
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, pin)
                }
                pinField[0].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                performGlobalAction(GLOBAL_ACTION_HOME)
                handler.postDelayed({
                    step = 2
                    launchTargetApp(currentTask!!)
                }, 2000)
                return
            }
        }
        
        handler.postDelayed({
            step = 2
            launchTargetApp(currentTask!!)
        }, 3000)
    }

    private fun launchTargetApp(task: Task) {
        val intent = when (task.targetApp) {
            "whatsapp" -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://send?phone=${task.phone}&text=${Uri.encode(task.message)}"))
            }
            "telegram" -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=${task.phone}")).apply {
                    putExtra("android.intent.extra.TEXT", task.message)
                }
            }
            "sms" -> {
                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${task.phone}")).apply {
                    putExtra("sms_body", task.message)
                }
            }
            "email" -> {
                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${task.phone}")).apply {
                    putExtra(Intent.EXTRA_SUBJECT, "Automated Message")
                    putExtra(Intent.EXTRA_TEXT, task.message)
                }
            }
            else -> return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        try {
            startActivity(intent)
            handler.postDelayed(timeoutRunnable, 15000)
        } catch (e: Exception) {
            finishTask()
        }
    }

    private val timeoutRunnable = Runnable {
        if (isProcessing) {
            finishTask()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !isProcessing || currentTask == null) return

        val packageName = event.packageName?.toString() ?: ""

        if (step == 2) {
            if (currentTask!!.targetApp == "whatsapp" && packageName.contains("whatsapp")) {
                handleWhatsApp(event)
            } else if (currentTask!!.targetApp == "telegram" && packageName.contains("org.telegram.messenger")) {
                handleTelegram(event)
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
                handler.postDelayed({ findAndClickSendButton("org.telegram.messenger:id/chat_send_button") }, 500)
            }, 1500)
        } else if (step == 3 && event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
             findAndClickSendButton("org.telegram.messenger:id/chat_send_button")
        }
    }

    private fun findAndClickSendButton(viewId: String) {
        if (step != 3) return
        val rootNode = rootInActiveWindow ?: return

        val sendNodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
        if (sendNodes.isNotEmpty()) {
            for (node in sendNodes) {
                if (node.isClickable || node.contentDescription?.contains("Send") == true || node.contentDescription?.contains("Kirim") == true) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    handler.postDelayed({
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        finishTask()
                    }, 1000)
                    return
                }
            }
        }
    }

    private fun closeWhatsApp() {
        // Press BACK button globally
        performGlobalAction(GLOBAL_ACTION_BACK)
        handler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_HOME)
            finishTask()
        }, 500)
    }

    private fun finishTask() {
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
        
        // Lock screen using Device Admin
        try {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
            if (dpm.isAdminActive(android.content.ComponentName(this, com.rhdevs.rhpatch.receivers.WaDeviceAdminReceiver::class.java))) {
                dpm.lockNow()
            }
        } catch (e: Exception) {}
        
        processNextTask()
    }

    override fun onInterrupt() {
        isProcessing = false
        currentTask = null
        wakeLock?.release()
    }
}
