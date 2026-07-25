package com.rhdevs.rhpatch.activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText
import com.wmods.wppenhacer.R
import com.rhdevs.rhpatch.scheduler.UniversalScheduler
import com.rhdevs.rhpatch.scheduler.UniversalTask
import com.rhdevs.rhpatch.scheduler.db.UniversalTaskEntity
import com.wmods.wppenhacer.database.AppDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ComposeScheduleActivity : AppCompatActivity() {

    private var selectedTimeMillis: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compose_schedule)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val spinnerTargetApp = findViewById<Spinner>(R.id.spinner_target_app)
        val etRecipient = findViewById<TextInputEditText>(R.id.et_recipient)
        val tilSubject = findViewById<android.view.View>(R.id.til_subject)
        val etSubject = findViewById<TextInputEditText>(R.id.et_subject)
        val etMessage = findViewById<TextInputEditText>(R.id.et_message)
        val btnPickTime = findViewById<android.view.View>(R.id.btn_pick_time)
        val tvSelectedTime = findViewById<TextView>(R.id.tv_selected_time)
        val btnSave = findViewById<Button>(R.id.btn_save_schedule)
        val btnPickContact = findViewById<android.widget.ImageButton>(R.id.btn_pick_contact)

        val targetOptions = arrayOf(
            "WhatsApp", "Telegram", "Telegram Group", "SMS", "Phone Call", "Email", "Facebook Messenger", "Instagram", "Discord"
        )
        val targetValues = arrayOf(
            "whatsapp", "telegram", "telegram_group", "sms", "call", "email", "messenger", "instagram", "discord"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, targetOptions)
        spinnerTargetApp.adapter = adapter

        spinnerTargetApp.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (targetValues[position] == "email") {
                    tilSubject.visibility = android.view.View.VISIBLE
                } else {
                    tilSubject.visibility = android.view.View.GONE
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Pre-fill for Edit Task
        val editTaskId = intent.getIntExtra("edit_task_id", -1)
        if (editTaskId != -1) {
            val editTargetApp = intent.getStringExtra("edit_target_app")
            val editRecipient = intent.getStringExtra("edit_recipient")
            val editMessage = intent.getStringExtra("edit_message")
            val editTime = intent.getLongExtra("edit_time", 0L)

            etRecipient.setText(editRecipient)
            
            if (editTargetApp == "email" && editMessage?.contains("|||") == true) {
                val parts = editMessage.split("|||", limit = 2)
                etSubject.setText(parts[0])
                etMessage.setText(parts[1])
            } else {
                etMessage.setText(editMessage)
            }
            
            val index = targetValues.indexOf(editTargetApp)
            if (index != -1) {
                spinnerTargetApp.setSelection(index)
            }
            
            if (editTime > 0) {
                selectedTimeMillis = editTime
                val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                tvSelectedTime.text = sdf.format(editTime)
            }
            btnSave.text = "Perbarui Jadwal"
        }

        btnPickContact.setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(android.Manifest.permission.READ_CONTACTS), 101)
                    return@setOnClickListener
                }
            }
            val pickIntent = android.content.Intent(android.content.Intent.ACTION_PICK, android.provider.ContactsContract.Contacts.CONTENT_URI)
            startActivityForResult(pickIntent, 102)
        }

        btnPickTime.setOnClickListener {
            val current = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                TimePickerDialog(this, { _, hourOfDay, minute ->
                    val selectedCal = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth, hourOfDay, minute, 0)
                    }
                    selectedTimeMillis = selectedCal.timeInMillis
                    
                    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    tvSelectedTime.text = sdf.format(selectedCal.time)
                }, current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE), true).show()
            }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnSave.setOnClickListener {
            val recipient = etRecipient.text.toString().trim()
            val message = etMessage.text.toString().trim()
            
            if (recipient.isEmpty() || message.isEmpty()) {
                Toast.makeText(this, "Penerima dan Pesan tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedTimeMillis <= System.currentTimeMillis()) {
                Toast.makeText(this, "Waktu pengiriman harus di masa depan!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Route to Permission Check first
            val permIntent = android.content.Intent(this, PermissionCheckActivity::class.java)
            startActivityForResult(permIntent, 201)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 102 && resultCode == android.app.Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val idIndex = it.getColumnIndex(android.provider.ContactsContract.Contacts._ID)
                        val hasPhoneIndex = it.getColumnIndex(android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER)
                        if (idIndex != -1 && hasPhoneIndex != -1) {
                            val id = it.getString(idIndex)
                            val hasPhone = it.getInt(hasPhoneIndex) > 0
                            if (hasPhone) {
                                val pCursor = contentResolver.query(
                                    android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    null,
                                    android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                    arrayOf(id),
                                    null
                                )
                                pCursor?.use { p ->
                                    if (p.moveToFirst()) {
                                        val numberIndex = p.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                                        if (numberIndex != -1) {
                                            val number = p.getString(numberIndex)
                                            val etRecipient = findViewById<TextInputEditText>(R.id.et_recipient)
                                            val current = etRecipient.text.toString().trim()
                                            if (current.isEmpty()) {
                                                etRecipient.setText(number)
                                            } else {
                                                etRecipient.setText("$current,$number")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (requestCode == 201 && resultCode == android.app.Activity.RESULT_OK) {
            // Permissions are granted, proceed to save task
            val spinnerTargetApp = findViewById<Spinner>(R.id.spinner_target_app)
            val etRecipient = findViewById<TextInputEditText>(R.id.et_recipient)
            val etSubject = findViewById<TextInputEditText>(R.id.et_subject)
            val etMessage = findViewById<TextInputEditText>(R.id.et_message)

            val recipient = etRecipient.text.toString().trim()
            var message = etMessage.text.toString().trim()
            val subject = etSubject.text.toString().trim()

            val targetValues = arrayOf(
                "whatsapp", "telegram", "telegram_group", "sms", "call", "email", "messenger", "instagram", "discord"
            )
            val targetApp = targetValues[spinnerTargetApp.selectedItemPosition]
            
            if (targetApp == "email" && subject.isNotEmpty()) {
                message = "$subject|||$message"
            }

            // Save to DB
            val editTaskId = intent.getIntExtra("edit_task_id", -1)
            val db = AppDatabase.getInstance(this).universalSchedulerDao()
            val finalId: Int
            
            if (editTaskId != -1) {
                val task = UniversalTaskEntity(
                    id = editTaskId,
                    targetApp = targetApp,
                    recipientName = "Unknown",
                    recipientPhoneOrEmail = recipient,
                    message = message,
                    triggerTimeMillis = selectedTimeMillis,
                    status = "PENDING"
                )
                db.updateTask(task)
                finalId = editTaskId
            } else {
                val task = UniversalTaskEntity(
                    targetApp = targetApp,
                    recipientName = "Unknown",
                    recipientPhoneOrEmail = recipient,
                    message = message,
                    triggerTimeMillis = selectedTimeMillis,
                    status = "PENDING"
                )
                val insertedId = db.insertTask(task)
                finalId = insertedId.toInt()
            }

            // Schedule Alarm
            UniversalScheduler.scheduleTask(
                context = this,
                task = UniversalTask(
                    id = finalId,
                    targetApp = targetApp,
                    targetContact = recipient,
                    messageText = message,
                    timestamp = selectedTimeMillis
                )
            )

            Toast.makeText(this, if (editTaskId != -1) "Jadwal berhasil diperbarui!" else "Jadwal berhasil disimpan!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
