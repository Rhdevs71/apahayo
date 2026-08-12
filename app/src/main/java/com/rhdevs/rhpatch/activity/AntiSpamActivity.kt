package com.rhdevs.rhpatch.activity

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import android.widget.LinearLayout
import android.graphics.Color
import android.view.View
import android.widget.TextView
import android.widget.ScrollView
import android.content.pm.PackageManager
import android.Manifest

class AntiSpamActivity : Activity() {
    private val PREFS_NAME = "prefs"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        setContentView(com.wmods.wppenhacer.R.layout.activity_anti_spam)
        
        val switchSms = findViewById<Switch>(com.wmods.wppenhacer.R.id.switch_sms)
        val inputKeywords = findViewById<EditText>(com.wmods.wppenhacer.R.id.input_keywords)
        val btnSaveSms = findViewById<Button>(com.wmods.wppenhacer.R.id.btn_save_sms)
        val switchCallHidden = findViewById<Switch>(com.wmods.wppenhacer.R.id.switch_call_hidden)
        val switchCallNonContacts = findViewById<Switch>(com.wmods.wppenhacer.R.id.switch_call_non_contacts)
        val btnLog = findViewById<Button>(com.wmods.wppenhacer.R.id.btn_log)
        
        // Initialize values
        switchSms.isChecked = prefs.getBoolean("antispam_sms_enabled", false)
        inputKeywords.setText(prefs.getString("antispam_sms_keywords", "pinjol,menang undian,gacor,slot,dana kaget"))
        switchCallHidden.isChecked = prefs.getBoolean("antispam_call_hidden", false)
        switchCallNonContacts.isChecked = prefs.getBoolean("antispam_call_non_contacts", false)
        
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
            Toast.makeText(this, "Riwayat spam masih kosong.", Toast.LENGTH_SHORT).show()
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
            if (file.exists()) file.setReadable(true, false)
        }
    }
}
