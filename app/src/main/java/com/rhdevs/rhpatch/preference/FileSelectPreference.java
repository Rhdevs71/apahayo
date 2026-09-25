package com.rhdevs.rhpatch.preference;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.rhdevs.rhpatch.App;
import com.rhdevs.rhpatch.R;
import com.rhdevs.rhpatch.utils.FilePicker;
import com.rhdevs.rhpatch.xposed.utils.Utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class FileSelectPreference extends Preference implements Preference.OnPreferenceClickListener, FilePicker.OnFilePickedListener, FilePicker.OnUriPickedListener {

    private String[] mineTypes;
    private boolean selectDirectory;

    public FileSelectPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public FileSelectPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public FileSelectPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void showAlertPermission() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        builder.setTitle(R.string.storage_permission);
        builder.setMessage(R.string.permission_storage);
        builder.setPositiveButton(R.string.allow, (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.fromParts("package", getContext().getPackageName(), null));
            getContext().startActivity(intent);
        });
        builder.setNegativeButton(R.string.deny, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            showAlertPermission();
            return true;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ((Activity) getContext()).requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 1);
                return true;
            }
        } else if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ((Activity) getContext()).requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            return true;
        }

        FilePicker.setOnFilePickedListener(this);
        if (selectDirectory) {
            showSelectDirectoryDialog();
            return true;
        }

        if (mineTypes.length == 1 && mineTypes[0].contains("image")) {
            FilePicker.setOnUriPickedListener(this);
            FilePicker.imageCapture.launch(new PickVisualMediaRequest.Builder().setMediaType(new ActivityResultContracts.PickVisualMedia.SingleMimeType(mineTypes[0])).build());
            return true;
        }
        FilePicker.fileCapture.launch(mineTypes);
        return false;
    }

    private void showSelectDirectoryDialog() {
        String currentPath = getSharedPreferences().getString(getKey(), Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());

        String[] options = new String[]{
                "Pilih / Buat Folder Baru (File Manager Sistem)",
                "Ketik Path Folder Manual (Bisa Buat Baru)",
                "Gunakan Folder Downloads Default",
                "Penjelajah Berkas Internal"
        };

        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Pilih Lokasi Penyimpanan")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // Buka SAF resmi Android yang mendukung tombol Buat Folder Baru
                            FilePicker.setOnFilePickedListener(this);
                            try {
                                FilePicker.directoryCapture.launch(null);
                            } catch (Exception e) {
                                openLegacyFilePicker();
                            }
                            break;
                        case 1:
                            // Dialog ketik manual
                            showManualPathDialog(currentPath);
                            break;
                        case 2:
                            // Reset ke Downloads default
                            File defaultDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                            if (!defaultDir.exists()) defaultDir.mkdirs();
                            getSharedPreferences().edit().putString(getKey(), defaultDir.getAbsolutePath()).apply();
                            setSummary(defaultDir.getAbsolutePath());
                            Toast.makeText(getContext(), "Lokasi diatur ke: " + defaultDir.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                            break;
                        case 3:
                            // Buka legacy picker
                            openLegacyFilePicker();
                            break;
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showManualPathDialog(String currentPath) {
        final EditText input = new EditText(getContext());
        input.setText(currentPath);
        input.setSelection(input.getText().length());

        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Ketik Lokasi Folder")
                .setMessage("Masukkan path folder penyimpanan. Folder akan dibuat otomatis jika belum ada:")
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String path = input.getText().toString().trim();
                    if (!path.isEmpty()) {
                        File dir = new File(path);
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }
                        getSharedPreferences().edit().putString(getKey(), dir.getAbsolutePath()).apply();
                        setSummary(dir.getAbsolutePath());
                        Toast.makeText(getContext(), "Lokasi disimpan: " + dir.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void openLegacyFilePicker() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.DIR_SELECT;
        properties.root = Environment.getExternalStorageDirectory();
        properties.error_dir = Environment.getExternalStorageDirectory();
        properties.offset = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        FilePickerDialog dialog = new FilePickerDialog(getContext(), properties);
        dialog.setTitle("Pilih Folder");
        dialog.setDialogSelectionListener((selectionPaths) -> {
            if (selectionPaths != null && selectionPaths.length > 0) {
                getSharedPreferences().edit().putString(getKey(), selectionPaths[0]).apply();
                setSummary(selectionPaths[0]);
            }
        });
        dialog.show();
    }

    @Override
    public void onFilePicked(File file) {
        if (file.isDirectory()) {
            try {
                var tmpFile = Files.write(new File(file, "tmp.file").toPath(), new byte[0]).toFile();
                boolean delete = tmpFile.delete();
            } catch (Exception ignored) {
                Toast.makeText(this.getContext(), R.string.failed_save_directory, Toast.LENGTH_SHORT).show();
                return;
            }
        } else if (!file.canRead()) {
            Toast.makeText(this.getContext(), R.string.unable_to_read_this_file, Toast.LENGTH_SHORT).show();
            return;

        }
        getSharedPreferences().edit().putString(getKey(), file.getAbsolutePath()).apply();
        setSummary(file.getAbsolutePath());
    }

    public void init(Context context, AttributeSet attrs) {
        setOnPreferenceClickListener(this);
        var typedArray = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.FileSelectPreference,
                0, 0
        );
        var attrsArray = typedArray.getTextArray(R.styleable.FileSelectPreference_android_entryValues);
        if (attrsArray != null) {
            mineTypes = Arrays.stream(attrsArray).map(String::valueOf).toArray(String[]::new);
        } else {
            mineTypes = new String[]{"*/*"};
        }
        selectDirectory = typedArray.getBoolean(R.styleable.FileSelectPreference_directory, false);
        var prefs = PreferenceManager.getDefaultSharedPreferences(context);
        var keyValue = prefs.getString(this.getKey(), null);
        setSummary(keyValue);
    }

    @Override
    public void onUriPicked(Uri uri) {
        ContentResolver contentResolver = getContext().getContentResolver();
        var type = Objects.requireNonNull(contentResolver.getType(uri));
        var extension = type.split("/")[1];
        var folder = new File(App.getRhpatchFolder(), "files");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        var outFile = new File(folder, this.getKey() + "." + extension);
        var editor = getSharedPreferences().edit();
        editor.putString(getKey(), "").commit();
        setSummary(outFile.getAbsolutePath());
        CompletableFuture.runAsync(() -> {
            try (var inputStream = contentResolver.openInputStream(uri)) {
                Files.copy(inputStream, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                Utils.showToast("Failed to save file: " + e, Toast.LENGTH_SHORT);
            }
            editor.putString(getKey(), outFile.getAbsolutePath()).commit();
        });

    }

}
