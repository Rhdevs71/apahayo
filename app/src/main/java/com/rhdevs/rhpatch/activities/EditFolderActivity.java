package com.rhdevs.rhpatch.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.rhdevs.rhpatch.activities.base.BaseActivity;
import com.rhdevs.rhpatch.databinding.ActivityEditFolderBinding;
import com.rhdevs.rhpatch.model.ContactPickerResult;
import com.rhdevs.rhpatch.preference.ContactPickerPreference;
import com.rhdevs.rhpatch.utils.WhatsAppContactPickerLauncher;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EditFolderActivity extends BaseActivity {

    private ActivityEditFolderBinding binding;
    private int folderIndex = -1;
    private List<JSONObject> foldersList = new ArrayList<>();
    private ArrayList<String> selectedJids = new ArrayList<>();

    private final String[] colorEntries = {"Green", "Blue", "Cyan", "Purple", "Orange", "Red", "Pink"};
    private final String[] colorValues = {"#ff4faf50", "#ff3b82f6", "#ff06b6d4", "#ff8b5cf6", "#fff97316", "#ffef4444", "#ffec4899"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditFolderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Setup Color Spinner
        ArrayAdapter<String> colorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, colorEntries);
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFolderColor.setAdapter(colorAdapter);

        // Load existing folders
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String foldersJson = prefs.getString("custom_folders", "[]");
        try {
            JSONArray array = new JSONArray(foldersJson);
            for (int i = 0; i < array.length(); i++) {
                foldersList.add(array.getJSONObject(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        folderIndex = getIntent().getIntExtra("folder_index", -1);
        if (folderIndex != -1) {
            loadExistingFolder();
        }

        binding.btnSelectChats.setOnClickListener(v -> startWhatsAppContactPicker());
        binding.btnSave.setOnClickListener(v -> saveFolder());
    }

    private void loadExistingFolder() {
        JSONObject folder = foldersList.get(folderIndex);
        binding.editFolderName.setText(folder.optString("name", ""));

        String color = folder.optString("color", "#ff4faf50");
        int spinnerPos = 0;
        for (int i = 0; i < colorValues.length; i++) {
            if (colorValues[i].equals(color)) {
                spinnerPos = i;
                break;
            }
        }
        binding.spinnerFolderColor.setSelection(spinnerPos);

        String contacts = folder.optString("contacts", "");
        if (!contacts.isEmpty()) {
            selectedJids = new ArrayList<>(Arrays.asList(contacts.split(",")));
            binding.textSelectedChats.setText(selectedJids.size() + " chats selected");
        }
    }

    private void startWhatsAppContactPicker() {
        var installedPackages = WhatsAppContactPickerLauncher.getInstalledWhatsAppPackages(this);
        if (installedPackages.isEmpty()) {
            Toast.makeText(this, "WhatsApp is not installed", Toast.LENGTH_SHORT).show();
            return;
        }

        String targetPackage = installedPackages.get(0);
        try {
            Intent intent = WhatsAppContactPickerLauncher.createPickerIntent(this, targetPackage, "custom_folders_picker", selectedJids);
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
            if (results != null) {
                selectedJids = results.stream().map(ContactPickerResult::jid).collect(Collectors.toCollection(ArrayList::new));
                binding.textSelectedChats.setText(selectedJids.size() + " chats selected");
            }
        }
    }

    private void saveFolder() {
        String name = binding.editFolderName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Folder name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedJids.isEmpty()) {
            Toast.makeText(this, "Please select at least one chat", Toast.LENGTH_SHORT).show();
            return;
        }

        String color = colorValues[binding.spinnerFolderColor.getSelectedItemPosition()];
        String contactsCsv = String.join(",", selectedJids);

        try {
            JSONObject folder = new JSONObject();
            folder.put("name", name);
            folder.put("color", color);
            folder.put("contacts", contactsCsv);

            if (folderIndex != -1) {
                foldersList.set(folderIndex, folder);
            } else {
                foldersList.add(folder);
            }

            JSONArray array = new JSONArray();
            for (JSONObject f : foldersList) {
                array.put(f);
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit().putString("custom_folders", array.toString()).apply();

            Toast.makeText(this, "Folder saved", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save folder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
