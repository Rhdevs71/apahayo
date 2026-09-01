package com.rhdevs.rhpatch.activity

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.rhdevs.rhpatch.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ElementHiderActivity : AppCompatActivity() {

    private lateinit var appSpinner: Spinner
    private lateinit var idInput: EditText
    private lateinit var btnSave: MaterialButton

    private val apps = mapOf(
        "WhatsApp" to "com.whatsapp",
        "WhatsApp Business" to "com.whatsapp.w4b",
        "Instagram" to "com.instagram.android",
        "TikTok" to "com.zhiliaoapp.musically"
    )
    
    private val appNames = apps.keys.toList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_element_hider)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        appSpinner = findViewById(R.id.app_spinner)
        idInput = findViewById(R.id.id_input)
        btnSave = findViewById(R.id.btn_save)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, appNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        appSpinner.adapter = adapter

        // Load existing json if possible, though handling dynamic switching requires a listener
        val prefs = getSharedPreferences("universal_hider", android.content.Context.MODE_PRIVATE)
        var currentJson = JSONObject()
        try {
            val savedText = prefs.getString("hider_data", "{}") ?: "{}"
            currentJson = JSONObject(savedText)
        } catch (e: Exception) {}

        appSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val pkgName = apps[appNames[position]]!!
                if (currentJson.has(pkgName)) {
                    val array = currentJson.getJSONArray(pkgName)
                    val list = mutableListOf<String>()
                    for (i in 0 until array.length()) {
                        list.add(array.getString(i))
                    }
                    idInput.setText(list.joinToString(", "))
                } else {
                    idInput.setText("")
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        btnSave.setOnClickListener {
            val pkgName = apps[appNames[appSpinner.selectedItemPosition]]!!
            val rawInput = idInput.text.toString()
            
            // Re-read prefs to avoid overwriting other apps' data
            try {
                val savedText = prefs.getString("hider_data", "{}") ?: "{}"
                currentJson = JSONObject(savedText)
            } catch (e: Exception) {}
            
            val idsArray = JSONArray()
            rawInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach {
                idsArray.put(it)
            }
            
            currentJson.put(pkgName, idsArray)
            
            try {
                prefs.edit().putString("hider_data", currentJson.toString(4)).apply()
                // Make world readable for XSharedPreferences
                val file = java.io.File(filesDir.parentFile, "shared_prefs/universal_hider.xml")
                if (file.exists()) {
                    file.setReadable(true, false)
                }
                Toast.makeText(this, "Berhasil disimpan untuk $pkgName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal menyimpan: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
