package com.wmods.wppenhacer.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.wmods.wppenhacer.R;
import com.wmods.wppenhacer.activities.base.BaseActivity;
import com.wmods.wppenhacer.database.AppDatabase;
import com.wmods.wppenhacer.database.SchedulerHelper;
import com.wmods.wppenhacer.database.ScheduledMessage;
import com.wmods.wppenhacer.databinding.ActivityEditScheduledMessageBinding;
import com.wmods.wppenhacer.model.ContactPickerResult;
import com.wmods.wppenhacer.preference.ContactPickerPreference;
import com.wmods.wppenhacer.utils.ContactHelper;
import com.wmods.wppenhacer.utils.RealPathUtil;
import com.wmods.wppenhacer.utils.WhatsAppContactPickerLauncher;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditScheduledMessageActivity extends BaseActivity {

    private ActivityEditScheduledMessageBinding binding;
    private ScheduledMessage messageToEdit = null;
    private int messageId = -1;

    private String selectedJid = null;
    private String selectedContactName = null;
    private String selectedMediaPath = null;
    private String selectedMediaType = null;
    private String targetAppPackage = "com.whatsapp"; // Default to WhatsApp
    private String recurrenceType = "ONCE"; // "ONCE", "DAILY", "WEEKLY", "MONTHLY"

    private Calendar scheduledCalendar = Calendar.getInstance();
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditScheduledMessageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Custom Toolbar Actions
        binding.btnBack.setOnClickListener(v -> onBackPressed());
        binding.btnSearch.setOnClickListener(v -> Toast.makeText(this, "Search", Toast.LENGTH_SHORT).show());
        binding.btnInfo.setOnClickListener(v -> Toast.makeText(this, "Message Scheduler Info", Toast.LENGTH_SHORT).show());

        // Setup Contact Picker Click
        binding.btnSelectContact.setOnClickListener(v -> startWhatsAppContactPicker());

        // Setup Segmented App Selector
        binding.tabWhatsapp.setOnClickListener(v -> selectTargetApp("com.whatsapp"));
        binding.tabBusiness.setOnClickListener(v -> selectTargetApp("com.whatsapp.w4b"));

        // Setup Media Attachment
        binding.btnAttachMedia.setOnClickListener(v -> selectMediaFile());
        binding.btnClearMedia.setOnClickListener(v -> clearMedia());

        // Setup Date & Time Pickers
        binding.btnPickDate.setOnClickListener(v -> showDatePicker());
        binding.btnPickTime.setOnClickListener(v -> showTimePicker());

        // Setup Message Character Counter
        binding.editMessageText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.textCharCounter.setText(s.length() + "/65535");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup Repeat Chip Selection
        binding.chipOnce.setOnClickListener(v -> selectRecurrence("ONCE"));
        binding.chipDaily.setOnClickListener(v -> selectRecurrence("DAILY"));
        binding.chipWeekly.setOnClickListener(v -> selectRecurrence("WEEKLY"));
        binding.chipMonthly.setOnClickListener(v -> selectRecurrence("MONTHLY"));

        // Load existing message if editing
        messageId = getIntent().getIntExtra("message_id", -1);
        if (messageId != -1) {
            loadExistingMessage();
        } else {
            updateDateTimeButtons();
            selectTargetApp("com.whatsapp");
            selectRecurrence("ONCE");
        }

        binding.btnSave.setOnClickListener(v -> saveScheduledMessage());
    }

    private void selectTargetApp(String pkg) {
        targetAppPackage = pkg;
        if ("com.whatsapp".equals(pkg)) {
            binding.tabWhatsapp.setBackgroundResource(R.drawable.bg_segmented_selected);
            binding.tabWhatsapp.setTextColor(0xFFFFFFFF);
            binding.tabBusiness.setBackgroundResource(android.R.color.transparent);
            binding.tabBusiness.setTextColor(0x8F8F9CAE);
        } else {
            binding.tabBusiness.setBackgroundResource(R.drawable.bg_segmented_selected);
            binding.tabBusiness.setTextColor(0xFFFFFFFF);
            binding.tabWhatsapp.setBackgroundResource(android.R.color.transparent);
            binding.tabWhatsapp.setTextColor(0x8F8F9CAE);
        }
    }

    private void selectRecurrence(String type) {
        recurrenceType = type;
        
        // Reset all backgrounds
        binding.chipOnce.setBackgroundResource(R.drawable.bg_chip_unselected);
        binding.chipOnce.setTextColor(0x8F8F9CAE);
        binding.chipDaily.setBackgroundResource(R.drawable.bg_chip_unselected);
        binding.chipDaily.setTextColor(0x8F8F9CAE);
        binding.chipWeekly.setBackgroundResource(R.drawable.bg_chip_unselected);
        binding.chipWeekly.setTextColor(0x8F8F9CAE);
        binding.chipMonthly.setBackgroundResource(R.drawable.bg_chip_unselected);
        binding.chipMonthly.setTextColor(0x8F8F9CAE);

        // Highlight selected
        switch (type) {
            case "ONCE":
                binding.chipOnce.setBackgroundResource(R.drawable.bg_chip_selected);
                binding.chipOnce.setTextColor(0xFFFFFFFF);
                break;
            case "DAILY":
                binding.chipDaily.setBackgroundResource(R.drawable.bg_chip_selected);
                binding.chipDaily.setTextColor(0xFFFFFFFF);
                break;
            case "WEEKLY":
                binding.chipWeekly.setBackgroundResource(R.drawable.bg_chip_selected);
                binding.chipWeekly.setTextColor(0xFFFFFFFF);
                break;
            case "MONTHLY":
                binding.chipMonthly.setBackgroundResource(R.drawable.bg_chip_selected);
                binding.chipMonthly.setTextColor(0xFFFFFFFF);
                break;
        }
    }

    private void loadExistingMessage() {
        dbExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            messageToEdit = db.scheduledMessageDao().getById(messageId);
            runOnUiThread(() -> {
                if (messageToEdit == null) {
                    Toast.makeText(this, "Message not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                selectedJid = messageToEdit.getJid();
                selectedContactName = messageToEdit.getContactName();
                binding.textSelectedContact.setText(selectedContactName);

                binding.editMessageText.setText(messageToEdit.getMessageText());

                selectedMediaPath = messageToEdit.getMediaPath();
                selectedMediaType = messageToEdit.getMediaType();
                if (selectedMediaPath != null && !selectedMediaPath.isEmpty()) {
                    binding.textSelectedMedia.setText(new File(selectedMediaPath).getName());
                    binding.btnClearMedia.setVisibility(View.VISIBLE);
                }

                scheduledCalendar.setTimeInMillis(messageToEdit.getScheduledTime());
                updateDateTimeButtons();

                selectTargetApp(messageToEdit.getTargetPackage() != null ? messageToEdit.getTargetPackage() : "com.whatsapp");
                
                String rec = messageToEdit.getRecurrenceType();
                if (!messageToEdit.isRecurring()) {
                    rec = "ONCE";
                }
                selectRecurrence(rec);

                binding.switchAutoDelete.setChecked(messageToEdit.getAutoDelete());
            });
        });
    }

    private void updateDateTimeButtons() {
        SimpleDateFormat dateSdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

        binding.textDateValue.setText(dateSdf.format(scheduledCalendar.getTime()));
        binding.textTimeValue.setText(timeSdf.format(scheduledCalendar.getTime()));
    }

    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            scheduledCalendar.set(Calendar.YEAR, year);
            scheduledCalendar.set(Calendar.MONTH, month);
            scheduledCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateTimeButtons();
        }, scheduledCalendar.get(Calendar.YEAR), scheduledCalendar.get(Calendar.MONTH), scheduledCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            scheduledCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            scheduledCalendar.set(Calendar.MINUTE, minute);
            scheduledCalendar.set(Calendar.SECOND, 0);
            scheduledCalendar.set(Calendar.MILLISECOND, 0);
            updateDateTimeButtons();
        }, scheduledCalendar.get(Calendar.HOUR_OF_DAY), scheduledCalendar.get(Calendar.MINUTE), true).show();
    }

    private void startWhatsAppContactPicker() {
        var installedPackages = WhatsAppContactPickerLauncher.getInstalledWhatsAppPackages(this);
        if (installedPackages.isEmpty()) {
            Toast.makeText(this, "WhatsApp is not installed", Toast.LENGTH_SHORT).show();
            return;
        }

        String targetPackage = installedPackages.contains(targetAppPackage) ? targetAppPackage : installedPackages.get(0);
        try {
            Intent intent = WhatsAppContactPickerLauncher.createPickerIntent(this, targetPackage, "message_scheduler_picker", null);
            startActivityForResult(intent, ContactPickerPreference.REQUEST_CONTACT_PICKER);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to launch contact picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void selectMediaFile() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Select File"), 1002);
        } catch (Exception e) {
            Toast.makeText(this, "Error selecting file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void clearMedia() {
        selectedMediaPath = null;
        selectedMediaType = null;
        binding.textSelectedMedia.setText("Attach Image");
        binding.btnClearMedia.setVisibility(View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ContactPickerPreference.REQUEST_CONTACT_PICKER && resultCode == RESULT_OK && data != null) {
            ArrayList<ContactPickerResult> results = (ArrayList<ContactPickerResult>) data.getSerializableExtra("picker_contacts");
            if (results != null && !results.isEmpty()) {
                ContactPickerResult result = results.get(0);
                selectedJid = result.jid();
                selectedContactName = result.fullName();
                if (selectedContactName == null || selectedContactName.isEmpty()) {
                    selectedContactName = ContactHelper.getContactName(this, selectedJid);
                }
                if (selectedContactName == null || selectedContactName.isEmpty()) {
                    selectedContactName = selectedJid.split("@")[0];
                }
                binding.textSelectedContact.setText(selectedContactName);
            }
        } else if (requestCode == 1002 && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
                try {
                    String realPath = RealPathUtil.getRealFilePath(this, fileUri);
                    if (realPath != null) {
                        selectedMediaPath = realPath;
                        binding.textSelectedMedia.setText(new File(realPath).getName());
                        binding.btnClearMedia.setVisibility(View.VISIBLE);

                        String type = getContentResolver().getType(fileUri);
                        if (type != null) {
                            if (type.startsWith("image/")) {
                                selectedMediaType = "IMAGE";
                            } else if (type.startsWith("video/")) {
                                selectedMediaType = "VIDEO";
                            } else if (type.startsWith("audio/")) {
                                selectedMediaType = "AUDIO";
                            } else {
                                selectedMediaType = "DOCUMENT";
                            }
                        } else {
                            selectedMediaType = "DOCUMENT";
                        }
                    } else {
                        Toast.makeText(this, "Could not resolve file path", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Error getting file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void saveScheduledMessage() {
        if (selectedJid == null) {
            Toast.makeText(this, "Please select a contact or group", Toast.LENGTH_SHORT).show();
            return;
        }

        String text = binding.editMessageText.getText().toString().trim();
        if (text.isEmpty() && (selectedMediaPath == null || selectedMediaPath.isEmpty())) {
            Toast.makeText(this, "Message text or media is required", Toast.LENGTH_SHORT).show();
            return;
        }

        long time = scheduledCalendar.getTimeInMillis();
        if (time <= System.currentTimeMillis() && "ONCE".equals(recurrenceType)) {
            Toast.makeText(this, "Scheduled time must be in the future", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isRecurring = !"ONCE".equals(recurrenceType);
        String finalRecType = isRecurring ? recurrenceType : "ONCE";

        final String finalText = text;
        final long finalTime = time;

        dbExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            ScheduledMessage message;

            if (messageToEdit != null) {
                message = new ScheduledMessage(
                    messageToEdit.getId(),
                    selectedJid,
                    selectedContactName,
                    finalText,
                    selectedMediaPath,
                    selectedMediaType,
                    finalTime,
                    isRecurring,
                    finalRecType,
                    null,
                    "PENDING",
                    binding.switchAutoDelete.isChecked(),
                    targetAppPackage
                );
                db.scheduledMessageDao().update(message);
                runOnUiThread(() -> Toast.makeText(this, "Schedule updated", Toast.LENGTH_SHORT).show());
            } else {
                message = new ScheduledMessage(
                    0,
                    selectedJid,
                    selectedContactName,
                    finalText,
                    selectedMediaPath,
                    selectedMediaType,
                    finalTime,
                    isRecurring,
                    finalRecType,
                    null,
                    "PENDING",
                    binding.switchAutoDelete.isChecked(),
                    targetAppPackage
                );
                db.scheduledMessageDao().insert(message);
                runOnUiThread(() -> Toast.makeText(this, "Schedule created", Toast.LENGTH_SHORT).show());
            }

            SchedulerHelper.INSTANCE.scheduleNextAlarm(this);
            runOnUiThread(this::finish);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}
