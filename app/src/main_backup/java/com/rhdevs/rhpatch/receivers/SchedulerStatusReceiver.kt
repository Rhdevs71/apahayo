package com.rhdevs.rhpatch.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.rhdevs.rhpatch.database.AppDatabase
import com.rhdevs.rhpatch.database.SchedulerHelper
import java.util.concurrent.Executors

class SchedulerStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == "com.rhdevs.rhpatch.SCHEDULED_STATUS") {
            val id = intent.getIntExtra("id", -1)
            val success = intent.getBooleanExtra("success", false)
            Log.d("SchedulerStatusReceiver", "onReceive: status received for message id $id, success: $success")
            if (id != -1) {
                val appContext = context.applicationContext
                Executors.newSingleThreadExecutor().execute {
                    try {
                        val db = AppDatabase.getInstance(appContext)
                        val message = db.scheduledMessageDao().getById(id) ?: return@execute
                        Log.d("SchedulerStatusReceiver", "Updating message id $id status to ${if (success) "SENT" else "FAILED"}")

                        if (success) {
                            if (message.isRecurring) {
                                val nextTime = SchedulerHelper.calculateNextOccurrence(message)
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
                                } else {
                                    db.scheduledMessageDao().update(message.copy(status = "SENT"))
                                }
                            }
                        } else {
                            db.scheduledMessageDao().update(message.copy(status = "FAILED"))
                        }

                        // Reschedule next alarm
                        SchedulerHelper.scheduleNextAlarm(appContext)
                    } catch (e: Exception) {
                        Log.e("SchedulerStatusReceiver", "Error updating status: ${e.message}")
                    }
                }
            }
        }
    }
}
