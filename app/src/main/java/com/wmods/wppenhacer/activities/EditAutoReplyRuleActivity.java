package com.wmods.wppenhacer.activities;

import android.app.TimePickerDialog;
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

import java.util.Calendar;
import java.util.Locale;

public class EditAutoReplyRuleActivity extends BaseActivity {

    private ActivityEditAutoReplyRuleBinding binding;
    private AutoReplyRule ruleToEdit = null;
    private int ruleId = -1;

    private String activeHoursStart = "09:00";
    private String activeHoursEnd = "17:00";
    private int delaySeconds = 0;

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
        AppDatabase db = AppDatabase.getInstance(this);
        ruleToEdit = db.autoReplyRuleDao().getById(ruleId);
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

        String matchingType = binding.spinnerMatchingType.getSelectedItem().toString();
        String targetType = binding.spinnerTargetType.getSelectedItem().toString();

        String start = null;
        String end = null;
        if (binding.switchActiveHours.isChecked()) {
            start = activeHoursStart;
            end = activeHoursEnd;
        }

        boolean quoteOriginal = binding.switchQuoteOriginal.isChecked();

        AppDatabase db = AppDatabase.getInstance(this);
        AutoReplyRule rule;

        if (ruleToEdit != null) {
            rule = new AutoReplyRule(
                ruleToEdit.getId(),
                keywords,
                matchingType,
                replyText,
                quoteOriginal,
                delaySeconds,
                targetType,
                start,
                end,
                ruleToEdit.isEnabled()
            );
            db.autoReplyRuleDao().update(rule);
            Toast.makeText(this, "Rule updated", Toast.LENGTH_SHORT).show();
        } else {
            rule = new AutoReplyRule(
                0,
                keywords,
                matchingType,
                replyText,
                quoteOriginal,
                delaySeconds,
                targetType,
                start,
                end,
                true
            );
            db.autoReplyRuleDao().insert(rule);
            Toast.makeText(this, "Rule created", Toast.LENGTH_SHORT).show();
        }

        // Sync to SharedPreferences for Xposed hook remote access
        AutoReplyActivity.syncRulesToSharedPreferences(this);
        finish();
    }
}
