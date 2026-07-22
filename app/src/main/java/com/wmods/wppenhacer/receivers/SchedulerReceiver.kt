package com.wmods.wppenhacer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.wmods.wppenhacer.database.SchedulerHelper
import com.wmods.wppenhacer.services.SchedulerService

class SchedulerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("SchedulerReceiver", "onReceive: triggered action $action")

        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d("SchedulerReceiver", "onReceive: rescheduling pending alarms on boot/update")
            SchedulerHelper.scheduleNextAlarm(context)
        } else if (action == "com.wmods.wppenhacer.TRIGGER_ALARM") {
            Log.d("SchedulerReceiver", "onReceive: TRIGGER_ALARM received, acquiring wake lock")
            val pendingResult = goAsync()
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "WaEnhancer:SchedulerWakeLock"
            )
            wakeLock.acquire(15 * 1000L) // 15 seconds timeout

            Thread {
                try {
                    SchedulerService.processNow(context)
                } finally {
                    if (wakeLock.isHeld) {
                        wakeLock.release()
                    }
                    pendingResult.finish()
                }
            }.start()
        }
    }
}
