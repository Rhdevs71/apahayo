package com.rhdevs.rhpatch.activities;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;

import android.content.Context;
import android.content.SharedPreferences;
import com.rhdevs.rhpatch.R;
import com.rhdevs.rhpatch.activities.base.BaseActivity;
import com.rhdevs.rhpatch.database.AppDatabase;
import com.rhdevs.rhpatch.database.AutoReplyRule;
import com.rhdevs.rhpatch.databinding.ActivityEditAutoReplyRuleBinding;
import com.rhdevs.rhpatch.model.ContactPickerResult;
import com.rhdevs.rhpatch.preference.ContactPickerPreference;
import com.rhdevs.rhpatch.utils.ContactHelper;
import com.rhdevs.rhpatch.utils.WhatsAppContactPickerLauncher;

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

        binding.btnInfoMatchingType.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                .setTitle("Penjelasan Matching Type")
                .setMessage("EXACT: Teks persis sama persis (Contoh: 'halo' -> harus 'halo').\n\n" +
                            "CONTAINS: Teks mengandung kata (Contoh: 'alo' -> cocok dengan 'halo').\n\n" +
                            "STARTS_WITH: Teks diawali kata kunci (Contoh: 'hal' -> cocok dengan 'halo bos').\n\n" +
                            "ENDS_WITH: Teks diakhiri kata kunci (Contoh: 'bos' -> cocok dengan 'halo bos').\n\n" +
                            "WILDCARD: Menggunakan tanda * sebagai kata ganti (Contoh: 'ha*lo').\n\n" +
                            "REGEX: Menggunakan Regular Expression tingkat lanjut.")
                .setPositiveButton("OK", null)
                .show();
        });

        // Setup Matching Type Spinner
        String[] matchingTypes = {"EXACT", "CONTAINS", "STARTS_WITH", "ENDS_WITH", "WILDCARD", "REGEX"};
        ArrayAdapter<String> matchingAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, matchingTypes);
        matchingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerMatchingType.setAdapter(matchingAdapter);

        // Setup Reply Type Spinner
        String[] replyTypes = {"TEXT", "RANDOM", "MULTIPLE", "AI"};
        ArrayAdapter<String> replyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, replyTypes);
        replyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerReplyType.setAdapter(replyAdapter);

        // Setup AI Providers
        String[] aiProviders = {"groq", "openai", "gemini"};
        ArrayAdapter<String> providerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, aiProviders);
        providerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerAiProvider.setAdapter(providerAdapter);

        // Setup AI Models
        String[] aiModels = {
            "llama3-8b-8192", 
            "gpt-4o", 
            "gemini-1.5-pro",
            "gemini-2.5-flash",
            "gemini-3-flash-preview",
            "gemini-3.1-flash-lite-preview",
            "gemini-2.5-flash-lite",
            "Custom..."
        };
        ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, aiModels);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerAiModel.setAdapter(modelAdapter);

        binding.spinnerAiModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if ("Custom...".equals(aiModels[position])) {
                    binding.editCustomModel.setVisibility(View.VISIBLE);
                } else {
                    binding.editCustomModel.setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.spinnerReplyType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if ("AI".equals(replyTypes[position])) {
                    binding.layoutAiSettings.setVisibility(View.VISIBLE);
                    binding.labelReplyText.setText("AI Prompt Instruction");
                    binding.editReplyText.setHint("You are a helpful assistant...");
                } else if ("MULTIPLE".equals(replyTypes[position]) || "RANDOM".equals(replyTypes[position])) {
                    binding.layoutAiSettings.setVisibility(View.GONE);
                    binding.labelReplyText.setText("Reply text (Use ||| to separate replies)");
                    binding.editReplyText.setHint("Option A|||Option B");
                } else {
                    binding.layoutAiSettings.setVisibility(View.GONE);
                    binding.labelReplyText.setText("Reply text");
                    binding.editReplyText.setHint("Automatic response content");
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
        
        String savedProvider = prefs.getString("ai_provider", "groq");
        for (int i = 0; i < aiProviders.length; i++) {
            if (aiProviders[i].equals(savedProvider)) {
                binding.spinnerAiProvider.setSelection(i);
                break;
            }
        }
        
        String savedModel = prefs.getString("ai_model", "llama3-8b-8192");
        boolean modelFound = false;
        for (int i = 0; i < aiModels.length; i++) {
            if (aiModels[i].equals(savedModel)) {
                binding.spinnerAiModel.setSelection(i);
                modelFound = true;
                break;
            }
        }
        if (!modelFound) {
            binding.spinnerAiModel.setSelection(aiModels.length - 1); // "Custom..."
            binding.editCustomModel.setText(savedModel);
            binding.editCustomModel.setVisibility(View.VISIBLE);
        }

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

                // Set matching type spinner
                int matchIndex = 0;
                switch(ruleToEdit.getMatchingType()) {
                    case "CONTAINS": matchIndex = 1; break;
                    case "STARTS_WITH": matchIndex = 2; break;
                    case "ENDS_WITH": matchIndex = 3; break;
                    case "WILDCARD": matchIndex = 4; break;
                    case "REGEX": matchIndex = 5; break;
                }
                binding.spinnerMatchingType.setSelection(matchIndex);

                binding.checkboxIgnoreCase.setChecked(ruleToEdit.getIgnoreCase());
                binding.editReplyText.setText(ruleToEdit.getReplyText());

                int targetSelection = 0;
                switch (ruleToEdit.getTargetType()) {
                    case "CONTACTS": targetSelection = 1; break;
                    case "GROUPS": targetSelection = 2; break;
                    case "NON_CONTACTS": targetSelection = 3; break;
                    case "SPECIFIC_CONTACTS": targetSelection = 4; break;
                }
                binding.spinnerTargetType.setSelection(targetSelection);
                
                int replySelection = 0;
                String rType = ruleToEdit.getReplyType() != null ? ruleToEdit.getReplyType() : (ruleToEdit.isAi() ? "AI" : "TEXT");
                switch (rType) {
                    case "RANDOM": replySelection = 1; break;
                    case "MULTIPLE": replySelection = 2; break;
                    case "AI": replySelection = 3; break;
                }
                binding.spinnerReplyType.setSelection(replySelection);

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
            ArrayList<String> jids = data.getStringArrayListExtra("contacts");
            if (jids != null && !jids.isEmpty()) {
                selectedJidsCsv = String.join(",", jids);
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
            Toast.makeText(this, "Reply text/prompt is required", Toast.LENGTH_SHORT).show();
            return;
        }

        String matchingType = binding.spinnerMatchingType.getSelectedItem().toString();
        String targetType = binding.spinnerTargetType.getSelectedItem().toString();
        String replyType = binding.spinnerReplyType.getSelectedItem().toString();
        String aiProvider = binding.spinnerAiProvider.getSelectedItem().toString();
        
        String aiModelRaw = binding.spinnerAiModel.getSelectedItem().toString();
        String aiModel = "Custom...".equals(aiModelRaw) ? binding.editCustomModel.getText().toString().trim() : aiModelRaw;
        
        if ("AI".equals(replyType)) {
            SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit()
                 .putString("ai_provider", aiProvider)
                 .putString("ai_model", aiModel)
                 .apply();
        }

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
                    "AI".equals(replyType),
                    ignoreCase,
                    selectedJidsCsv,
                    replyType,
                    "AI".equals(replyType) ? aiProvider : null,
                    "AI".equals(replyType) ? replyText : null,
                    ruleToEdit.getAttachmentUri()
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
                    "AI".equals(replyType),
                    ignoreCase,
                    selectedJidsCsv,
                    replyType,
                    "AI".equals(replyType) ? aiProvider : null,
                    "AI".equals(replyType) ? replyText : null,
                    null
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
