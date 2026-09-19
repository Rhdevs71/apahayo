package com.rhdevs.rhpatch.activity

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.switchmaterial.SwitchMaterial
import com.rhdevs.rhpatch.activities.base.BaseActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AntiSpamActivity : BaseActivity() {
    private val PREFS_NAME = "prefs"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        setContentView(com.rhdevs.rhpatch.R.layout.activity_anti_spam)
        
        findViewById<com.google.android.material.appbar.MaterialToolbar>(com.rhdevs.rhpatch.R.id.toolbar)?.setNavigationOnClickListener {
            finish()
        }
        
        val switchSms = findViewById<SwitchMaterial>(com.rhdevs.rhpatch.R.id.switch_sms)
        val inputKeywords = findViewById<EditText>(com.rhdevs.rhpatch.R.id.input_keywords)
        val btnSaveSms = findViewById<Button>(com.rhdevs.rhpatch.R.id.btn_save_sms)
        val switchCallHidden = findViewById<SwitchMaterial>(com.rhdevs.rhpatch.R.id.switch_call_hidden)
        val switchCallNonContacts = findViewById<SwitchMaterial>(com.rhdevs.rhpatch.R.id.switch_call_non_contacts)
        val btnLog = findViewById<Button>(com.rhdevs.rhpatch.R.id.btn_log)
        val switchWa = findViewById<SwitchMaterial>(com.rhdevs.rhpatch.R.id.switch_wa)
        val inputWaKeywords = findViewById<EditText>(com.rhdevs.rhpatch.R.id.input_wa_keywords)
        val btnSaveWa = findViewById<Button>(com.rhdevs.rhpatch.R.id.btn_save_wa)
        
        // Initialize values
        switchSms.isChecked = prefs.getBoolean("antispam_sms_enabled", false)
        inputKeywords.setText(prefs.getString("antispam_sms_keywords", "pinjol,menang undian,gacor,slot,dana kaget"))
        switchCallHidden.isChecked = prefs.getBoolean("antispam_call_hidden", false)
        switchCallNonContacts.isChecked = prefs.getBoolean("antispam_call_non_contacts", false)
        
        switchWa.isChecked = prefs.getBoolean("message_blocker_enabled", false)
        inputWaKeywords.setText(prefs.getString("message_block_keywords", ""))
        
        // Listeners
        switchSms.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("antispam_sms_enabled", isChecked).commit()
            makeFileReadable()
            val msg = if (isChecked) "Filter SMS Spam diaktifkan" else "Filter SMS Spam dinonaktifkan"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
        
        btnSaveSms.setOnClickListener {
            prefs.edit().putString("antispam_sms_keywords", inputKeywords.text.toString()).commit()
            makeFileReadable()
            Toast.makeText(this, "Kata Kunci SMS Disimpan!", Toast.LENGTH_SHORT).show()
        }
        
        switchWa.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("message_blocker_enabled", isChecked).commit()
            makeFileReadable()
        }
        
        btnSaveWa.setOnClickListener {
            prefs.edit().putString("message_block_keywords", inputWaKeywords.text.toString()).commit()
            makeFileReadable()
            Toast.makeText(this, "Kata Kunci WhatsApp Disimpan!", Toast.LENGTH_SHORT).show()
        }
        
        val syncCallMasterState = {
            val isHidden = switchCallHidden.isChecked
            val isNonContacts = switchCallNonContacts.isChecked
            val isCallEnabled = isHidden || isNonContacts
            prefs.edit().putBoolean("antispam_call_enabled", isCallEnabled).commit()
            makeFileReadable()
            if (isCallEnabled) {
                requestCallScreeningRoleIfNeeded()
            }
        }

        switchCallHidden.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("antispam_call_hidden", isChecked).commit()
            syncCallMasterState()
        }
        
        switchCallNonContacts.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 101)
                switchCallNonContacts.isChecked = false // Revert until permission granted
            } else {
                prefs.edit().putBoolean("antispam_call_non_contacts", isChecked).commit()
                syncCallMasterState()
            }
        }
        
        btnLog.setOnClickListener {
            loadSpamHistory()
        }
    }

    private fun requestCallScreeningRoleIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                    startActivityForResult(intent, 202)
                }
            }
        }
    }
    
    private fun loadSpamHistory() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val logs = prefs.getString("antispam_logs", "[]") ?: "[]"
        val container = findViewById<LinearLayout>(com.rhdevs.rhpatch.R.id.container_spam_history)
        
        container.removeAllViews()
        container.visibility = View.VISIBLE
        
        try {
            val jsonArray = JSONArray(logs)
            
            val allLogs = mutableListOf<JSONObject>()
            for (i in 0 until jsonArray.length()) allLogs.add(jsonArray.getJSONObject(i))
            
            if (allLogs.isEmpty()) {
                Toast.makeText(this, "Riwayat spam masih kosong.", Toast.LENGTH_SHORT).show()
                container.visibility = View.GONE
                return
            }
            
            // Sort by time descending
            allLogs.sortByDescending { it.optLong("time", 0) }
            
            for (logObj in allLogs) {
                val type = logObj.optString("type", "Spam")
                val message = logObj.optString("message", "")
                val time = logObj.optLong("time", 0)
                
                val dateString = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date(time))
                
                val card = FrameLayout(this)
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 0, 0, 24)
                card.layoutParams = params
                
                val bg = GradientDrawable()
                bg.setColor(Color.parseColor("#1A2235"))
                bg.cornerRadius = 24f
                val strokeColor = when {
                    type.contains("WhatsApp", true) -> Color.parseColor("#25D366")
                    type.contains("Panggilan", true) -> Color.parseColor("#EF4444")
                    else -> Color.parseColor("#3B82F6")
                }
                bg.setStroke(2, strokeColor)
                card.background = bg
                card.setPadding(32, 32, 32, 32)
                
                val textLayout = LinearLayout(this)
                textLayout.orientation = LinearLayout.VERTICAL
                
                val headerText = TextView(this)
                headerText.text = "$type • $dateString"
                headerText.setTextColor(Color.parseColor("#94A3B8"))
                headerText.textSize = 12f
                
                val msgText = TextView(this)
                msgText.text = message
                msgText.setTextColor(Color.parseColor("#FFFFFF"))
                msgText.textSize = 14f
                msgText.setPadding(0, 8, 0, 0)
                
                textLayout.addView(headerText)
                textLayout.addView(msgText)
                card.addView(textLayout)
                
                container.addView(card)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memuat riwayat", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            val switchCallNonContacts = findViewById<SwitchMaterial>(com.rhdevs.rhpatch.R.id.switch_call_non_contacts)
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                switchCallNonContacts.isChecked = true
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putBoolean("antispam_call_non_contacts", true).putBoolean("antispam_call_enabled", true).commit()
                makeFileReadable()
                Toast.makeText(this, "Izin kontak diberikan! Blokir non-kontak aktif.", Toast.LENGTH_SHORT).show()
                requestCallScreeningRoleIfNeeded()
            } else {
                switchCallNonContacts.isChecked = false
                Toast.makeText(this, "Izin kontak diperlukan untuk memfilter nomor non-kontak.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 202) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Rhpatch berhasil diaktifkan sebagai Penyaring Panggilan!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Penyaring panggilan membutuhkan izin agar dapat memblokir telepon spam.", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun makeFileReadable() {
        runCatching {
            val file = java.io.File(filesDir.parentFile, "shared_prefs/${PREFS_NAME}.xml")
            if (file.exists()) {
                file.setReadable(true, false)
                file.parentFile?.setExecutable(true, false)
                file.parentFile?.setReadable(true, false)
                filesDir.parentFile?.setExecutable(true, false)
                filesDir.parentFile?.setReadable(true, false)
            }
        }
    }
}
