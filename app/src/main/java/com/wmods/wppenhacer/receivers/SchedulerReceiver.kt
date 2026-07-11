package com.wmods.wppenhacer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.wmods.wppenhacer.database.SchedulerHelper
import com.wmods.wppenhacer.services.SchedulerService

class SchedulerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Reschedule all alarms on boot or package update
            SchedulerHelper.scheduleNextAlarm(context)
        } else if (action == "com.wmods.wppenhacer.TRIGGER_ALARM") {
            // Wake lock to keep CPU running while service starts
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "WaEnhancer:SchedulerWakeLock"
            )
            wakeLock.acquire(10 * 1000L) // 10 seconds timeout

            // Start foreground service to process messages
            val serviceIntent = Intent(context, SchedulerService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
