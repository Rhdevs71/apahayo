package com.wmods.wppenhacer.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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

    private Calendar scheduledCalendar = Calendar.getInstance();
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditScheduledMessageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Setup Spinners
        String[] recurrenceTypes = {"ONCE", "DAILY", "WEEKLY", "MONTHLY", "SPECIFIC_DAYS"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, recurrenceTypes);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerRecurrenceType.setAdapter(spinnerAdapter);

        binding.spinnerRecurrenceType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = recurrenceTypes[position];
                if ("SPECIFIC_DAYS".equals(selectedType)) {
                    binding.layoutDaysSelector.setVisibility(View.VISIBLE);
                } else {
                    binding.layoutDaysSelector.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Setup Pickers
        binding.btnPickDate.setOnClickListener(v -> showDatePicker());
        binding.btnPickTime.setOnClickListener(v -> showTimePicker());

        // Setup Contact Picker
        binding.btnSelectContact.setOnClickListener(v -> startWhatsAppContactPicker());

        // Setup Media Attachment
        binding.btnAttachMedia.setOnClickListener(v -> selectMediaFile());
        binding.btnClearMedia.setOnClickListener(v -> clearMedia());

        // Setup Recurrence Toggle
        binding.switchRecurring.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.layoutRecurrenceOptions.setVisibility(View.VISIBLE);
            } else {
                binding.layoutRecurrenceOptions.setVisibility(View.GONE);
            }
        });

        // Load existing message if editing
        messageId = getIntent().getIntExtra("message_id", -1);
        if (messageId != -1) {
            loadExistingMessage();
        } else {
            updateDateTimeButtons();
        }

        binding.btnSave.setOnClickListener(v -> saveScheduledMessage());
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
                binding.textSelectedContact.setText(selectedContactName + " (" + selectedJid + ")");

                binding.editMessageText.setText(messageToEdit.getMessageText());

                selectedMediaPath = messageToEdit.getMediaPath();
                selectedMediaType = messageToEdit.getMediaType();
                if (selectedMediaPath != null && !selectedMediaPath.isEmpty()) {
                    binding.textSelectedMedia.setText(selectedMediaPath);
                    binding.btnClearMedia.setVisibility(View.VISIBLE);
                }

                scheduledCalendar.setTimeInMillis(messageToEdit.getScheduledTime());
                updateDateTimeButtons();

                binding.switchRecurring.setChecked(messageToEdit.isRecurring());
                if (messageToEdit.isRecurring()) {
                    binding.layoutRecurrenceOptions.setVisibility(View.VISIBLE);
                    int selection = 0;
                    switch (messageToEdit.getRecurrenceType()) {
                        case "DAILY" -> selection = 1;
                        case "WEEKLY" -> selection = 2;
                        case "MONTHLY" -> selection = 3;
                        case "SPECIFIC_DAYS" -> {
                            selection = 4;
                            binding.layoutDaysSelector.setVisibility(View.VISIBLE);
                            loadDaysCheckboxes(messageToEdit.getRecurrenceDays());
                        }
                    }
                    binding.spinnerRecurrenceType.setSelection(selection);
                }

                binding.switchAutoDelete.setChecked(messageToEdit.getAutoDelete());
            });
        });
    }

    private void loadDaysCheckboxes(String daysCsv) {
        if (daysCsv == null || daysCsv.isEmpty()) return;
        String[] days = daysCsv.split(",");
        for (String day : days) {
            try {
                int d = Integer.parseInt(day.trim());
                if (d == 1) binding.checkboxSun.setChecked(true);
                if (d == 2) binding.checkboxMon.setChecked(true);
                if (d == 3) binding.checkboxTue.setChecked(true);
                if (d == 4) binding.checkboxWed.setChecked(true);
                if (d == 5) binding.checkboxThu.setChecked(true);
                if (d == 6) binding.checkboxFri.setChecked(true);
                if (d == 7) binding.checkboxSat.setChecked(true);
            } catch (Exception ignored) {}
        }
    }

    private String getSelectedDaysCsv() {
        ArrayList<String> days = new ArrayList<>();
        if (binding.checkboxSun.isChecked()) days.add("1");
        if (binding.checkboxMon.isChecked()) days.add("2");
        if (binding.checkboxTue.isChecked()) days.add("3");
        if (binding.checkboxWed.isChecked()) days.add("4");
        if (binding.checkboxThu.isChecked()) days.add("5");
        if (binding.checkboxFri.isChecked()) days.add("6");
        if (binding.checkboxSat.isChecked()) days.add("7");
        return String.join(",", days);
    }

    private void updateDateTimeButtons() {
        SimpleDateFormat dateSdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

        binding.btnPickDate.setText("Date: " + dateSdf.format(scheduledCalendar.getTime()));
        binding.btnPickTime.setText("Time: " + timeSdf.format(scheduledCalendar.getTime()));
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

        String targetPackage = installedPackages.get(0);
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
        binding.textSelectedMedia.setText("No file attached (Optional)");
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
                binding.textSelectedContact.setText(selectedContactName + " (" + selectedJid + ")");
            }
        } else if (requestCode == 1002 && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
                try {
                    String realPath = RealPathUtil.getRealFilePath(this, fileUri);
                    if (realPath != null) {
                        selectedMediaPath = realPath;
                        binding.textSelectedMedia.setText(realPath);
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
        if (time <= System.currentTimeMillis() && !binding.switchRecurring.isChecked()) {
            Toast.makeText(this, "Scheduled time must be in the future", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isRecurring = binding.switchRecurring.isChecked();
        String recType = "ONCE";
        String recDays = null;

        if (isRecurring) {
            recType = binding.spinnerRecurrenceType.getSelectedItem().toString();
            if ("SPECIFIC_DAYS".equals(recType)) {
                recDays = getSelectedDaysCsv();
                if (recDays.isEmpty()) {
                    Toast.makeText(this, "Please select at least one day", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

        final String finalText = text;
        final long finalTime = time;
        final String finalRecType = recType;
        final String finalRecDays = recDays;

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
                    finalRecDays,
                    "PENDING",
                    binding.switchAutoDelete.isChecked()
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
                    finalRecDays,
                    "PENDING",
                    binding.switchAutoDelete.isChecked()
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
