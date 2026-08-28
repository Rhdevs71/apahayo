package com.rhdevs.rhpatch.ui

import android.content.Context
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.rhdevs.rhpatch.R
import com.rhdevs.rhpatch.activities.base.BaseActivity

class ScreenLockConfigActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_lock_config)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val prefs = getSharedPreferences("screen_lock_prefs", Context.MODE_PRIVATE)
        val savedType = prefs.getString("lock_type", "swipe") ?: "swipe"
        val savedEncoded = prefs.getString("saved_pin", "") ?: ""
        val savedPin = try {
            if (savedEncoded.isNotEmpty()) String(Base64.decode(savedEncoded, Base64.DEFAULT)) else ""
        } catch (e: Exception) { "" }

        val radioGroup = findViewById<RadioGroup>(R.id.radio_group_lock)
        val btnNone = findViewById<RadioButton>(R.id.radio_none)
        val btnSwipe = findViewById<RadioButton>(R.id.radio_swipe)
        val btnPin = findViewById<RadioButton>(R.id.radio_pin)
        val btnPassNum = findViewById<RadioButton>(R.id.radio_pass_num)
        val btnPassAlphanum = findViewById<RadioButton>(R.id.radio_pass_alphanum)
        val input = findViewById<EditText>(R.id.input_pin)
        val btnSave = findViewById<Button>(R.id.btn_save)

        when (savedType) {
            "none" -> btnNone.isChecked = true
            "swipe" -> btnSwipe.isChecked = true
            "pin" -> btnPin.isChecked = true
            "password_num" -> btnPassNum.isChecked = true
            "password_alphanum" -> btnPassAlphanum.isChecked = true
            else -> btnSwipe.isChecked = true
        }

        if (savedPin.isNotEmpty()) {
            input.setText(savedPin)
            input.setSelection(savedPin.length)
        }

        btnSave.setOnClickListener {
            val selectedType = when (radioGroup.checkedRadioButtonId) {
                R.id.radio_none -> "none"
                R.id.radio_swipe -> "swipe"
                R.id.radio_pin -> "pin"
                R.id.radio_pass_num -> "password_num"
                R.id.radio_pass_alphanum -> "password_alphanum"
                else -> "swipe"
            }
            
            val pinText = input.text.toString()
            if ((selectedType == "pin" || selectedType == "password_num" || selectedType == "password_alphanum") && pinText.isEmpty()) {
                Toast.makeText(this, "PIN/Sandi tidak boleh kosong!", Toast.LENGTH_SHORT).show()
            } else {
                val encoded = Base64.encodeToString(pinText.toByteArray(), Base64.DEFAULT)
                prefs.edit()
                    .putString("lock_type", selectedType)
                    .putString("saved_pin", encoded)
                    .apply()
                Toast.makeText(this, "Konfigurasi Kunci Layar berhasil disimpan!", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}