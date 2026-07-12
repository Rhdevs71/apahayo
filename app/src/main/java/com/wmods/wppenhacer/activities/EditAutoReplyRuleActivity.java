package com.wmods.wppenhacer.activities;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.Nullable;

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

public class EditAutoReplyRuleActivity extends BaseActivity {

    private ActivityEditAutoReplyRuleBinding binding;
    private AutoReplyRule ruleToEdit = null;
    private int ruleId = -1;

    private String activeHoursStart = "09:00";
    private String activeHoursEnd = "17:00";
    private int delaySeconds = 0;
    private String selectedForwardJid = null;

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditAutoReplyRuleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Setup Spinners
        String[] matchingTypes = {"EXACT", "CONTAINS", "REGEX"};
        ArrayAdapter<String> matchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, matchingTypes);
        matchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerMatchingType.setAdapter(matchAdapter);

        String[] targetTypes = {"ALL", "CONTACTS", "GROUPS", "NON_CONTACTS"};
        ArrayAdapter<String> targetAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, targetTypes);
        targetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerTargetType.setAdapter(targetAdapter);

        // Setup Time Windows Switch
        binding.switchActiveHours.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.layoutActiveHours.setVisibility(View.VISIBLE);
            } else {
                binding.layoutActiveHours.setVisibility(View.GONE);
            }
        });

        binding.btnActiveStart.setOnClickListener(v -> showTimePicker(true));
        binding.btnActiveEnd.setOnClickListener(v -> showTimePicker(false));

        // Setup Forward Switch
        binding.switchIsForward.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.layoutForwardJid.setVisibility(View.VISIBLE);
                binding.switchIsAi.setChecked(false); // Mutual exclusion
            } else {
                binding.layoutForwardJid.setVisibility(View.GONE);
            }
        });

        binding.switchIsAi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.switchIsForward.setChecked(false); // Mutual exclusion
            }
        });

        binding.btnSelectForwardContact.setOnClickListener(v -> startWhatsAppContactPicker());

        // Setup Delay SeekBar
        binding.seekBarDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                delaySeconds = progress;
                binding.textDelayValue.setText(progress + "s");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

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

                binding.editKeywords.setText(ruleToEdit.getKeywords());

                int matchSelection = 0;
                switch (ruleToEdit.getMatchingType()) {
                    case "CONTAINS" -> matchSelection = 1;
                    case "REGEX" -> matchSelection = 2;
                }
                binding.spinnerMatchingType.setSelection(matchSelection);

                binding.editReplyText.setText(ruleToEdit.getReplyText());

                int targetSelection = 0;
                switch (ruleToEdit.getTargetType()) {
                    case "CONTACTS" -> targetSelection = 1;
                    case "GROUPS" -> targetSelection = 2;
                    case "NON_CONTACTS" -> targetSelection = 3;
                }
                binding.spinnerTargetType.setSelection(targetSelection);

                boolean hasActiveHours = ruleToEdit.getActiveHoursStart() != null && ruleToEdit.getActiveHoursEnd() != null;
                binding.switchActiveHours.setChecked(hasActiveHours);
                if (hasActiveHours) {
                    binding.layoutActiveHours.setVisibility(View.VISIBLE);
                    activeHoursStart = ruleToEdit.getActiveHoursStart();
                    activeHoursEnd = ruleToEdit.getActiveHoursEnd();
                }
                updateTimeButtons();

                binding.switchQuoteOriginal.setChecked(ruleToEdit.getQuoteOriginal());

                delaySeconds = ruleToEdit.getDelaySeconds();
                binding.seekBarDelay.setProgress(delaySeconds);
                binding.textDelayValue.setText(delaySeconds + "s");

                binding.switchIsAi.setChecked(ruleToEdit.isAi());
                binding.switchIsForward.setChecked(ruleToEdit.isForward());
                selectedForwardJid = ruleToEdit.getForwardJid();
                if (selectedForwardJid != null && !selectedForwardJid.isEmpty()) {
                    String name = ContactHelper.getContactName(this, selectedForwardJid);
                    if (name == null || name.isEmpty()) {
                        name = selectedForwardJid.split("@")[0];
                    }
                    binding.textForwardJid.setText("Forward Recipient JID: " + name + " (" + selectedForwardJid + ")");
                    binding.layoutForwardJid.setVisibility(View.VISIBLE);
                }
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
            if (selectedForwardJid != null) {
                preSelected.add(selectedForwardJid);
            }
            Intent intent = WhatsAppContactPickerLauncher.createPickerIntent(this, targetPackage, "auto_reply_forward_picker", preSelected);
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
                ContactPickerResult result = results.get(0);
                selectedForwardJid = result.jid();
                String name = result.fullName();
                if (name == null || name.isEmpty()) {
                    name = ContactHelper.getContactName(this, selectedForwardJid);
                }
                if (name == null || name.isEmpty()) {
                    name = selectedForwardJid.split("@")[0];
                }
                binding.textForwardJid.setText("Forward Recipient JID: " + name + " (" + selectedForwardJid + ")");
            }
        }
    }

    private void saveAutoReplyRule() {
        String keywords = binding.editKeywords.getText().toString().trim();
        if (keywords.isEmpty()) {
            Toast.makeText(this, "Trigger keywords are required", Toast.LENGTH_SHORT).show();
            return;
        }

        String replyText = binding.editReplyText.getText().toString().trim();
        boolean isForward = binding.switchIsForward.isChecked();
        boolean isAi = binding.switchIsAi.isChecked();

        if (!isForward && !isAi && replyText.isEmpty()) {
            Toast.makeText(this, "Reply text is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isForward && selectedForwardJid == null) {
            Toast.makeText(this, "Please select a forward JID", Toast.LENGTH_SHORT).show();
            return;
        }

        String matchingType = binding.spinnerMatchingType.getSelectedItem().toString();
        String targetType = binding.spinnerTargetType.getSelectedItem().toString();

        String start = null;
        String end = null;
        if (binding.switchActiveHours.isChecked()) {
            start = activeHoursStart;
            end = activeHoursEnd;
        }

        boolean quoteOriginal = binding.switchQuoteOriginal.isChecked();

        final String finalKeywords = keywords;
        final String finalReplyText = replyText;
        final String finalStart = start;
        final String finalEnd = end;
        final String finalForwardJid = selectedForwardJid;

        dbExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            AutoReplyRule rule;

            if (ruleToEdit != null) {
                rule = new AutoReplyRule(
                    ruleToEdit.getId(),
                    finalKeywords,
                    matchingType,
                    finalReplyText,
                    quoteOriginal,
                    delaySeconds,
                    targetType,
                    finalStart,
                    finalEnd,
                    ruleToEdit.isEnabled(),
                    isForward,
                    finalForwardJid,
                    isAi
                );
                db.autoReplyRuleDao().update(rule);
                runOnUiThread(() -> Toast.makeText(this, "Rule updated", Toast.LENGTH_SHORT).show());
            } else {
                rule = new AutoReplyRule(
                    0,
                    finalKeywords,
                    matchingType,
                    finalReplyText,
                    quoteOriginal,
                    delaySeconds,
                    targetType,
                    finalStart,
                    finalEnd,
                    true,
                    isForward,
                    finalForwardJid,
                    isAi
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
