package com.wmods.wppenhacer.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.wmods.wppenhacer.ui.fragments.CustomizationFragment;
import com.wmods.wppenhacer.ui.fragments.GeneralFragment;
import com.wmods.wppenhacer.ui.fragments.HomeFragment;
import com.wmods.wppenhacer.ui.fragments.MediaFragment;
import com.wmods.wppenhacer.ui.fragments.PrivacyFragment;
import com.wmods.wppenhacer.ui.fragments.RecordingsFragment;

public class MainPagerAdapter extends FragmentStateAdapter {

    private final boolean isRecordingEnabled;

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        var prefs = PreferenceManager.getDefaultSharedPreferences(fragmentActivity);
        isRecordingEnabled = prefs.getBoolean("call_recording_enable", false);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) return new PrivacyFragment();
        if (position == 1) return new GeneralFragment();
        if (position == 2) return new HomeFragment();
        if (position == 3) return new MediaFragment();
        if (position == 4) return new CustomizationFragment();
        return new PrivacyFragment();
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}