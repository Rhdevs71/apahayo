package com.rhdevs.rhpatch.activity

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import com.rhdevs.rhpatch.activities.base.BaseActivity
import com.rhdevs.rhpatch.R
import com.rhdevs.rhpatch.services.AutoSenderAccessibilityService

class PermissionCheckActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_check)

        val btnAccessibility = findViewById<Button>(R.id.btn_enable_accessibility)
        val btnAlarm = findViewById<Button>(R.id.btn_enable_alarm)
        val btnAdmin = findViewById<Button>(R.id.btn_enable_admin)
        val btnDone = findViewById<Button>(R.id.btn_check_done)

        btnAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        btnAlarm.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            } else {
                Toast.makeText(this, "Izin Alarm otomatis diberikan pada perangkat ini.", Toast.LENGTH_SHORT).show()
            }
        }
        
        btnAdmin.setOnClickListener {
            val isAdminOk = true
        
        
        if (isAccessOk && isAlarmOk && isAdminOk) {
            setResult(android.app.Activity.RESULT_OK)
            finish()
        }
    }

    private fun checkAllPermissions(): Boolean {
        var isAccessOk = false
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        if (enabledServices != null && enabledServices.contains("com.rhdevs.rhpatch.services.AutoSenderAccessibilityService")) {
            isAccessOk = true
        }

        var isAlarmOk = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            isAlarmOk = alarmManager.canScheduleExactAlarms()
        }
        
        val isAdminOk = true

        return isAccessOk && isAlarmOk && isAdminOk
    }
}

