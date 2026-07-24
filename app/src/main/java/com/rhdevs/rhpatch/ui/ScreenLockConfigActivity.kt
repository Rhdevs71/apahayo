package com.rhdevs.rhpatch.ui

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
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
            text = "Konfigurasi Kunci Layar (PIN/Sandi)"
            textSize = 22f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 30)
        }
        layout.addView(title)

        val desc = TextView(this).apply {
            text = "Masukkan PIN atau Sandi yang digunakan untuk membuka layar. Aplikasi ini hanya mendukung PIN/Sandi (tidak mendukung pola/pattern)."
            textSize = 14f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, 50)
        }
        layout.addView(desc)

        val input = EditText(this).apply {
            hint = "Masukkan PIN / Sandi Layar"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(30, 30, 30, 30)
            setBackgroundColor(Color.parseColor("#1E293B"))
        }
        layout.addView(input)

        val btnSave = Button(this).apply {
            text = "Simpan Konfigurasi"
            setBackgroundColor(Color.parseColor("#10B981"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 50
            }
            
            setOnClickListener {
                val pass = input.text.toString()
                if (pass.isEmpty()) {
                    Toast.makeText(this@ScreenLockConfigActivity, "PIN/Sandi tidak boleh kosong", Toast.LENGTH_SHORT).show()
                } else {
                    savePin(pass)
                    Toast.makeText(this@ScreenLockConfigActivity, "Tersimpan", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
        layout.addView(btnSave)
        
        val btnClear = Button(this).apply {
            text = "Hapus Konfigurasi"
            setBackgroundColor(Color.parseColor("#EF4444"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 20
            }
            
            setOnClickListener {
                clearPin()
                input.setText("")
                Toast.makeText(this@ScreenLockConfigActivity, "Dihapus", Toast.LENGTH_SHORT).show()
            }
        }
        layout.addView(btnClear)

        setContentView(layout)
        
        // Load existing
        val existing = loadPin()
        if (existing.isNotEmpty()) {
            input.setText(existing)
        }
    }

    private fun savePin(pin: String) {
        val prefs = getSharedPreferences("screen_lock_prefs", Context.MODE_PRIVATE)
        val encoded = Base64.encodeToString(pin.toByteArray(), Base64.DEFAULT)
        prefs.edit().putString("saved_pin", encoded).apply()
    }
    
    private fun clearPin() {
        val prefs = getSharedPreferences("screen_lock_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("saved_pin").apply()
    }

    private fun loadPin(): String {
        val prefs = getSharedPreferences("screen_lock_prefs", Context.MODE_PRIVATE)
        val encoded = prefs.getString("saved_pin", "") ?: ""
        if (encoded.isEmpty()) return ""
        return try {
            String(Base64.decode(encoded, Base64.DEFAULT))
        } catch (e: Exception) {
            ""
        }
    }
}
