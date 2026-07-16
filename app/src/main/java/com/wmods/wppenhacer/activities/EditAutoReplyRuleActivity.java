package com.wmods.wppenhacer.activities;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.wmods.wppenhacer.R;
import com.wmods.wppenhacer.activities.base.BaseActivity;
import com.wmods.wppenhacer.database.AppDatabase;
import com.wmods.wppenhacer.database.AutoReplyRule;
import com.wmods.wppenhacer.databinding.ActivityEditAutoReplyRuleBinding;
import com.wmods.wppenhacer.model.ContactPickerResult;
import com.wmods.wppenhacer.preference.ContactPickerPreference;
import com.wmods.wppenhacer.utils.ContactHelper;
import com.wmods.wppenhacer.utils.WhatsAppContactPickerLauncher;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class EditAutoReplyRuleActivity extends BaseActivity {

    private ActivityEditAutoReplyRuleBinding binding;
    private AutoReplyRule ruleToEdit = null;
    private int ruleId = -1;

    private String activeHoursStart = "09:00";
    private String activeHoursEnd = "17:00";
    private String selectedJidsCsv = "";

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditAutoReplyRuleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Custom Toolbar Action
        binding.btnBack.setOnClickListener(v -> onBackPressed());
        binding.btnSearch.setOnClickListener(v -> Toast.makeText(this, "Search", Toast.LENGTH_SHORT).show());
        binding.btnInfo.setOnClickListener(v -> Toast.makeText(this, "Auto Reply Info", Toast.LENGTH_SHORT).show());

        // Setup Spinners
        String[] targetTypes = {"ALL", "CONTACTS", "GROUPS", "NON_CONTACTS", "SPECIFIC_CONTACTS"};
        ArrayAdapter<String> targetAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, targetTypes);
        targetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerTargetType.setAdapter(targetAdapter);

        // Control Specific Contacts view visibility
        binding.spinnerTargetType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String type = targetTypes[position];
                if ("SPECIFIC_CONTACTS".equals(type)) {
                    binding.labelSelectedContacts.setVisibility(View.VISIBLE);
                    binding.layoutSelectedContacts.setVisibility(View.VISIBLE);
                    binding.btnSelectContacts.setVisibility(View.VISIBLE);
                } else {
                    binding.labelSelectedContacts.setVisibility(View.GONE);
                    binding.layoutSelectedContacts.setVisibility(View.GONE);
                    binding.btnSelectContacts.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Setup Time Windows Switch (Apply only on schedule)
        binding.switchActiveHours.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.layoutActiveHours.setVisibility(View.VISIBLE);
            } else {
                binding.layoutActiveHours.setVisibility(View.GONE);
            }
        });

        binding.btnActiveStart.setOnClickListener(v -> showTimePicker(true));
        binding.btnActiveEnd.setOnClickListener(v -> showTimePicker(false));

        // Setup Contacts Selection Click
        binding.btnSelectContacts.setOnClickListener(v -> startWhatsAppContactPicker());

        // Load existing rule if editing
        ruleId = getIntent().getIntExtra("rule_id", -1);
        if (ruleId != -1) {
            loadExistingRule();
        } else {
            updateTimeButtons();
        }

        binding.btnSave.setOnClickListener(v -> saveAutoReplyRule());
    }

    private void loadExistingRule() {
        dbExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            ruleToEdit = db.autoReplyRuleDao().getById(ruleId);
            runOnUiThread(() -> {
                if (ruleToEdit == null) {
                    Toast.makeText(this, "Rule not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                binding.switchRuleEnabled.setChecked(ruleToEdit.isEnabled());
                binding.editKeywords.setText(ruleToEdit.getKeywords());

                // Set radio matching type
                if ("REGEX".equals(ruleToEdit.getMatchingType())) {
                    binding.radioRegex.setChecked(true);
                } else {
                    binding.radioWildcard.setChecked(true);
                }

                binding.checkboxIgnoreCase.setChecked(ruleToEdit.getIgnoreCase());
                binding.editReplyText.setText(ruleToEdit.getReplyText());

                int targetSelection = 0;
                switch (ruleToEdit.getTargetType()) {
                    case "CONTACTS" -> targetSelection = 1;
                    case "GROUPS" -> targetSelection = 2;
                    case "NON_CONTACTS" -> targetSelection = 3;
                    case "SPECIFIC_CONTACTS" -> targetSelection = 4;
                }
                binding.spinnerTargetType.setSelection(targetSelection);

                selectedJidsCsv = ruleToEdit.getTargetContacts() != null ? ruleToEdit.getTargetContacts() : "";
                updateContactsFieldText();

                boolean hasActiveHours = ruleToEdit.getActiveHoursStart() != null && ruleToEdit.getActiveHoursEnd() != null;
                binding.switchActiveHours.setChecked(hasActiveHours);
                if (hasActiveHours) {
                    binding.layoutActiveHours.setVisibility(View.VISIBLE);
                    activeHoursStart = ruleToEdit.getActiveHoursStart();
                    activeHoursEnd = ruleToEdit.getActiveHoursEnd();
                }
                updateTimeButtons();

                binding.switchQuoteOriginal.setChecked(ruleToEdit.getQuoteOriginal());
                binding.switchIsAi.setChecked(ruleToEdit.isAi());
                binding.switchIsForward.setChecked(ruleToEdit.isForward());
                binding.seekBarDelay.setProgress(ruleToEdit.getDelaySeconds());
            });
        });
    }

    private void updateTimeButtons() {
        binding.btnActiveStart.setText("Start: " + activeHoursStart);
        binding.btnActiveEnd.setText("End: " + activeHoursEnd);
    }

    private void showTimePicker(boolean isStart) {
        String currentStr = isStart ? activeHoursStart : activeHoursEnd;
        String[] parts = currentStr.split(":");
        int currentHour = Integer.parseInt(parts[0]);
        int currentMin = Integer.parseInt(parts[1]);

        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String formatted = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            if (isStart) {
                activeHoursStart = formatted;
            } else {
                activeHoursEnd = formatted;
            }
            updateTimeButtons();
        }, currentHour, currentMin, true).show();
    }

    private void startWhatsAppContactPicker() {
        var installedPackages = WhatsAppContactPickerLauncher.getInstalledWhatsAppPackages(this);
        if (installedPackages.isEmpty()) {
            Toast.makeText(this, "WhatsApp is not installed", Toast.LENGTH_SHORT).show();
            return;
        }

        String targetPackage = installedPackages.get(0);
        try {
            ArrayList<String> preSelected = new ArrayList<>();
            if (selectedJidsCsv != null && !selectedJidsCsv.isEmpty()) {
                for (String jid : selectedJidsCsv.split(",")) {
                    if (!jid.trim().isEmpty()) {
                        preSelected.add(jid.trim());
                    }
                }
            }
            Intent intent = WhatsAppContactPickerLauncher.createPickerIntent(this, targetPackage, "auto_reply_contacts_picker", preSelected);
            startActivityForResult(intent, ContactPickerPreference.REQUEST_CONTACT_PICKER);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to launch contact picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ContactPickerPreference.REQUEST_CONTACT_PICKER && resultCode == RESULT_OK && data != null) {
            ArrayList<ContactPickerResult> results = (ArrayList<ContactPickerResult>) data.getSerializableExtra("picker_contacts");
            if (results != null && !results.isEmpty()) {
                selectedJidsCsv = results.stream().map(ContactPickerResult::jid).collect(Collectors.joining(","));
                updateContactsFieldText();
            }
        }
    }

    private void updateContactsFieldText() {
        if (selectedJidsCsv == null || selectedJidsCsv.isEmpty()) {
            binding.editTargetContacts.setText("No contacts selected");
            return;
        }

        String[] jids = selectedJidsCsv.split(",");
        ArrayList<String> names = new ArrayList<>();
        for (String jid : jids) {
            String name = ContactHelper.getContactName(this, jid.trim());
            if (name == null || name.isEmpty()) {
                name = jid.split("@")[0];
            }
            names.add(name);
        }
        binding.editTargetContacts.setText(String.join(", ", names));
    }

    private void saveAutoReplyRule() {
        String keywords = binding.editKeywords.getText().toString().trim();
        if (keywords.isEmpty()) {
            Toast.makeText(this, "Trigger keywords are required", Toast.LENGTH_SHORT).show();
            return;
        }

        String replyText = binding.editReplyText.getText().toString().trim();
        if (replyText.isEmpty()) {
            Toast.makeText(this, "Reply text is required", Toast.LENGTH_SHORT).show();
            return;
        }

        String matchingType = binding.radioRegex.isChecked() ? "REGEX" : "WILDCARD";
        String targetType = binding.spinnerTargetType.getSelectedItem().toString();

        if ("SPECIFIC_CONTACTS".equals(targetType) && (selectedJidsCsv == null || selectedJidsCsv.isEmpty())) {
            Toast.makeText(this, "Please select at least one contact", Toast.LENGTH_SHORT).show();
            return;
        }

        String start = null;
        String end = null;
        if (binding.switchActiveHours.isChecked()) {
            start = activeHoursStart;
            end = activeHoursEnd;
        }

        final boolean ruleEnabled = binding.switchRuleEnabled.isChecked();
        final String finalKeywords = keywords;
        final String finalReplyText = replyText;
        final String finalStart = start;
        final String finalEnd = end;
        final boolean ignoreCase = binding.checkboxIgnoreCase.isChecked();

        dbExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            AutoReplyRule rule;

            if (ruleToEdit != null) {
                rule = new AutoReplyRule(
                    ruleToEdit.getId(),
                    finalKeywords,
                    matchingType,
                    finalReplyText,
                    binding.switchQuoteOriginal.isChecked(),
                    binding.seekBarDelay.getProgress(),
                    targetType,
                    finalStart,
                    finalEnd,
                    ruleEnabled,
                    binding.switchIsForward.isChecked(),
                    ruleToEdit.getForwardJid(),
                    binding.switchIsAi.isChecked(),
                    ignoreCase,
                    selectedJidsCsv
                );
                db.autoReplyRuleDao().update(rule);
                runOnUiThread(() -> Toast.makeText(this, "Rule updated", Toast.LENGTH_SHORT).show());
            } else {
                rule = new AutoReplyRule(
                    0,
                    finalKeywords,
                    matchingType,
                    finalReplyText,
                    binding.switchQuoteOriginal.isChecked(),
                    binding.seekBarDelay.getProgress(),
                    targetType,
                    finalStart,
                    finalEnd,
                    ruleEnabled,
                    binding.switchIsForward.isChecked(),
                    null,
                    binding.switchIsAi.isChecked(),
                    ignoreCase,
                    selectedJidsCsv
                );
                db.autoReplyRuleDao().insert(rule);
                runOnUiThread(() -> Toast.makeText(this, "Rule created", Toast.LENGTH_SHORT).show());
            }

            AutoReplyActivity.syncRulesToSharedPreferences(this);
            runOnUiThread(this::finish);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}
