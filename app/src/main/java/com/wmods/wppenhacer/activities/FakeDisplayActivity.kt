package com.wmods.wppenhacer.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.wmods.wppenhacer.R
import com.wmods.wppenhacer.activities.base.BaseActivity
import com.wmods.wppenhacer.databinding.ActivityFakeDisplayBinding
import com.wmods.wppenhacer.model.ContactPickerResult
import com.wmods.wppenhacer.preference.ContactPickerPreference
import com.wmods.wppenhacer.utils.ContactHelper
import com.wmods.wppenhacer.utils.RealPathUtil
import com.wmods.wppenhacer.utils.WhatsAppContactPickerLauncher
import java.io.File

class FakeDisplayActivity : BaseActivity() {

    private lateinit var binding: ActivityFakeDisplayBinding
    private var selectedJid: String? = null
    private var selectedContactName: String? = null
    private var selectedPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFakeDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar Back button
        binding.btnBack.setOnClickListener { onBackPressed() }

        // Switch Self (Me) change listener
        binding.switchIsSelf.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.layoutContactSelect.visibility = View.GONE
                selectedJid = "me"
                selectedContactName = "Self / Me"
                loadSettingsForJid("me")
            } else {
                binding.layoutContactSelect.visibility = View.VISIBLE
                selectedJid = null
                selectedContactName = null
                resetInputFields()
            }
        }

        // Contact Select Click
        binding.btnSelectContact.setOnClickListener { startWhatsAppContactPicker() }

        // Switch Name Override listener
        binding.switchNameEnabled.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutNameInput.alpha = if (isChecked) 1.0f else 0.5f
            binding.editFakeName.isEnabled = isChecked
        }

        // Switch Photo Override listener
        binding.switchPhotoEnabled.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutPhotoInput.alpha = if (isChecked) 1.0f else 0.5f
            binding.btnSelectPhoto.isClickable = isChecked
            binding.btnSelectPhoto.isFocusable = isChecked
        }

        // Select Photo Layout Click (only clickable if enabled)
        binding.layoutPhotoInput.setOnClickListener {
            if (binding.switchPhotoEnabled.isChecked) {
                selectProfilePhotoFile()
            }
        }

        // Save Button Click
        binding.btnSave.setOnClickListener { saveFakeDisplaySettings() }
    }

    private fun resetInputFields() {
        binding.textSelectedContact.text = "Choose WhatsApp contact"
        binding.textSelectedContact.setTextColor(0x8F8F9CAE.toInt())
        binding.switchNameEnabled.isChecked = false
        binding.editFakeName.setText("")
        binding.switchPhotoEnabled.isChecked = false
        binding.textSelectedPhoto.text = "Choose fake photo"
        binding.textSelectedPhoto.setTextColor(0x8F8F9CAE.toInt())
        selectedPhotoPath = null
    }

    private fun loadSettingsForJid(jid: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val nameEnabled = prefs.getBoolean("fake_display_name_enabled_$jid", false)
        val nameValue = prefs.getString("fake_display_name_$jid", "") ?: ""
        val photoEnabled = prefs.getBoolean("fake_display_photo_enabled_$jid", false)
        val photoValue = prefs.getString("fake_display_photo_$jid", "") ?: ""

        binding.switchNameEnabled.isChecked = nameEnabled
        binding.editFakeName.setText(nameValue)
        binding.switchPhotoEnabled.isChecked = photoEnabled
        if (photoValue.isNotEmpty()) {
            selectedPhotoPath = photoValue
            binding.textSelectedPhoto.text = File(photoValue).name
            binding.textSelectedPhoto.setTextColor(0xFFE3E6EB.toInt())
        } else {
            selectedPhotoPath = null
            binding.textSelectedPhoto.text = "Choose fake photo"
            binding.textSelectedPhoto.setTextColor(0x8F8F9CAE.toInt())
        }
    }

    private fun startWhatsAppContactPicker() {
        val installedPackages = WhatsAppContactPickerLauncher.getInstalledWhatsAppPackages(this)
        if (installedPackages.isEmpty()) {
            Toast.makeText(this, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
            return
        }
        val targetPackage = installedPackages[0]
        try {
            val intent = WhatsAppContactPickerLauncher.createPickerIntent(this, targetPackage, "fake_display_picker", null)
            startActivityForResult(intent, ContactPickerPreference.REQUEST_CONTACT_PICKER)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to launch contact picker: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectProfilePhotoFile() {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(Intent.createChooser(intent, "Select Profile Image"), 2001)
        } catch (e: Exception) {
            Toast.makeText(this, "Error selecting photo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ContactPickerPreference.REQUEST_CONTACT_PICKER && resultCode == Activity.RESULT_OK && data != null) {
            val results = data.getSerializableExtra("picker_contacts") as? ArrayList<ContactPickerResult>
            if (results != null && results.isNotEmpty()) {
                val result = results[0]
                selectedJid = result.jid()
                selectedContactName = result.fullName() ?: ContactHelper.getContactName(this, selectedJid) ?: selectedJid?.split("@")?.get(0)
                binding.textSelectedContact.text = selectedContactName
                binding.textSelectedContact.setTextColor(0xFFE3E6EB.toInt())

                selectedJid?.let { loadSettingsForJid(it) }
            }
        } else if (requestCode == 2001 && resultCode == Activity.RESULT_OK && data != null) {
            val fileUri = data.data
            if (fileUri != null) {
                try {
                    val realPath = RealPathUtil.getRealFilePath(this, fileUri)
                    if (realPath != null) {
                        selectedPhotoPath = realPath
                        binding.textSelectedPhoto.text = File(realPath).name
                        binding.textSelectedPhoto.setTextColor(0xFFE3E6EB.toInt())
                    } else {
                        Toast.makeText(this, "Could not resolve photo path", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error loading photo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveFakeDisplaySettings() {
        val jid = selectedJid
        if (jid == null) {
            Toast.makeText(this, "Please select a contact or choose self", Toast.LENGTH_SHORT).show()
            return
        }

        val nameEnabled = binding.switchNameEnabled.isChecked
        val fakeName = binding.editFakeName.text.toString().trim()
        if (nameEnabled && fakeName.isEmpty()) {
            Toast.makeText(this, "Please enter a fake name", Toast.LENGTH_SHORT).show()
            return
        }

        val photoEnabled = binding.switchPhotoEnabled.isChecked
        val photoPath = selectedPhotoPath
        if (photoEnabled && (photoPath == null || photoPath.isEmpty())) {
            Toast.makeText(this, "Please select a fake photo", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = prefs.edit()

        editor.putBoolean("fake_display_name_enabled_$jid", nameEnabled)
        editor.putString("fake_display_name_$jid", if (nameEnabled) fakeName else "")
        editor.putBoolean("fake_display_photo_enabled_$jid", photoEnabled)
        editor.putString("fake_display_photo_$jid", if (photoEnabled) photoPath else "")

        // Add this JID to our active list of overrides
        val currentListStr = prefs.getString("fake_display_contacts_list", "") ?: ""
        val currentList = currentListStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
        currentList.add(jid)
        editor.putString("fake_display_contacts_list", currentList.joinToString(","))

        editor.apply()

        Toast.makeText(this, "Fake display settings saved successfully", Toast.LENGTH_SHORT).show()
        finish()
    }
}
