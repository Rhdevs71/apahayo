package com.rhdevs.rhpatch.scheduler

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.rhdevs.rhpatch.R

data class UniversalTask(
    val id: Int,
    val targetApp: String, // "whatsapp", "telegram", "sms", "email"
    val targetContact: String, // phone or username
    val messageText: String,
    val timestamp: Long,
    val mediaPath: String? = null,
    val mediaType: String? = null
)

class UniversalAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("id", -1)
        val targetApp = intent.getStringExtra("targetApp") ?: return
        val contact = intent.getStringExtra("contact") ?: return
        val messageText = intent.getStringExtra("messageText") ?: return
        val mediaPath = intent.getStringExtra("mediaPath")
        val mediaType = intent.getStringExtra("mediaType")

        // Acquire WakeLock to turn on screen from deep sleep
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = pm.newWakeLock(
                android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or
                android.os.PowerManager.ON_AFTER_RELEASE,
                "Rhpatch:UniversalAlarmWakeLock"
            )
            wakeLock.acquire(60 * 1000L)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Show Informational Notification (Non-clickable)
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "scheduler_alarm_channel"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Scheduler Alarm",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Menjalankan jadwal pesan otomatis di background"
                    enableVibration(false)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_new)
                .setContentTitle("Menjalankan Jadwal Pesan...")
                .setContentText("Kirim ke $contact ($targetApp)")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .build()

            nm.notify(id, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Direct task dispatch to AutoSenderAccessibilityService in background
        com.rhdevs.rhpatch.services.AutoSenderAccessibilityService.enqueueUniversalTask(
            id = id,
            targetApp = targetApp,
            contact = contact,
            messageText = messageText,
            mediaPath = mediaPath,
            mediaType = mediaType
        )
    }
}

object UniversalScheduler {
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleTask(context: Context, task: UniversalTask) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, UniversalAlarmReceiver::class.java).apply {
            putExtra("id", task.id)
            putExtra("targetApp", task.targetApp)
            putExtra("contact", task.targetContact)
            putExtra("messageText", task.messageText)
            putExtra("mediaPath", task.mediaPath)
            putExtra("mediaType", task.mediaType)
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getBroadcast(context, task.id, intent, flags)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.timestamp, pendingIntent)
            } else {
                // Fallback if permission not granted
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.timestamp, pendingIntent)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.timestamp, pendingIntent)
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, task.timestamp, pendingIntent)
        }
    }
}
