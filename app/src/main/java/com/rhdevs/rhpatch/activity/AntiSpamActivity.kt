package com.rhdevs.rhpatch.activity

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.google.android.material.switchmaterial.SwitchMaterial
import android.widget.Toast
import android.widget.LinearLayout
import android.graphics.Color
import android.view.View
import android.widget.TextView
import android.widget.ScrollView
import android.content.pm.PackageManager
import android.Manifest
import com.rhdevs.rhpatch.activities.base.BaseActivity

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
            prefs.edit().putBoolean("antispam_sms_enabled", isChecked).apply()
            makeFileReadable()
        }
        
        btnSaveSms.setOnClickListener {
            prefs.edit().putString("antispam_sms_keywords", inputKeywords.text.toString()).apply()
            makeFileReadable()
            Toast.makeText(this, "Kata Kunci SMS Disimpan!", Toast.LENGTH_SHORT).show()
        }
        
        switchWa.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("message_blocker_enabled", isChecked).apply()
            makeFileReadable()
        }
        
        btnSaveWa.setOnClickListener {
            prefs.edit().putString("message_block_keywords", inputWaKeywords.text.toString()).apply()
            makeFileReadable()
            Toast.makeText(this, "Kata Kunci WhatsApp Disimpan!", Toast.LENGTH_SHORT).show()
        }
        
        switchCallHidden.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("antispam_call_hidden", isChecked).apply()
            makeFileReadable()
        }
        
        switchCallNonContacts.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 101)
                switchCallNonContacts.isChecked = false // Revert until permission granted
            } else {
                prefs.edit().putBoolean("antispam_call_non_contacts", isChecked).apply()
                makeFileReadable()
            }
        }
        
        btnLog.setOnClickListener {
            loadSpamHistory()
        }
    }
    
    private fun loadSpamHistory() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val logsSms = prefs.getString("antispam_logs", "[]") ?: "[]"
        val logsWa = prefs.getString("wa_antispam_logs", "[]") ?: "[]"
        val container = findViewById<LinearLayout>(com.rhdevs.rhpatch.R.id.container_spam_history)
        
        container.removeAllViews()
        container.visibility = View.VISIBLE
        
        try {
            val jsonArraySms = org.json.JSONArray(logsSms)
            val jsonArrayWa = org.json.JSONArray(logsWa)
            
            val allLogs = mutableListOf<org.json.JSONObject>()
            for (i in 0 until jsonArraySms.length()) allLogs.add(jsonArraySms.getJSONObject(i))
            for (i in 0 until jsonArrayWa.length()) allLogs.add(jsonArrayWa.getJSONObject(i))
            
            if (allLogs.isEmpty()) {
                Toast.makeText(this, "Riwayat spam masih kosong.", Toast.LENGTH_SHORT).show()
                container.visibility = View.GONE
                return
            }
            
            // Sort by time descending
            allLogs.sortByDescending { it.optLong("time", 0) }
            
            for (logObj in allLogs) {
                val type = logObj.optString("type", "Unknown")
                val message = logObj.optString("message", "")
                val time = logObj.optLong("time", 0)
                
                val dateString = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(time))
                
                val card = android.widget.FrameLayout(this)
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 0, 0, 24)
                card.layoutParams = params
                
                val bg = android.graphics.drawable.GradientDrawable()
                bg.setColor(android.graphics.Color.parseColor("#1A2235"))
                bg.cornerRadius = 24f
                val strokeColor = if (type.contains("WhatsApp", true)) Color.parseColor("#25D366") else Color.parseColor("#3B82F6")
                bg.setStroke(2, strokeColor)
                card.background = bg
                card.setPadding(32, 32, 32, 32)
                
                val textLayout = LinearLayout(this)
                textLayout.orientation = LinearLayout.VERTICAL
                
                val headerText = TextView(this)
                val typeLabel = if (type.contains("WhatsApp", true)) " $type" else " $type"
                headerText.text = "$typeLabel • $dateString"
                headerText.setTextColor(android.graphics.Color.parseColor("#94A3B8"))
                headerText.textSize = 12f
                
                val msgText = TextView(this)
                msgText.text = message
                msgText.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
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
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Izin kontak diberikan! Silakan aktifkan opsi kembali.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Izin kontak diperlukan untuk fitur ini.", Toast.LENGTH_SHORT).show()
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
