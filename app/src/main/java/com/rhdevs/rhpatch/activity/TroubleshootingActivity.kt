package com.rhdevs.rhpatch.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.wmods.wppenhacer.R

class TroubleshootingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_troubleshooting)

        val btnBattery = findViewById<Button>(R.id.btn_trouble_battery)
        val btnAutostart = findViewById<Button>(R.id.btn_trouble_autostart)
        val btnAccessibility = findViewById<Button>(R.id.btn_trouble_accessibility)
        val btnNotification = findViewById<Button>(R.id.btn_trouble_notification)

        btnBattery.setOnClickListener {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            try {
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }

        btnAutostart.setOnClickListener {
            // Autostart intents are highly OEM specific. We attempt to open App Info as fallback.
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }

        btnAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        btnNotification.setOnClickListener {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }
    }
}
