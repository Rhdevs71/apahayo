package com.wmods.wppenhacer.database

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.wmods.wppenhacer.receivers.SchedulerReceiver
import java.util.*
import java.util.concurrent.Executors

object SchedulerHelper {

    private const val TAG = "SchedulerHelper"
    private val executor = Executors.newSingleThreadExecutor()

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleNextAlarm(context: Context) {
        val appContext = context.applicationContext
        executor.execute {
            try {
                val db = AppDatabase.getInstance(appContext)
                val pendingMessages = db.scheduledMessageDao().getByStatus("PENDING")
                Log.d(TAG, "scheduleNextAlarm: found ${pendingMessages.size} pending messages")

                if (pendingMessages.isEmpty()) {
                    val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val intent = Intent(appContext, SchedulerReceiver::class.java).apply {
                        action = "com.wmods.wppenhacer.TRIGGER_ALARM"
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        appContext,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    alarmManager.cancel(pendingIntent)
                    Log.d(TAG, "scheduleNextAlarm: cancelled pending alarms because no messages are pending")
                    return@execute
                }

                val earliestMessage = pendingMessages.minByOrNull { it.scheduledTime } ?: return@execute
                val triggerTime = earliestMessage.scheduledTime

                val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(appContext, SchedulerReceiver::class.java).apply {
                    action = "com.wmods.wppenhacer.TRIGGER_ALARM"
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    appContext,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                Log.d(TAG, "scheduleNextAlarm: scheduling message id ${earliestMessage.id} at $triggerTime (now is ${System.currentTimeMillis()})")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                        Log.d(TAG, "scheduleNextAlarm: setExactAndAllowWhileIdle successful")
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                        Log.d(TAG, "scheduleNextAlarm: fallback to setAndAllowWhileIdle (canScheduleExactAlarms is false)")
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    Log.d(TAG, "scheduleNextAlarm: setExactAndAllowWhileIdle on SDK >= 23")
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    Log.d(TAG, "scheduleNextAlarm: setExact on older SDK")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in scheduleNextAlarm: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun calculateNextOccurrence(message: ScheduledMessage): Long {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        calendar.timeInMillis = message.scheduledTime

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
                    val targetDays = daysStr.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
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
                        return 0L
                    }
                }
                else -> return 0L
            }
        }
        return calendar.timeInMillis
    }
}
