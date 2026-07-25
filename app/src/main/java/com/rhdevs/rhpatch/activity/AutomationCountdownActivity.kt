package com.rhdevs.rhpatch.activity

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.wmods.wppenhacer.R

class AutomationCountdownActivity : AppCompatActivity() {

    private var countDownTimer: CountDownTimer? = null

    private var automationReceiver: android.content.BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Wake up screen and show above lockscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        
        // Ensure screen stays on during processing
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_countdown)

        val taskId = intent.getIntExtra("taskId", -1)
        val targetApp = intent.getStringExtra("targetApp") ?: ""
        val contact = intent.getStringExtra("contact") ?: ""
        val messageText = intent.getStringExtra("messageText") ?: ""
        val mediaPath = intent.getStringExtra("mediaPath")
        val mediaType = intent.getStringExtra("mediaType")

        val tvCountdown = findViewById<TextView>(R.id.tv_countdown)
        val btnCancel = findViewById<Button>(R.id.btn_cancel)
        
        automationReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: android.content.Intent?) {
                if (intent?.action == "com.rhdevs.rhpatch.AUTOMATION_COMPLETE") {
                    finish()
                }
            }
        }
        
        val filter = android.content.IntentFilter("com.rhdevs.rhpatch.AUTOMATION_COMPLETE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(automationReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(automationReceiver, filter)
        }

        btnCancel.setOnClickListener {
            countDownTimer?.cancel()
            finish()
        }

        countDownTimer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tvCountdown.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                tvCountdown.textSize = 24f
                tvCountdown.text = "Memproses..."
                btnCancel.visibility = android.view.View.GONE
                // Time's up! Send to Accessibility Service
                com.rhdevs.rhpatch.services.AutoSenderAccessibilityService.enqueueUniversalTask(
                    taskId, targetApp, contact, messageText, mediaPath, mediaType
                )
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        automationReceiver?.let { unregisterReceiver(it) }
    }
}
