package com.rhdevs.rhpatch.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import java.util.concurrent.ConcurrentLinkedQueue

class AutoSenderAccessibilityService : AccessibilityService() {

    data class SendTask(val phone: String, val message: String)

    companion object {
        private var instance: AutoSenderAccessibilityService? = null
        private val taskQueue = ConcurrentLinkedQueue<SendTask>()
        private var isProcessing = false
        private var wakeLock: PowerManager.WakeLock? = null

        fun enqueueTask(phone: String, message: String) {
            taskQueue.add(SendTask(phone, message))
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
    private var currentTask: SendTask? = null
    private var step = 0 // 0: Wake/Unlock, 1: Launch WA, 2: Click Send, 3: Close

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Toast.makeText(this, "WaEnhancer Auto Sender Connected", Toast.LENGTH_SHORT).show()
        processNextTask()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    private fun executeTask(task: SendTask) {
        currentTask = task
        step = 0
        
        // Step 0: Wake up screen
        wakeUpScreen()
        
        // Wait for screen to turn on, then launch WhatsApp
        handler.postDelayed({
            step = 1
            launchWhatsApp(task)
        }, 1000)
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
            wakeLock?.acquire(10 * 60 * 1000L /*10 minutes*/)
            
            // TODO: Implement PIN/Swipe to unlock if necessary using gestures
            // For now, assuming no secure lock screen or user handles it
        }
    }

    private fun launchWhatsApp(task: SendTask) {
        val uri = Uri.parse("whatsapp://send?phone=${task.phone}&text=${Uri.encode(task.message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        
        // Fail-safe to reset processing if WhatsApp doesn't open
        handler.postDelayed(timeoutRunnable, 10000)
    }

    private val timeoutRunnable = Runnable {
        if (isProcessing) {
            finishTask()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !isProcessing || currentTask == null) return

        val packageName = event.packageName?.toString() ?: ""

        if (packageName.contains("whatsapp")) {
            if (step == 1 && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                // Wait a bit for the chat to fully load
                handler.removeCallbacks(timeoutRunnable)
                handler.postDelayed({
                    step = 2
                    findAndClickSendButton()
                }, 1000)
            } else if (step == 2 && event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                // Sometimes the button appears slightly later
                findAndClickSendButton()
            }
        }
    }

    private fun findAndClickSendButton() {
        if (step != 2) return
        
        val rootNode = rootInActiveWindow ?: return
        
        // Find send button. WhatsApp uses content description "Send" or ID "send"
        var sendButton: AccessibilityNodeInfo? = null
        
        val nodesById = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
        if (nodesById.isNotEmpty()) {
            sendButton = nodesById[0]
        } else {
            val nodesByDesc = rootNode.findAccessibilityNodeInfosByText("Send")
            if (nodesByDesc.isNotEmpty()) {
                sendButton = nodesByDesc[0]
            }
        }

        if (sendButton != null && sendButton.isClickable) {
            sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            step = 3
            
            // Wait for message to be sent, then close
            handler.postDelayed({
                closeWhatsApp()
            }, 1000)
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
