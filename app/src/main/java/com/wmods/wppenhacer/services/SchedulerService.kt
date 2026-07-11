package com.wmods.wppenhacer.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.wmods.wppenhacer.database.AppDatabase
import com.wmods.wppenhacer.database.SchedulerHelper
import com.wmods.wppenhacer.database.ScheduledMessage
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SchedulerService : Service() {

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val channelId = "scheduler_service"
    private var isProcessing = false
    private val TAG = "SchedulerService"

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getIntExtra("id", -1)
            val success = intent.getBooleanExtra("success", false)
            Log.d(TAG, "statusReceiver: status received for message id $id, success: $success")
            if (id != -1) {
                updateMessageStatus(id, success)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: service initializing")
        createNotificationChannel()
        val notification = createNotification("Processing scheduled messages...")
        startForeground(1122, notification)

        val filter = IntentFilter("com.wmods.wppenhacer.SCHEDULED_STATUS")
        ContextCompat.registerReceiver(
            this,
            statusReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: service starting")
        if (!isProcessing) {
            isProcessing = true
            executor.execute {
                // Run media cleaner first if necessary
                cleanMediaIfNecessary()
                // Process pending messages
                processPendingMessages()
            }
        }
        return START_NOT_STICKY
    }

    private fun cleanMediaIfNecessary() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val enabled = prefs.getBoolean("media_cleaner_enabled", false)
        if (!enabled) return

        val now = System.currentTimeMillis()
        val lastClean = prefs.getLong("media_cleaner_last_run", 0L)
        // Only run cleaner once every 24 hours
        if (now - lastClean < 24 * 60 * 60 * 1000L) {
            Log.d(TAG, "cleanMediaIfNecessary: skipped, last clean was less than 24 hours ago")
            return
        }

        val cleanDaysStr = prefs.getString("media_cleaner_days", "30") ?: "30"
        val cleanDays = cleanDaysStr.toIntOrNull() ?: 30
        Log.d(TAG, "cleanMediaIfNecessary: running media cleaner for files older than $cleanDays days")

        try {
            // Find WhatsApp media folder in external shared storage
            val appLabel = packageManager.getApplicationLabel(applicationInfo).toString()
            val mediaRoot = File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media")
            val mediaRootBusiness = File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp.w4b/WhatsApp Business/Media")

            cleanDirectory(mediaRoot, cleanDays)
            cleanDirectory(mediaRootBusiness, cleanDays)

            prefs.edit().putLong("media_cleaner_last_run", now).apply()
            Log.d(TAG, "cleanMediaIfNecessary: media cleanup completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "cleanMediaIfNecessary Error: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun cleanDirectory(dir: File, days: Int) {
        if (!dir.exists() || !dir.isDirectory) return
        val files = dir.listFiles() ?: return
        val threshold = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)

        for (file in files) {
            if (file.isDirectory) {
                // Do not delete WhatsApp Sent folders or core structure folders entirely, just clean files recursively
                cleanDirectory(file, days)
            } else {
                if (file.lastModified() < threshold) {
                    val deleted = file.delete()
                    if (deleted) {
                        Log.d(TAG, "cleanDirectory: deleted expired file: ${file.name}")
                    }
                }
            }
        }
    }

    private fun processPendingMessages() {
        val db = AppDatabase.getInstance(this)
        val now = System.currentTimeMillis()
        val pendingMessages = db.scheduledMessageDao().getPendingBefore(now)
        Log.d(TAG, "processPendingMessages: found ${pendingMessages.size} pending messages at $now")

        if (pendingMessages.isEmpty()) {
            Log.d(TAG, "processPendingMessages: no messages pending, stopping service")
            stopService()
            return
        }

        for (message in pendingMessages) {
            Log.d(TAG, "processPendingMessages: sending message id ${message.id} to WhatsApp JID: ${message.jid}")
            sendMessageToWhatsApp(message)
            try {
                // Wait up to 5 seconds for status feedback broadcast
                Thread.sleep(5000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        stopService()
    }

    private fun sendMessageToWhatsApp(message: ScheduledMessage) {
        val intent = Intent("com.wmods.wppenhacer.SCHEDULED_SEND").apply {
            putExtra("id", message.id)
            putExtra("jid", message.jid)
            putExtra("messageText", message.messageText)
            putExtra("mediaPath", message.mediaPath)
            putExtra("mediaType", message.mediaType)
        }

        Log.d(TAG, "sendMessageToWhatsApp: broadcasting to com.whatsapp & com.whatsapp.w4b")
        val broadcastWpp = Intent(intent).apply { `package` = "com.whatsapp" }
        sendBroadcast(broadcastWpp)

        val broadcastBusiness = Intent(intent).apply { `package` = "com.whatsapp.w4b" }
        sendBroadcast(broadcastBusiness)
    }

    private fun updateMessageStatus(id: Int, success: Boolean) {
        executor.execute {
            val db = AppDatabase.getInstance(this)
            val message = db.scheduledMessageDao().getById(id) ?: return@execute
            Log.d(TAG, "updateMessageStatus: updating message id $id, status: ${message.status}, success: $success")

            if (success) {
                if (message.isRecurring) {
                    val nextTime = SchedulerHelper.calculateNextOccurrence(message)
                    Log.d(TAG, "updateMessageStatus: recurring message, calculated next time: $nextTime")
                    if (nextTime > 0L) {
                        val updatedMessage = message.copy(
                            scheduledTime = nextTime,
                            status = "PENDING"
                        )
                        db.scheduledMessageDao().update(updatedMessage)
                    } else {
                        if (message.autoDelete) {
                            db.scheduledMessageDao().delete(message)
                        } else {
                            db.scheduledMessageDao().update(message.copy(status = "SENT"))
                        }
                    }
                } else {
                    if (message.autoDelete) {
                        db.scheduledMessageDao().delete(message)
                        Log.d(TAG, "updateMessageStatus: deleted once-only message id $id")
                    } else {
                        db.scheduledMessageDao().update(message.copy(status = "SENT"))
                        Log.d(TAG, "updateMessageStatus: set message id $id to SENT")
                    }
                }
            } else {
                db.scheduledMessageDao().update(message.copy(status = "FAILED"))
                Log.d(TAG, "updateMessageStatus: set message id $id to FAILED")
            }

            SchedulerHelper.scheduleNextAlarm(this)
        }
    }

    private fun stopService() {
        Log.d(TAG, "stopService: scheduling next alarm and stopping foreground")
        SchedulerHelper.scheduleNextAlarm(this)
        isProcessing = false
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: service destroyed")
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
