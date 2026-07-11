package com.wmods.wppenhacer.database

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wmods.wppenhacer.receivers.SchedulerReceiver
import java.util.*

object SchedulerHelper {

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleNextAlarm(context: Context) {
        val db = AppDatabase.getInstance(context)
        val pendingMessages = db.scheduledMessageDao().getByStatus("PENDING")
        if (pendingMessages.isEmpty()) {
            // Cancel current scheduling pending intent if no messages are pending
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, SchedulerReceiver::class.java).apply {
                action = "com.wmods.wppenhacer.TRIGGER_ALARM"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            return
        }

        // Find the message with the earliest scheduled time
        val earliestMessage = pendingMessages.minByOrNull { it.scheduledTime } ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, SchedulerReceiver::class.java).apply {
            action = "com.wmods.wppenhacer.TRIGGER_ALARM"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = earliestMessage.scheduledTime

        // Schedule exact alarm that fires even in idle/doze mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun calculateNextOccurrence(message: ScheduledMessage): Long {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        calendar.timeInMillis = message.scheduledTime

        // If scheduled time has already passed, compute next recurrence
        if (calendar.timeInMillis <= now) {
            when (message.recurrenceType) {
                "DAILY" -> {
                    while (calendar.timeInMillis <= now) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                "WEEKLY" -> {
                    while (calendar.timeInMillis <= now) {
                        calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    }
                }
                "MONTHLY" -> {
                    while (calendar.timeInMillis <= now) {
                        calendar.add(Calendar.MONTH, 1)
                    }
                }
                "SPECIFIC_DAYS" -> {
                    val daysStr = message.recurrenceDays ?: ""
                    val targetDays = daysStr.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet() // 1=Sunday, 2=Monday...
                    if (targetDays.isNotEmpty()) {
                        while (true) {
                            calendar.add(Calendar.DAY_OF_YEAR, 1)
                            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                            if (targetDays.contains(dayOfWeek)) {
                                if (calendar.timeInMillis > now) {
                                    break
                                }
                            }
                        }
                    } else {
                        // Fallback to once if no specific days are selected
                        return 0L
                    }
                }
                else -> return 0L // Once
            }
        }

        return calendar.timeInMillis
    }
}
