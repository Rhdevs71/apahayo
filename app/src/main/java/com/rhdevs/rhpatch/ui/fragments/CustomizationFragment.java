package com.rhdevs.rhpatch.ui.fragments;

import android.content.Intent;
import androidx.preference.Preference;
import com.rhdevs.rhpatch.activity.ThemeStoreActivity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rhdevs.rhpatch.R;
import com.rhdevs.rhpatch.ui.fragments.base.BasePreferenceFragment;

public class CustomizationFragment extends BasePreferenceFragment {
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPreferencesFromResource(R.xml.fragment_customization, rootKey);

                Preference storePref = findPreference("theme_store_online");
        if (storePref != null) {
            storePref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getContext(), ThemeStoreActivity.class);
                startActivity(intent);
                return true;
            });
        }

        Preference studioPref = findPreference("visual_theme_studio");
        if (studioPref != null) {
            studioPref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getContext(), com.rhdevs.rhpatch.activity.ThemeStudioActivity.class);
                startActivity(intent);
                return true;
            });
        }

    }

    @Override
    public void onResume() {
        super.onResume();
    }
    
    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Handle scroll to preference from search
        if (getActivity() != null && getActivity().getIntent() != null) {
            String scrollToKey = getActivity().getIntent().getStringExtra("scroll_to_preference");
            if (scrollToKey != null) {
                scrollToPreference(scrollToKey);
                // Clear the intent extra
                getActivity().getIntent().removeExtra("scroll_to_preference");
            }
        }
    }

}

