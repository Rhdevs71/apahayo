package com.rhdevs.rhpatch.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.Base64
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class ScreenLockConfigActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            setBackgroundColor(Color.parseColor("#0F172A"))
        }

        val title = TextView(this).apply {
            text = "Konfigurasi Kunci Layar"
            textSize = 22f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 30)
        }
        layout.addView(title)

        val desc = TextView(this).apply {
            text = "Pilih salah satu dari 2 opsi di bawah ini agar sistem otomatisasi dapat membuka layar perangkat Anda."
            textSize = 14f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, 50)
        }
        layout.addView(desc)

        // Option 1: Hapus Kunci Layar
        val opt1Title = TextView(this).apply {
            text = "Opsi 1 (Disarankan): Hapus Kunci Layar"
            textSize = 16f
            setTextColor(Color.parseColor("#3B82F6"))
            setPadding(0, 0, 0, 10)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        layout.addView(opt1Title)

        val opt1Desc = TextView(this).apply {
            text = "Ubah kunci layar perangkat Anda menjadi 'Tidak Ada' atau 'Geser' (Swipe). Opsi ini paling aman dan meminimalisir kegagalan saat menyalakan layar otomatis."
            textSize = 14f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, 20)
        }
        layout.addView(opt1Desc)

        val btnRemoveLock = Button(this).apply {
            text = "Buka Pengaturan Keamanan"
            setBackgroundColor(Color.parseColor("#3B82F6"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                startActivity(intent)
            }
        }
        layout.addView(btnRemoveLock)

        // Divider
        val divider = android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2
            ).apply { setMargins(0, 50, 0, 50) }
            setBackgroundColor(Color.parseColor("#1E293B"))
        }
        layout.addView(divider)

        // Option 2: Masukkan PIN / Sandi
        val opt2Title = TextView(this).apply {
            text = "Opsi 2: Tetap Gunakan PIN/Sandi"
            textSize = 16f
            setTextColor(Color.parseColor("#F59E0B"))
            setPadding(0, 0, 0, 10)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        layout.addView(opt2Title)

        val opt2Desc = TextView(this).apply {
            text = "Masukkan PIN atau Sandi Anda di bawah ini agar otomatisasi dapat mengetiknya. (Tidak mendukung kunci pola/pattern)."
            textSize = 14f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, 20)
        }
        layout.addView(opt2Desc)

        val input = EditText(this).apply {
            hint = "Masukkan PIN / Sandi"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(30, 30, 30, 30)
            setBackgroundColor(Color.parseColor("#1E293B"))
        }
        layout.addView(input)

        val btnSavePin = Button(this).apply {
            text = "Simpan PIN/Sandi"
            setBackgroundColor(Color.parseColor("#10B981"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 30 }
            
            setOnClickListener {
                val pinText = input.text.toString()
                if (pinText.isEmpty()) {
                    Toast.makeText(context, "PIN tidak boleh kosong jika memilih Opsi 2", Toast.LENGTH_SHORT).show()
                } else {
                    val encoded = Base64.encodeToString(pinText.toByteArray(), Base64.DEFAULT)
                    val prefs = getSharedPreferences("screen_lock_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("saved_pin", encoded).apply()
                    Toast.makeText(context, "PIN / Sandi berhasil disimpan!", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
        layout.addView(btnSavePin)

        setContentView(layout)
    }
}
