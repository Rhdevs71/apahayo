package com.wmods.wppenhacer.activities

import android.graphics.BitmapFactory
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.wmods.wppenhacer.R
import com.wmods.wppenhacer.databinding.ActivityFakeCallBinding
import java.io.File

class FakeCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFakeCallBinding
    private var ringtone: Ringtone? = null
    private val handler = Handler(Looper.getMainLooper())
    private var callDurationSeconds = 0
    private var isCallActive = false

    private val timerRunnable = object : Runnable {
        override fun run() {
            callDurationSeconds++
            val minutes = callDurationSeconds / 60
            val seconds = callDurationSeconds % 60
            binding.textCallSubstatus.text = String.format("%02d:%02d", minutes, seconds)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Show over lock screen and wake up device
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        binding = ActivityFakeCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val contactName = intent.getStringExtra("CONTACT_NAME") ?: "Tokoh Politik"
        val photoPath = intent.getStringExtra("PHOTO_PATH") ?: ""
        val isVideo = intent.getBooleanExtra("IS_VIDEO", false)

        binding.textCallContactName.text = contactName
        binding.textCallStatus.text = if (isVideo) "WhatsApp Video Call" else "WhatsApp Voice Call"

        // Load profile photo
        if (photoPath.isNotEmpty()) {
            val file = File(photoPath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    binding.img_call_profile.setImageBitmap(bitmap)
                    binding.imgBgBlur.setImageBitmap(bitmap)
                }
            }
        }

        // Start ringing
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(applicationContext, ringtoneUri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Set listeners
        binding.btnDecline.setOnClickListener {
            stopRingtone()
            finish()
        }

        binding.btnAccept.setOnClickListener {
            stopRingtone()
            startCallSimulation()
        }

        binding.btnEndCall.setOnClickListener {
            endCallSimulation()
        }
    }

    private fun startCallSimulation() {
        isCallActive = true
        binding.layoutIncomingActions.visibility = View.GONE
        binding.layoutActiveActions.visibility = View.VISIBLE
        binding.textCallSubstatus.text = "00:00"
        
        // Start duration timer
        handler.postDelayed(timerRunnable, 1000)
    }

    private fun endCallSimulation() {
        handler.removeCallbacks(timerRunnable)
        finish()
    }

    private fun stopRingtone() {
        try {
            ringtone?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRingtone()
        handler.removeCallbacks(timerRunnable)
    }
}
