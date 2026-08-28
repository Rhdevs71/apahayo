/*
 * Copyright 2026 Morphe.
 * https://github.com/Morphecom/rhdevs/rhpatch/youtube-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package com.rhdevs.rhpatch.youtube.extension.shared.settings.preference;

import static com.rhdevs.rhpatch.youtube.extension.shared.StringRef.str;
import static com.rhdevs.rhpatch.youtube.extension.shared.settings.preference.ExportLogToClipboardPreference.saveLogsToUri;

import static com.rhdevs.rhpatch.youtube.shared.settings.PreferencesKt.getPreferences;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.preference.TwoStatePreference;
import android.text.InputType;
import android.util.Pair;
import android.util.TypedValue;
import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;

import com.rhdevs.rhpatch.youtube.extension.shared.Logger;
import com.rhdevs.rhpatch.youtube.extension.shared.ResourceType;
import com.rhdevs.rhpatch.youtube.extension.shared.ResourceUtils;
import com.rhdevs.rhpatch.youtube.extension.shared.Utils;
import com.rhdevs.rhpatch.youtube.extension.shared.settings.BaseSettings;
import com.rhdevs.rhpatch.youtube.extension.shared.settings.BooleanSetting;
import com.rhdevs.rhpatch.youtube.extension.shared.settings.Setting;
import com.rhdevs.rhpatch.youtube.extension.shared.settings.SharedSettings;
import com.rhdevs.rhpatch.youtube.extension.shared.settings.preference.about.MorpheAboutPreference;
import com.rhdevs.rhpatch.youtube.extension.shared.ui.CustomDialog;
import com.rhdevs.rhpatch.youtube.extension.shared.ui.Dim;

import com.rhdevs.rhpatch.youtube.shared.misc.settings.preference.BasePreference;

@SuppressWarnings("deprecation")
public abstract class AbstractPreferenceFragment extends PreferenceFragment {
    private static void performToggleHaptic(View view, TwoStatePreference pref) {
        int feedbackConstant = Build.VERSION.SDK_INT >= 34
                ? (pref.isChecked() ? HapticFeedbackConstants.TOGGLE_OFF : HapticFeedbackConstants.TOGGLE_ON)
                : HapticFeedbackConstants.CLOCK_TICK;
        view.performHapticFeedback(feedbackConstant);
    }

    private static class ClickDebouncer {
        private static final long FAST_CLICK_MS = 300;
        private int lastPosition = AdapterView.INVALID_POSITION;
        private long lastTimeMs;

        boolean isFastRepeat(int position) {
            final long now = SystemClock.elapsedRealtime();
            if (position == lastPosition && now - lastTimeMs < FAST_CLICK_MS) {
                return true;
            }
            lastPosition = position;
            lastTimeMs = now;
            return false;
        }
    }

    private static class DebouncedListView extends ListView {
        private final ClickDebouncer debouncer = new ClickDebouncer();

        public DebouncedListView(Context context) {
            super(context);

            setId(android.R.id.list); // Required so PreferenceFragment recognizes it.

            // Match the default layout params
            setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
        }

        @Override
        public boolean performItemClick(View view, int position, long id) {
            Object item = getAdapter().getItem(position);

            if (item instanceof TwoStatePreference twoState) {
                performToggleHaptic(view, twoState);
                return super.performItemClick(view, position, id);
            }

            // Only debounce repeated taps on the same item to prevent accidental double-clicks.
            if (debouncer.isFastRepeat(position)) return true;
            return super.performItemClick(view, position, id);
        }
    }

    private static class DebouncedItemClickListener implements AdapterView.OnItemClickListener {
        private final AdapterView.OnItemClickListener originalListener;
        private final ClickDebouncer debouncer = new ClickDebouncer();

        DebouncedItemClickListener(AdapterView.OnItemClickListener originalListener) {
            this.originalListener = originalListener;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Object item = parent.getAdapter().getItem(position);

            if (item instanceof TwoStatePreference twoState) {
                performToggleHaptic(view, twoState);
                originalListener.onItemClick(parent, view, position, id);
                return;
            }

            // Only debounce repeated taps on the same item to prevent accidental double-clicks.
            if (debouncer.isFastRepeat(position)) return;
            originalListener.onItemClick(parent, view, position, id);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference instanceof PreferenceScreen) {
            Dialog dialog = ((PreferenceScreen) preference).getDialog();
            if (dialog != null) {
                ListView listView = dialog.findViewById(android.R.id.list);
                if (listView != null) {
                    AdapterView.OnItemClickListener originalListener = listView.getOnItemClickListener();
                    if (originalListener != null && !(originalListener instanceof DebouncedItemClickListener)) {
                        listView.setOnItemClickListener(new DebouncedItemClickListener(originalListener));
                    }
                }
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    // Cached Collator instance with its locale.
    @Nullable
    private static Locale cachedCollatorLocale;

    @Nullable
    private static Collator cachedCollator;

    public static WeakReference<AbstractPreferenceFragment> instance = new WeakReference<>(null);

    /**
     * Indicates that if a preference changes,
     * to apply the change from the Setting to the UI component.
     */
    public static boolean settingImportInProgress;

    /**
     * Prevents recursive calls during preference <-> UI syncing from showing extra dialogs.
     */
    private static boolean updatingPreference;

    /**
     * Used to prevent showing reboot dialog, if user cancels a setting user dialog.
     */
    private static boolean showingUserDialogMessage;

    /**
     * Confirm and restart dialog button text and title.
     * Set by subclasses if Strings cannot be added as a resource.
     */
    @Nullable
    protected static CharSequence restartDialogTitle, restartDialogMessage, restartDialogButtonText, confirmDialogTitle;

    private ComponentCallbacks2 configurationListener;
    private int currentUiMode = -1;
    private static final int READ_REQUEST_CODE = 42;
    private static final int WRITE_REQUEST_CODE = 43;
    private String existingSettings = "";

    private EditText currentImportExportEditText;

    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, str) -> {
        try {
            if (updatingPreference) {
                Logger.printDebug(() -> "Ignoring preference change as sync is in progress");
                return;
            }

            Setting<?> setting = Setting.getSettingFromPath(Objects.requireNonNull(str));
            if (setting == null) {
                return;
            }
            Preference pref = findPreference(str);
            if (pref == null) {
                return;
            }
            Logger.printDebug(() -> "Preference changed: " + setting.key);

            if (!settingImportInProgress && !showingUserDialogMessage) {
                if (setting.userDialogMessage != null && !prefIsSetToDefault(pref, setting)) {
                    // Do not change the setting yet, to allow preserving whatever
                    // list/text value was previously set if it needs to be reverted.
                    showSettingUserDialogConfirmation(pref, setting);
                    return;
                } else if (setting.rebootApp) {
                    showRestartDialog(getContext());
                }
            }

            updatingPreference = true;
            // Apply 'Setting <- Preference', unless during importing when it needs to be 'Setting -> Preference'.
            // Updating here can cause a recursive call back into this same method.
            updatePreference(pref, setting, true, settingImportInProgress);
            // Update any other preference availability that may now be different.
            updateUIAvailability();
            updatingPreference = false;
        } catch (Exception ex) {
            Logger.printException(() -> "OnSharedPreferenceChangeListener failure", ex);
        }
    };

    /**
     * Initialize this instance, and do any custom behavior.
     * <p>
     * To ensure all {@link Setting} instances are correctly synced to the UI,
     * it is important that subclasses make a call or otherwise reference their Settings class bundle
     * so all app specific {@link Setting} instances are loaded before this method returns.
     */
    protected void initialize() {
        if (!BaseSettings.MORPHE_LANGUAGE.isSetToDefault()) {
            ResourceUtils.useActivityContextIfAvailable = false;
        }

        Activity context = getActivity();
        PreferenceManager manager = getPreferenceManager();
        manager.setSharedPreferencesName(Setting.preferences.name);
        final PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.setKey("morphe_settings_root_screen_sort_by_key");
        setPreferenceScreen(screen);

        var preferencesBuilder = getPreferences();
        if (preferencesBuilder != null) {
            preferencesBuilder.forEach(builder -> {
                try {
                    screen.addPreference(builder.build(context, manager));
                } catch (Exception ignored) {}
            });
            preferencesBuilder.forEach(builder -> {
                try {
                    builder.onAttachedToHierarchy();
                } catch (Exception ignored) {}
            });
        }

        try {
            sortPreferenceGroups(screen);
        } catch (Exception ignored) {}
        try {
            Utils.setPreferenceTitlesToMultiLineIfNeeded(screen);
        } catch (Exception ignored) {}
    }

    private void showSettingUserDialogConfirmation(Preference pref, Setting<?> setting) {
        Utils.verifyOnMainThread();

        final var context = getContext();
        if (confirmDialogTitle == null) {
            confirmDialogTitle = str("morphe_settings_confirm_user_dialog_title");
        }

        showingUserDialogMessage = true;

        CharSequence message = BulletPointPreference.formatIntoBulletPoints(
                Objects.requireNonNull(setting.userDialogMessage).toString());

        Pair<Dialog, LinearLayout> dialogPair = CustomDialog.create(
                context,
                confirmDialogTitle, // Title.
                message,
                null, // No EditText.
                null, // OK button text.
                () -> {
                    // OK button action. User confirmed, save to the Setting.
                    updatePreference(pref, setting, true, false);

                    // Update availability of other preferences that may be changed.
                    updateUIAvailability();

                    if (setting.rebootApp) {
                        showRestartDialog(context);
                    }
                },
                () -> {
                    // Cancel button action. Restore whatever the setting was before the change.
                    updatePreference(pref, setting, true, true);
                },
                null, // No Neutral button.
                null, // No Neutral button action.
                true  // Dismiss dialog when onNeutralClick.
        );

        dialogPair.first.setOnDismissListener(d -> showingUserDialogMessage = false);
        dialogPair.first.setCancelable(false);

        // Show the dialog.
        dialogPair.first.show();
    }

    /**
     * Updates all Preferences values and their availability using the current values in {@link Setting}.
     */
    protected void updateUIToSettingValues() {
        updatePreferenceScreen(getPreferenceScreen(), true, true);
    }

    /**
     * Updates Preferences availability only using the status of {@link Setting}.
     */
    protected void updateUIAvailability() {
        updatePreferenceScreen(getPreferenceScreen(), false, false);
    }

    /**
     * @return If the preference is currently set to the default value of the Setting.
     */
    protected boolean prefIsSetToDefault(Preference pref, Setting<?> setting) {
        Object defaultValue = setting.defaultValue;
        if (pref instanceof SwitchPreference switchPref) {
            return defaultValue instanceof Boolean && switchPref.isChecked() == (Boolean) defaultValue;
        }
        String defaultValueString = defaultValue != null ? defaultValue.toString() : "";
        if (pref instanceof EditTextPreference editPreference) {
            String txt = editPreference.getText();
            return txt != null && txt.equals(defaultValueString);
        }
        if (pref instanceof ListPreference listPref) {
            String val = listPref.getValue();
            return val != null && val.equals(defaultValueString);
        }

        return true;
    }

    /**
     * Syncs all UI Preferences to any {@link Setting} they represent.
     */
    private void updatePreferenceScreen(@NonNull PreferenceGroup group,
                                        boolean syncSettingValue,
                                        boolean applySettingToPreference) {
        // Alternatively this could iterate through all Settings and check for any matching Preferences,
        // but there are many more Settings than UI preferences so it's more efficient to only check
        // the Preferences.
        for (int i = 0, prefCount = group.getPreferenceCount(); i < prefCount; i++) {
            Preference pref = group.getPreference(i);
            if (pref instanceof PreferenceGroup subGroup) {
                updatePreferenceScreen(subGroup, syncSettingValue, applySettingToPreference);
            } else if (pref.hasKey()) {
                String key = pref.getKey();
                Setting<?> setting = Setting.getSettingFromPath(key);

                if (setting != null) {
                    updatePreference(pref, setting, syncSettingValue, applySettingToPreference);
                } else if (BaseSettings.DEBUG.get() && (pref instanceof SwitchPreference
                        || pref instanceof EditTextPreference || pref instanceof ListPreference)) {
                    // Probably a typo in the patches preference declaration.
                    Logger.printException(() -> "Preference key has no setting: " + key);
                }
            }
        }
    }

    /**
     * Handles syncing a UI Preference with the {@link Setting} that backs it.
     * If needed, subclasses can override this to handle additional UI Preference types.
     *
     * @param applySettingToPreference If true, then apply {@link Setting} -> Preference.
     *                                 If false, then apply {@link Setting} <- Preference.
     */
    protected void syncSettingWithPreference(@NonNull Preference pref,
                                             @NonNull Setting<?> setting,
                                             boolean applySettingToPreference) {
        if (pref instanceof SwitchPreference switchPref) {
            // Skip sync when the Setting's backing type does not match SwitchPreference's boolean model.
            // The preference overrides persistBoolean/getPersistedBoolean and maps the boolean
            // to a non-boolean backing on its own.
            if (!(setting instanceof BooleanSetting boolSetting)) {
                return;
            }
            if (applySettingToPreference) {
                switchPref.setChecked(boolSetting.get());
            } else {
                BooleanSetting.privateSetValue(boolSetting, switchPref.isChecked());
            }
        } else if (pref instanceof EditTextPreference editPreference) {
            if (applySettingToPreference) {
                editPreference.setText(setting.get().toString());
            } else {
                Setting.privateSetValueFromString(setting, editPreference.getText());
            }
        } else if (pref instanceof ListPreference listPref) {
            if (applySettingToPreference) {
                listPref.setValue(setting.get().toString());
            } else {
                Setting.privateSetValueFromString(setting, listPref.getValue());
            }
            updateListPreferenceSummary(listPref, setting);
        }
    }

    /**
     * Updates a UI Preference with the {@link Setting} that backs it.
     *
     * @param syncSetting If the UI should be synced {@link Setting} <-> Preference
     * @param applySettingToPreference If true, then apply {@link Setting} -> Preference.
     *                                 If false, then apply {@link Setting} <- Preference.
     */
    private void updatePreference(@NonNull Preference pref, @NonNull Setting<?> setting,
                                  boolean syncSetting, boolean applySettingToPreference) {
        if (!syncSetting && applySettingToPreference) {
            throw new IllegalArgumentException();
        }

        if (syncSetting) {
            syncSettingWithPreference(pref, setting, applySettingToPreference);
        }

        updatePreferenceAvailability(pref, setting);
    }

    protected void updatePreferenceAvailability(@NonNull Preference pref, @NonNull Setting<?> setting) {
        pref.setEnabled(setting.isAvailable());
    }

    protected void updateListPreferenceSummary(ListPreference listPreference, Setting<?> setting) {
        String objectStringValue = setting.get() != null ? setting.get().toString() : "";
        final int entryIndex = listPreference.findIndexOfValue(objectStringValue);
        CharSequence[] entries = listPreference.getEntries();
        if (entryIndex >= 0 && entries != null && entryIndex < entries.length) {
            listPreference.setSummary(entries[entryIndex]);
        } else {
            listPreference.setSummary(objectStringValue);
        }
    }

    public static void showRestartDialog(Context context) {
        Utils.verifyOnMainThread();
        if (restartDialogTitle == null) {
            restartDialogTitle = str("morphe_settings_restart_title");
        }
        if (restartDialogMessage == null) {
            restartDialogMessage = str("morphe_settings_restart_dialog_message");
        }
        if (restartDialogButtonText == null) {
            restartDialogButtonText = str("morphe_settings_restart");
        }

        Pair<Dialog, LinearLayout> dialogPair = CustomDialog.create(
                context,
                restartDialogTitle,              // Title.
                restartDialogMessage,            // Message.
                null,                            // No EditText.
                restartDialogButtonText,         // OK button text.
                () -> Utils.restartApp(context), // OK button action.
                () -> {},                        // Cancel button action (dismiss only).
                null,                            // No Neutral button text.
                null,                            // No Neutral button action.
                true                             // Dismiss dialog when onNeutralClick.
        );

        // Show the dialog.
        dialogPair.first.show();
    }

    /**
     * Import / Export Subroutines
     */
    public void showImportExportTextDialog() {
        try {
            Activity context = getActivity();
            // Must set text before showing dialog,
            // otherwise text is non-selectable if this preference is later reopened.
            existingSettings = Setting.exportToJson(context);
            currentImportExportEditText = getEditText(context);

            // Create a custom dialog with the EditText.
            Pair<Dialog, LinearLayout> dialogPair = CustomDialog.create(
                    context,
                    str("morphe_pref_import_export_title"), // Title.
                    null,     // No message (EditText replaces it).
                    currentImportExportEditText, // Pass the EditText.
                    str("morphe_settings_save"), // OK button text.
                    () -> importSettingsText(context, currentImportExportEditText.getText().toString()), // OK button action.
                    () -> {}, // Cancel button action (dismiss only).
                    str("morphe_settings_import_copy"), // Neutral button (Copy) text.
                    () -> Utils.setClipboard(currentImportExportEditText.getText().toString()), // Neutral button (Copy) action. Show the user the settings in JSON format.
                    true // Dismiss dialog when onNeutralClick.
            );

            final int margin = Dim.dp4;

            Button btnExport = CustomDialog.createButton(context, null, str("morphe_settings_export_file"), this::exportActivity, false, false);
            Button btnImport = CustomDialog.createButton(context, null, str("morphe_settings_import_file"), this::importActivity, false, false);

            LinearLayout.LayoutParams exportParams = new LinearLayout.LayoutParams(0, Dim.dp36, 1.0f);
            exportParams.setMargins(0, 0, margin, 0);
            btnExport.setLayoutParams(exportParams);

            LinearLayout.LayoutParams importParams = new LinearLayout.LayoutParams(0, Dim.dp36, 1.0f);
            importParams.setMargins(margin, 0, 0, 0);
            btnImport.setLayoutParams(importParams);

            LinearLayout fileButtonsContainer = getLinearLayout(context);
            fileButtonsContainer.addView(btnExport);
            fileButtonsContainer.addView(btnImport);

            dialogPair.second.addView(fileButtonsContainer, 2);

            dialogPair.first.setOnDismissListener(d -> currentImportExportEditText = null);

            // If there are no settings yet, then show the on-screen keyboard and bring focus to
            // the edit text. This makes it easier to paste saved settings after a reinstallation.
            dialogPair.first.setOnShowListener(dialogInterface -> {
                if (existingSettings.isEmpty() && currentImportExportEditText != null) {
                    currentImportExportEditText.postDelayed(() -> {
                        if (currentImportExportEditText != null) {
                            currentImportExportEditText.requestFocus();
                            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null) imm.showSoftInput(currentImportExportEditText, InputMethodManager.SHOW_IMPLICIT);
                        }
                    }, 100);
                }
            });

            // Show the dialog.
            dialogPair.first.show();
        } catch (Exception ex) {
            Logger.printException(() -> "showImportExportTextDialog failure", ex);
        }
    }

    @NonNull
    private static LinearLayout getLinearLayout(Context context) {
        LinearLayout fileButtonsContainer = new LinearLayout(context);
        fileButtonsContainer.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams fbParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        int marginTop = (int) TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 16f, context.getResources().getDisplayMetrics());
        fbParams.setMargins(0, marginTop, 0, 0);
        fileButtonsContainer.setLayoutParams(fbParams);
        return fileButtonsContainer;
    }

    @NonNull
    private EditText getEditText(Context context) {
        EditText editText = new EditText(context);
        editText.setText(existingSettings);
        editText.setAutofillHints((String) null);
        editText.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
                InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setSingleLine(false);
        editText.setTextSize(14);
        return editText;
    }

    public void exportActivity() {
        try {
            Setting.exportToJson(getActivity());
            String appName = Utils.getApplicationName();
            String safeAppName = appName.replaceAll("\\s+", "_");
            String formatDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            String fileName = safeAppName + "_Settings_" + formatDate + ".txt";

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, fileName);
            startActivityForResult(intent, WRITE_REQUEST_CODE);
        } catch (Exception ex) {
            Logger.printException(() -> "exportActivity failure", ex);
        }
    }

    public void importActivity() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, READ_REQUEST_CODE);
        } catch (Exception ex) {
            Logger.printException(() -> "importActivity failure", ex);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return new DebouncedListView(getActivity());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == WRITE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            exportTextToFile(data.getData());
        } else if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            importTextFromFile(data.getData());
        } else if (requestCode == ExportLogToClipboardPreference.WRITE_LOGS_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            saveLogsToUri(getContext(), data.getData());
        }
    }

    protected static void showLocalizedToast(String resourceKey, String fallbackMessage) {
        if (ResourceUtils.getIdentifier(ResourceType.STRING, resourceKey) != 0) {
            Utils.showToastLong(str(resourceKey));
        } else {
            Utils.showToastLong(fallbackMessage);
        }
    }

    private void exportTextToFile(Uri uri) {
        try (OutputStream out = getContext().getContentResolver().openOutputStream(uri, "rwt")) {
            if (out != null) {
                String textToExport = existingSettings;
                if (currentImportExportEditText != null) {
                    textToExport = currentImportExportEditText.getText().toString();
                }
                out.write(textToExport.getBytes(StandardCharsets.UTF_8));

                showLocalizedToast("morphe_settings_export_file_success", "Settings exported successfully");
            }
        } catch (Exception e) {
            showLocalizedToast("morphe_settings_export_file_failed", "Failed to export settings");
            Logger.printException(() -> "exportTextToFile failure", e);
        }
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    private void importTextFromFile(Uri uri) {
        try (InputStream in = getContext().getContentResolver().openInputStream(uri)) {
            if (in != null) {
                Scanner scanner = new Scanner(in, StandardCharsets.UTF_8.name()).useDelimiter("\\A");
                String result = scanner.hasNext() ? scanner.next() : "";

                if (currentImportExportEditText != null) {
                    currentImportExportEditText.setText(result);
                    showLocalizedToast("morphe_settings_import_file_success", "Settings imported successfully, tap Save to apply");
                } else {
                    importSettingsText(getContext(), result);
                }
            }
        } catch (Exception e) {
            showLocalizedToast("morphe_settings_import_file_failed", "Failed to import settings");
            Logger.printException(() -> "importTextFromFile failure", e);
        }
    }

    private void importSettingsText(Context context, String replacementSettings) {
        try {
            existingSettings = Setting.exportToJson(null);
            if (replacementSettings.equals(existingSettings)) {
                return;
            }
            settingImportInProgress = true;
            final boolean rebootNeeded = Setting.importFromJSON(getActivity(), replacementSettings);
            if (rebootNeeded) {
                showRestartDialog(context);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "importSettingsText failure", ex);
        } finally {
            settingImportInProgress = false;
        }
    }

    @SuppressLint("ResourceType")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUiMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        instance = new WeakReference<>(this);

        configurationListener = new ComponentCallbacks2() {
            @SuppressLint("ChromeOsOnConfigurationChanged")
            @Override
            public void onConfigurationChanged(@NonNull android.content.res.Configuration newConfig) {
                int newUiMode = newConfig.uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                if (currentUiMode != -1 && newUiMode != currentUiMode) {
                    currentUiMode = newUiMode;
                    Utils.setIsDarkModeEnabled(newUiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES);

                    Activity activity = getActivity();
                    if (activity != null) {
                        Intent intent = activity.getIntent();
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        activity.finish();
                        activity.overridePendingTransition(0, 0);
                        startActivity(intent);
                        activity.overridePendingTransition(0, 0);
                    }
                }
            }

            @Override
            public void onLowMemory() {}

            @Override
            public void onTrimMemory(int level) {}
        };
        getActivity().getApplicationContext().registerComponentCallbacks(configurationListener);

        try {
            PreferenceManager preferenceManager = getPreferenceManager();
            preferenceManager.setSharedPreferencesName(Setting.preferences.name);

            initialize();
            try {
                updateUIToSettingValues();
            } catch (Exception e) {
                Logger.printDebug(() -> "updateUIToSettingValues error: " + e.getMessage());
            }

            try {
                preferenceManager.getSharedPreferences().registerOnSharedPreferenceChangeListener(listener);
            } catch (Exception e) {
                Logger.printDebug(() -> "registerOnSharedPreferenceChangeListener error: " + e.getMessage());
            }
        } catch (Exception ex) {
            Logger.printException(() -> "onCreate() failure", ex);
        }
    }

    @Override
    public void onDestroy() {
        if (instance.get() == this) {
            instance = new WeakReference<>(null);
        }
        if (configurationListener != null && getActivity() != null) {
            getActivity().getApplicationContext().unregisterComponentCallbacks(configurationListener);
        }
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener);
        super.onDestroy();
    }

    /**
     * Sorts a {@link PreferenceGroup} and all nested subgroups by title or key.
     * <p>
     * The sort order is controlled by the {@link Sort} suffix present in the preference key.
     * Preferences without a key or without a {@link Sort} suffix remain in their original order.
     * <p>
     * Sorting is performed using {@link Collator} with the current user locale,
     * ensuring correct alphabetical ordering for all supported languages
     * (e.g., Ukrainian "Ñ–", German "ÃŸ", French accented characters, etc.).
     *
     * @param group the {@link PreferenceGroup} to sort
     */
    @SuppressWarnings("deprecation")
    protected static void sortPreferenceGroups(PreferenceGroup group) {
        Sort groupSort = Sort.fromKey(group.getKey(), Sort.UNSORTED);
        List<Pair<String, Preference>> preferences = new ArrayList<>();

        // Get cached Collator for locale-aware string comparison.
        Collator collator = getCollator();

        for (int i = 0, prefCount = group.getPreferenceCount(); i < prefCount; i++) {
            Preference preference = group.getPreference(i);

            final Sort preferenceSort;
            if (preference instanceof PreferenceGroup subGroup) {
                sortPreferenceGroups(subGroup);
                preferenceSort = groupSort; // Sort value for groups is for it's content, not itself.
            } else {
                // Allow individual preferences to set a key sorting.
                // Used to force a preference to the top or bottom of a group.
                preferenceSort = Sort.fromKey(preference.getKey(), groupSort);
            }

            final String sortValue;
            switch (preferenceSort) {
                case BY_TITLE:
                    sortValue = Utils.removePunctuationToLowercase(preference.getTitle());
                    break;
                case BY_KEY:
                    sortValue = preference.getKey();
                    break;
                case UNSORTED:
                    continue; // Keep original sorting.
                default:
                    throw new IllegalStateException();
            }

            preferences.add(new Pair<>(sortValue, preference));
        }

        // Sort the list using locale-specific collation rules with null safety.
        preferences.sort((pair1, pair2) -> {
            String s1 = pair1.first != null ? pair1.first : "";
            String s2 = pair2.first != null ? pair2.first : "";
            return collator.compare(s1, s2);
        });

        // Reassign order values to reflect the new sorted sequence
        int index = 0;
        for (Pair<String, Preference> pair : preferences) {
            int order = index++;
            Preference pref = pair.second;

            // Move any screens, intents, and the one off About preference to the top.
            if (pref instanceof PreferenceScreen || pref instanceof MorpheAboutPreference
                    || pref.getIntent() != null) {
                // Any arbitrary large number.
                order -= 1000;
            }

            pref.setOrder(order);
        }
    }

    /**
     * {@link PreferenceScreen} and {@link PreferenceGroup} sorting styles.
     */
    private enum Sort {
        /**
         * Sort by the localized preference title.
         */
        BY_TITLE("_sort_by_title"),

        /**
         * Sort by the preference keys.
         */
        BY_KEY("_sort_by_key"),

        /**
         * Unspecified sorting.
         */
        UNSORTED("_sort_by_unsorted");

        final String keySuffix;

        Sort(String keySuffix) {
            this.keySuffix = keySuffix;
        }

        static Sort fromKey(@Nullable String key, Sort defaultSort) {
            if (key != null) {
                for (Sort sort : values()) {
                    if (key.endsWith(sort.keySuffix)) {
                        return sort;
                    }
                }
            }
            return defaultSort;
        }
    }

    /**
     * Returns a cached Collator for the current locale, or creates a new one if locale changed.
     */
    private static Collator getCollator() {
        Locale currentLocale = BaseSettings.MORPHE_LANGUAGE.get().getLocale();

        if (cachedCollator == null || !currentLocale.equals(cachedCollatorLocale)) {
            cachedCollatorLocale = currentLocale;
            cachedCollator = Collator.getInstance(currentLocale);
            cachedCollator.setStrength(Collator.SECONDARY); // Case-insensitive, diacritic-insensitive.
        }

        return cachedCollator;
    }
}
