package com.wmods.wppenhacer.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.wmods.wppenhacer.BuildConfig
import com.wmods.wppenhacer.database.AppDatabase
import com.wmods.wppenhacer.database.SchedulerHelper
import com.wmods.wppenhacer.database.ScheduledMessage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SchedulerService : Service() {

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val channelId = "scheduler_service"
    private var isProcessing = false

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getIntExtra("id", -1)
            val success = intent.getBooleanExtra("success", false)
            if (id != -1) {
                updateMessageStatus(id, success)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = createNotification("Processing scheduled messages...")
        startForeground(1122, notification)

        // Register status receiver from Xposed WhatsApp hook
        val filter = IntentFilter("com.wmods.wppenhacer.SCHEDULED_STATUS")
        ContextCompat.registerReceiver(
            this,
            statusReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isProcessing) {
            isProcessing = true
            executor.execute {
                processPendingMessages()
            }
        }
        return START_NOT_STICKY
    }

    private fun processPendingMessages() {
        val context = this
        val db = AppDatabase.getInstance(context)
        val now = System.currentTimeMillis()
        // Fetch all pending messages that should run now or in the past
        val pendingMessages = db.scheduledMessageDao().getPendingBefore(now)

        if (pendingMessages.isEmpty()) {
            stopService()
            return
        }

        for (message in pendingMessages) {
            sendMessageToWhatsApp(message)
            // Wait up to 5 seconds for status feedback from WhatsApp broadcast
            try {
                Thread.sleep(5000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        stopService()
    }

    private fun sendMessageToWhatsApp(message: ScheduledMessage) {
        // Send intent broadcast to hooked WhatsApp receiver
        val intent = Intent("com.wmods.wppenhacer.SCHEDULED_SEND").apply {
            putExtra("id", message.id)
            putExtra("jid", message.jid)
            putExtra("messageText", message.messageText)
            putExtra("mediaPath", message.mediaPath)
            putExtra("mediaType", message.mediaType)
            // Set package to ensure broadcast reaches only the target WhatsApp version if it's running
            // Wait, we can broadcast to both com.whatsapp and com.whatsapp.w4b since WhatsApp is the target.
            // Better to not set package if it can target both, or we can broadcast twice for each target package.
        }

        // Broadcast to both com.whatsapp and com.whatsapp.w4b
        val broadcastWpp = Intent(intent).apply { `package` = "com.whatsapp" }
        sendBroadcast(broadcastWpp)

        val broadcastBusiness = Intent(intent).apply { `package` = "com.whatsapp.w4b" }
        sendBroadcast(broadcastBusiness)
    }

    private fun updateMessageStatus(id: Int, success: Boolean) {
        executor.execute {
            val db = AppDatabase.getInstance(this)
            val message = db.scheduledMessageDao().getById(id) ?: return@execute

            if (success) {
                if (message.isRecurring) {
                    // Calculate next scheduled time and keep pending
                    val nextTime = SchedulerHelper.calculateNextOccurrence(message)
                    if (nextTime > 0L) {
                        val updatedMessage = message.copy(
                            scheduledTime = nextTime,
                            status = "PENDING"
                        )
                        db.scheduledMessageDao().update(updatedMessage)
                    } else {
                        // Recurrence error, mark as sent or delete
                        if (message.autoDelete) {
                            db.scheduledMessageDao().delete(message)
                        } else {
                            db.scheduledMessageDao().update(message.copy(status = "SENT"))
                        }
                    }
                } else {
                    // Non-recurring
                    if (message.autoDelete) {
                        db.scheduledMessageDao().delete(message)
                    } else {
                        db.scheduledMessageDao().update(message.copy(status = "SENT"))
                    }
                }
            } else {
                // Failed, update status
                db.scheduledMessageDao().update(message.copy(status = "FAILED"))
            }

            // Reschedule the alarm for the next pending message
            SchedulerHelper.scheduleNextAlarm(this)
        }
    }

    private fun stopService() {
        // Reschedule alarm for next earliest message
        SchedulerHelper.scheduleNextAlarm(this)
        isProcessing = false
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(statusReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        executor.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                "Scheduler Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Message Scheduler")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
