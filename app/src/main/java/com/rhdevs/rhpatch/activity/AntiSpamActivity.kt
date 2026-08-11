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
        
        // Dynamic UI without XML
        val scrollView = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            setBackgroundColor(Color.parseColor("#0F172A"))
        }
        
        // --- HEADER ---
        container.addView(TextView(this).apply {
            text = "Pengaturan Anti-Spam Sistem"
            textSize = 24f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 10)
        })
        container.addView(TextView(this).apply {
            text = "Blokir SMS penipuan dan panggilan tak dikenal langsung dari inti sistem."
            textSize = 14f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, 40)
        })
        
        // --- SMS ANTI SPAM ---
        container.addView(createSectionTitle("🛡️ Filter SMS Anti-Spam"))
        
        val switchSms = Switch(this).apply {
            text = "Aktifkan Pemblokiran SMS Berdasarkan Kata"
            setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean("antispam_sms_enabled", false)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("antispam_sms_enabled", isChecked).apply()
                makeFileReadable()
            }
        }
        container.addView(switchSms)
        
        container.addView(TextView(this).apply {
            text = "Masukkan kata kunci terlarang (pisahkan dengan koma). Contoh: pinjol,menang undian,gacor"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 10, 0, 10)
        })
        
        val inputKeywords = EditText(this).apply {
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            hint = "pinjol,gacor,undian..."
            setText(prefs.getString("antispam_sms_keywords", "pinjol,menang undian,gacor,slot,dana kaget"))
        }
        container.addView(inputKeywords)
        
        val btnSaveSms = Button(this).apply {
            text = "Simpan Kata Kunci SMS"
            setBackgroundColor(Color.parseColor("#3B82F6"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                prefs.edit().putString("antispam_sms_keywords", inputKeywords.text.toString()).apply()
                makeFileReadable()
                Toast.makeText(this@AntiSpamActivity, "Kata Kunci SMS Disimpan!", Toast.LENGTH_SHORT).show()
            }
        }
        container.addView(btnSaveSms)
        
        // --- SPACING ---
        container.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, 60) })
        
        // --- CALL ANTI SPAM ---
        container.addView(createSectionTitle("📞 Filter Panggilan Anti-Spam"))
        
        val switchCallHidden = Switch(this).apply {
            text = "Blokir Nomor Pribadi (Hidden / Unknown)"
            setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean("antispam_call_hidden", false)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("antispam_call_hidden", isChecked).apply()
                makeFileReadable()
            }
        }
        container.addView(switchCallHidden)
        
        container.addView(TextView(this).apply {
            text = "Panggilan tanpa Caller ID akan otomatis ditolak."
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 5, 0, 30)
        })
        
        val switchCallNonContacts = Switch(this).apply {
            text = "Blokir Semua Nomor Asing (Non-Kontak)"
            setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean("antispam_call_non_contacts", false)
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 101)
                    this.isChecked = false // Revert until permission granted
                } else {
                    prefs.edit().putBoolean("antispam_call_non_contacts", isChecked).apply()
                    makeFileReadable()
                }
            }
        }
        container.addView(switchCallNonContacts)
        
        container.addView(TextView(this).apply {
            text = "Hanya nomor yang tersimpan di kontak Anda yang bisa menelpon."
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 5, 0, 40)
        })
        
        // --- SPACING ---
        container.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, 40) })
        
        val btnLog = Button(this).apply {
            text = "Lihat Riwayat Pemblokiran (Spam Log)"
            setBackgroundColor(Color.parseColor("#10B981"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                Toast.makeText(this@AntiSpamActivity, "Riwayat spam masih kosong.", Toast.LENGTH_SHORT).show()
            }
        }
        container.addView(btnLog)
        
        scrollView.addView(container)
        setContentView(scrollView)
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
    
    private fun createSectionTitle(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 18f
            setTextColor(Color.parseColor("#3B82F6"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 20)
        }
    }
    
    private fun makeFileReadable() {
        runCatching {
            val file = java.io.File(filesDir.parentFile, "shared_prefs/${PREFS_NAME}.xml")
            if (file.exists()) file.setReadable(true, false)
        }
    }
}
