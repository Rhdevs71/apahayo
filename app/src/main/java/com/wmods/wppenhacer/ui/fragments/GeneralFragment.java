package com.wmods.wppenhacer.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wmods.wppenhacer.R;
import com.wmods.wppenhacer.ui.fragments.base.BaseFragment;
import com.wmods.wppenhacer.ui.fragments.base.BasePreferenceFragment;

public class GeneralFragment extends BaseFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        var root = super.onCreateView(inflater, container, savedInstanceState);
        if (savedInstanceState == null) {
            getChildFragmentManager().beginTransaction().add(R.id.frag_container, new GeneralPreferenceFragment()).commitNow();
        }
        
        // Handle scroll to preference from search
        if (getActivity() != null && getActivity().getIntent() != null) {
            String scrollToKey = getActivity().getIntent().getStringExtra("scroll_to_preference");
            if (scrollToKey != null) {
                getView().postDelayed(() -> {
                    BasePreferenceFragment activeFragment = (BasePreferenceFragment) getChildFragmentManager().findFragmentById(R.id.frag_container);
                    if (activeFragment != null) {
                        activeFragment.scrollToPreference(scrollToKey);
                    }
                }, 300);
                // Clear the intent extra
                getActivity().getIntent().removeExtra("scroll_to_preference");
            }
        }
        
        return root;
    }

    public static class GeneralPreferenceFragment extends BasePreferenceFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            super.onCreatePreferences(savedInstanceState, rootKey);
            setPreferencesFromResource(R.xml.fragment_general, rootKey);
        }

        @Override
        public boolean onPreferenceTreeClick(@NonNull androidx.preference.Preference preference) {
            String key = preference.getKey();
            if ("message_scheduler_settings".equals(key)) {
                android.content.Intent intent = new android.content.Intent(getContext(), com.wmods.wppenhacer.activities.MessageSchedulerActivity.class);
                startActivity(intent);
                return true;
            } else if ("auto_reply_settings".equals(key)) {
                android.content.Intent intent = new android.content.Intent(getContext(), com.wmods.wppenhacer.activities.AutoReplyActivity.class);
                startActivity(intent);
                return true;
            } else if ("custom_folders_settings".equals(key)) {
                android.content.Intent intent = new android.content.Intent(getContext(), com.wmods.wppenhacer.activities.CustomFoldersActivity.class);
                startActivity(intent);
                return true;
            } else if ("use_accessibility_sender".equals(key)) {
                if (preference instanceof rikka.material.preference.MaterialSwitchPreference) {
                    boolean isChecked = ((rikka.material.preference.MaterialSwitchPreference) preference).isChecked();
                    if (isChecked) {
                        // Check if accessibility service is enabled
                        boolean accessibilityEnabled = false;
                        android.view.accessibility.AccessibilityManager am = (android.view.accessibility.AccessibilityManager) getContext().getSystemService(android.content.Context.ACCESSIBILITY_SERVICE);
                        java.util.List<android.accessibilityservice.AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
                        for (android.accessibilityservice.AccessibilityServiceInfo service : enabledServices) {
                            if (service.getId().contains("AutoSenderAccessibilityService")) {
                                accessibilityEnabled = true;
                                break;
                            }
                        }
                        
                        // Check if device admin is enabled
                        android.app.admin.DevicePolicyManager dpm = (android.app.admin.DevicePolicyManager) getContext().getSystemService(android.content.Context.DEVICE_POLICY_SERVICE);
                        android.content.ComponentName componentName = new android.content.ComponentName(getContext(), com.rhdevs.rhpatch.receivers.WaDeviceAdminReceiver.class);
                        boolean adminEnabled = dpm.isAdminActive(componentName);

                        if (!accessibilityEnabled || !adminEnabled) {
                            final boolean finalAccessibilityEnabled = accessibilityEnabled;
                            final boolean finalAdminEnabled = adminEnabled;

                            new androidx.appcompat.app.AlertDialog.Builder(getContext())
                                .setTitle("Perizinan Tambahan Diperlukan")
                                .setMessage("Fitur 'Accessibility Auto-Sender' memerlukan akses Aksesibilitas (untuk klik otomatis) dan Administrator Perangkat (untuk mengunci layar kembali).\n\nMohon izinkan keduanya di pengaturan.")
                                .setPositiveButton("Buka Pengaturan", (dialog, which) -> {
                                    if (!finalAccessibilityEnabled) {
                                        startActivity(new android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
                                    }
                                    if (!finalAdminEnabled) {
                                        android.content.Intent intent = new android.content.Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                                        intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
                                        intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Diperlukan untuk mengunci layar otomatis.");
                                        startActivity(intent);
                                    }
                                })
                                .setNegativeButton("Batal", (dialog, which) -> {
                                    ((rikka.material.preference.MaterialSwitchPreference) preference).setChecked(false);
                                })
                                .show();
                        }
                    }
                }
                return true;
            }

            return super.onPreferenceTreeClick(preference);
        }

        @Override
        public void onResume() {
            super.onResume();
        }
    }

    public static class HomeGeneralPreference extends BasePreferenceFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            super.onCreatePreferences(savedInstanceState, rootKey);
            setPreferencesFromResource(R.xml.preference_general_home, rootKey);
            setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class HomeScreenGeneralPreference extends BasePreferenceFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            super.onCreatePreferences(savedInstanceState, rootKey);
            setPreferencesFromResource(R.xml.preference_general_homescreen, rootKey);
            setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class ConversationGeneralPreference extends BasePreferenceFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            super.onCreatePreferences(savedInstanceState, rootKey);
            setPreferencesFromResource(R.xml.preference_general_conversation, rootKey);
            setDisplayHomeAsUpEnabled(true);
        }
    }

}