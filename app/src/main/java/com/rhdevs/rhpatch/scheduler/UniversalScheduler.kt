package com.rhdevs.rhpatch.scheduler

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

data class UniversalTask(
    val id: Int,
    val targetApp: String, // "whatsapp", "telegram", "sms", "email"
    val targetContact: String, // phone or username
    val messageText: String,
    val timestamp: Long
)

class UniversalAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("id", -1)
        val targetApp = intent.getStringExtra("targetApp") ?: return
        val contact = intent.getStringExtra("contact") ?: return
        val messageText = intent.getStringExtra("messageText") ?: return

        // Route to AccessibilityService for Keyguard bypass and UI injection
        com.rhdevs.rhpatch.services.AutoSenderAccessibilityService.enqueueUniversalTask(
            id, targetApp, contact, messageText
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
