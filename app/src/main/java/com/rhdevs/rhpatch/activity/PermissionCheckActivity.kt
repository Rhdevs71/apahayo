package com.rhdevs.rhpatch.activity

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.wmods.wppenhacer.R
import com.rhdevs.rhpatch.services.AutoSenderAccessibilityService

class PermissionCheckActivity : AppCompatActivity() {

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
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
            val adminComponent = android.content.ComponentName(this, com.rhdevs.rhpatch.receivers.WaDeviceAdminReceiver::class.java)
            if (!dpm.isAdminActive(adminComponent)) {
                val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Dibutuhkan untuk mematikan layar secara otomatis setelah jadwal terkirim.")
                startActivity(intent)
            } else {
                Toast.makeText(this, "Administrator sudah aktif.", Toast.LENGTH_SHORT).show()
            }
        }

        btnDone.setOnClickListener {
            if (checkAllPermissions()) {
                setResult(android.app.Activity.RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, "Tolong aktifkan SEMUA perizinan terlebih dahulu!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionUI()
    }

    private fun updatePermissionUI() {
        val layoutAccessibility = findViewById<android.view.View>(R.id.layout_permission_accessibility)
        val layoutAlarm = findViewById<android.view.View>(R.id.layout_permission_alarm)
        val layoutAdmin = findViewById<android.view.View>(R.id.layout_permission_admin)

        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val isAccessOk = enabledServices != null && enabledServices.contains("com.rhdevs.rhpatch.services.AutoSenderAccessibilityService")
        layoutAccessibility.visibility = if (isAccessOk) android.view.View.GONE else android.view.View.VISIBLE

        var isAlarmOk = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            isAlarmOk = alarmManager.canScheduleExactAlarms()
        }
        layoutAlarm.visibility = if (isAlarmOk) android.view.View.GONE else android.view.View.VISIBLE

        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        val adminComponent = android.content.ComponentName(this, com.rhdevs.rhpatch.receivers.WaDeviceAdminReceiver::class.java)
        val isAdminOk = dpm.isAdminActive(adminComponent)
        layoutAdmin.visibility = if (isAdminOk) android.view.View.GONE else android.view.View.VISIBLE
        
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
        
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        val adminComponent = android.content.ComponentName(this, com.rhdevs.rhpatch.receivers.WaDeviceAdminReceiver::class.java)
        val isAdminOk = dpm.isAdminActive(adminComponent)

        return isAccessOk && isAlarmOk && isAdminOk
    }
}
