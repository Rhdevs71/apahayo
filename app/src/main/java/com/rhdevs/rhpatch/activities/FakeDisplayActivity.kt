package com.rhdevs.rhpatch.activities

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.rhdevs.rhpatch.R
import com.rhdevs.rhpatch.activities.base.BaseActivity
import com.rhdevs.rhpatch.databinding.ActivityFakeDisplayBinding
import com.rhdevs.rhpatch.model.ContactPickerResult
import com.rhdevs.rhpatch.preference.ContactPickerPreference
import com.rhdevs.rhpatch.utils.ContactHelper
import com.rhdevs.rhpatch.utils.RealPathUtil
import com.rhdevs.rhpatch.utils.WhatsAppContactPickerLauncher
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FakeDisplayActivity : BaseActivity() {

    private lateinit var binding: ActivityFakeDisplayBinding
    private var selectedJid: String? = null
    private var selectedContactName: String? = null
    private var selectedPhotoPath: String? = null

    // Calendar for fake message and call log injection
    private val chatCalendar = Calendar.getInstance()
    private val callCalendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFakeDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar Back button
        binding.btnBack.setOnClickListener { onBackPressed() }

        // Check if JID is passed from WhatsApp menu
        val chatJidExtra = intent.getStringExtra("CHAT_JID")
        if (chatJidExtra != null) {
            selectedJid = chatJidExtra
            selectedContactName = ContactHelper.getContactName(this, chatJidExtra) ?: chatJidExtra.split("@").getOrNull(0) ?: "Contact"
            binding.textSelectedContact.text = selectedContactName
            binding.textSelectedContact.setTextColor(0xFFE3E6EB.toInt())
            binding.switchIsSelf.isChecked = false
            binding.layoutContactSelect.visibility = View.VISIBLE
            loadSettingsForJid(chatJidExtra)
        }

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

        // Set up tab click listeners
        setupTabs()

        // Set up Fake Chat elements
        setupFakeChatSection()

        // Set up Fake Call elements
        setupFakeCallSection()
    }

    private fun setupTabs() {
        binding.tabDisplay.setOnClickListener {
            selectTab(0)
        }
        binding.tabChat.setOnClickListener {
            selectTab(1)
        }
        binding.tabCall.setOnClickListener {
            selectTab(2)
        }
    }

    private fun selectTab(index: Int) {
        // Reset backgrounds
        binding.tabDisplay.setBackgroundResource(0)
        binding.tabChat.setBackgroundResource(0)
        binding.tabCall.setBackgroundResource(0)
        binding.tabDisplay.setTextColor(0x8F8F9CAE.toInt())
        binding.tabChat.setTextColor(0x8F8F9CAE.toInt())
        binding.tabCall.setTextColor(0x8F8F9CAE.toInt())

        // Set selected layout
        binding.layoutSectionDisplay.visibility = View.GONE
        binding.layoutSectionChat.visibility = View.GONE
        binding.layoutSectionCall.visibility = View.GONE

        when (index) {
            0 -> {
                binding.tabDisplay.setBackgroundResource(R.drawable.bg_segmented_selected)
                binding.tabDisplay.setTextColor(0xFFFFFFFF.toInt())
                binding.layoutSectionDisplay.visibility = View.VISIBLE
            }
            1 -> {
                binding.tabChat.setBackgroundResource(R.drawable.bg_segmented_selected)
                binding.tabChat.setTextColor(0xFFFFFFFF.toInt())
                binding.layoutSectionChat.visibility = View.VISIBLE
            }
            2 -> {
                binding.tabCall.setBackgroundResource(R.drawable.bg_segmented_selected)
                binding.tabCall.setTextColor(0xFFFFFFFF.toInt())
                binding.layoutSectionCall.visibility = View.VISIBLE
            }
        }
    }

    private fun setupFakeChatSection() {
        // Show/hide status option depending on Sender
        binding.rgMsgSender.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radio_msg_me) {
                binding.layoutMsgStatus.visibility = View.VISIBLE
            } else {
                binding.layoutMsgStatus.visibility = View.GONE
            }
        }

        // Update time labels
        updateChatTimeLabel()

        binding.btnMsgDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                chatCalendar.set(Calendar.YEAR, year)
                chatCalendar.set(Calendar.MONTH, month)
                chatCalendar.set(Calendar.DAY_OF_MONTH, day)
                updateChatTimeLabel()
            }, chatCalendar.get(Calendar.YEAR), chatCalendar.get(Calendar.MONTH), chatCalendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnMsgTime.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                chatCalendar.set(Calendar.HOUR_OF_DAY, hour)
                chatCalendar.set(Calendar.MINUTE, minute)
                updateChatTimeLabel()
            }, chatCalendar.get(Calendar.HOUR_OF_DAY), chatCalendar.get(Calendar.MINUTE), true).show()
        }

        // Inject message click listener
        binding.btnInjectMessage.setOnClickListener {
            Toast.makeText(this, "Fitur dinonaktifkan sementara karena menyebabkan corrupt database WhatsApp", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateChatTimeLabel() {
        binding.textMsgTimestampPreview.text = "Timestamp: " + dateFormat.format(chatCalendar.time)
    }

    private fun setupFakeCallSection() {
        // Duration slider listener
        binding.sliderCallDuration.addOnChangeListener { _, value, _ ->
            val totalSec = value.toInt()
            val min = totalSec / 60
            val sec = totalSec % 60
            binding.textCallDurationPreview.text = String.format("%d min %d sec", min, sec)
        }

        // Update time label
        updateCallTimeLabel()

        binding.btnCallDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                callCalendar.set(Calendar.YEAR, year)
                callCalendar.set(Calendar.MONTH, month)
                callCalendar.set(Calendar.DAY_OF_MONTH, day)
                updateCallTimeLabel()
            }, callCalendar.get(Calendar.YEAR), callCalendar.get(Calendar.MONTH), callCalendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnCallTime.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                callCalendar.set(Calendar.HOUR_OF_DAY, hour)
                callCalendar.set(Calendar.MINUTE, minute)
                updateCallTimeLabel()
            }, callCalendar.get(Calendar.HOUR_OF_DAY), callCalendar.get(Calendar.MINUTE), true).show()
        }

        // Inject Call history listener
        binding.btnInjectCall.setOnClickListener {
            Toast.makeText(this, "Fitur dinonaktifkan sementara karena menyebabkan corrupt database WhatsApp", Toast.LENGTH_LONG).show()
        }

        // Launch simulated incoming call screen
        binding.btnLaunchCallSimulator.setOnClickListener {
            val name = if (binding.switchNameEnabled.isChecked) {
                binding.editFakeName.text.toString().trim()
            } else {
                selectedContactName
            } ?: "Tokoh Politik"

            val photo = if (binding.switchPhotoEnabled.isChecked) {
                selectedPhotoPath ?: ""
            } else {
                ""
            }

            val isVideo = binding.radioCallSimVideo.isChecked

            val intent = Intent(this, FakeCallActivity::class.java).apply {
                putExtra("CONTACT_NAME", name)
                putExtra("PHOTO_PATH", photo)
                putExtra("IS_VIDEO", isVideo)
            }
            startActivity(intent)
        }
    }

    private fun updateCallTimeLabel() {
        binding.textCallTimestampPreview.text = "Timestamp: " + dateFormat.format(callCalendar.time)
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
