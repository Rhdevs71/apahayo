package com.rhdevs.rhpatch.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.waseemsabir.betterypermissionhelper.BatteryPermissionHelper;
import com.rhdevs.rhpatch.App;
import com.rhdevs.rhpatch.R;
import com.rhdevs.rhpatch.activities.base.BaseActivity;
import com.rhdevs.rhpatch.adapter.MainPagerAdapter;
import com.rhdevs.rhpatch.databinding.ActivityMainBinding;
import com.rhdevs.rhpatch.ui.fragments.GeneralFragment;
import com.rhdevs.rhpatch.ui.fragments.HomeFragment;
import com.rhdevs.rhpatch.ui.fragments.base.BasePreferenceFragment;
import com.rhdevs.rhpatch.utils.FilePicker;

import java.io.File;

public class MainActivity extends BaseActivity {

    private ActivityMainBinding binding;
    private BatteryPermissionHelper batteryPermissionHelper = BatteryPermissionHelper.Companion.getInstance();
    private String pendingScrollToPreference = null;
    private int pendingScrollToFragment = -1;
    private String pendingParentKey = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        App.changeLanguage(this);
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        MainActivityHelper.setupHeroStatusCard(this, binding, "com.whatsapp");

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        MainPagerAdapter pagerAdapter = new MainPagerAdapter(this);
        binding.viewPager.setAdapter(pagerAdapter);
        binding.viewPager.setUserInputEnabled(true);
        binding.viewPager.setOffscreenPageLimit(5);

        new com.google.android.material.tabs.TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(R.string.privacy);
                    break;
                case 1:
                    tab.setText(R.string.general);
                    break;
                case 2:
                    tab.setText("Status");
                    break;
                case 3:
                    tab.setText(R.string.media);
                    break;
                case 4:
                    tab.setText(R.string.perso);
                    break;
            }
        }).attach();

        var prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("call_recording_enable", false)) {
            MenuItem recordingsItem = binding.toolbar.getMenu().findItem(R.id.navigation_recordings);
            if (recordingsItem != null) {
                recordingsItem.setVisible(false);
            }
        }
        
        binding.viewPager.setCurrentItem(0, false);
        createMainDir();
        FilePicker.registerFilePicker(this);
        
        // Handle incoming navigation from search
        handleIncomingIntent(getIntent());
    }

    private void createMainDir() {
        var nomedia = new File(App.getRhpatchFolder(), ".nomedia");
        if (nomedia.exists()) {
            nomedia.delete();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIncomingIntent(intent);
    }
    
    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        
        int fragmentPosition = intent.getIntExtra("navigate_to_fragment", -1);
        String preferenceKey = intent.getStringExtra("scroll_to_preference");
        String parentKey = intent.getStringExtra("parent_preference");
        
        if (fragmentPosition >= 0 && preferenceKey != null) {
            // Store the scroll target
            pendingScrollToPreference = preferenceKey;
            pendingScrollToFragment = fragmentPosition;
            pendingParentKey = parentKey;
            
            // Navigate to the fragment (onPageSelected will handle the scroll)
            binding.viewPager.setCurrentItem(fragmentPosition, false);
            
            // Clear intent extras
            intent.removeExtra("navigate_to_fragment");
            intent.removeExtra("scroll_to_preference");
            intent.removeExtra("parent_preference");
        } else if (fragmentPosition >= 0) {
            // Just navigate without scrolling
            binding.viewPager.setCurrentItem(fragmentPosition, true);
        }
    }
    
    private void scrollToPreferenceInCurrentFragment(String preferenceKey, String parentKey) {
        // Get the current fragment from the ViewPager
        int currentItem = binding.viewPager.getCurrentItem();
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + currentItem);
        
        if (fragment == null) return;
        
        // Handle different fragment types
        if (fragment instanceof GeneralFragment || fragment instanceof HomeFragment) {
            // These fragments have child fragments
            if (parentKey != null && !parentKey.isEmpty()) {
                // Navigate to sub-fragment first, then scroll
                navigateToSubFragmentAndScroll(fragment, parentKey, preferenceKey);
            } else {
                // Direct scroll in current child fragment
                scrollInChildFragment(fragment, preferenceKey);
            }
        } else if (fragment instanceof BasePreferenceFragment) {
            // Direct preference fragments (no nesting)
            ((BasePreferenceFragment) fragment).scrollToPreference(preferenceKey);
        }
    }
    
    private void navigateToSubFragmentAndScroll(Fragment parentFragment, String parentKey, String childPreferenceKey) {
        // Directly instantiate the sub-fragment
        Fragment subFragment = null;
        
        switch (parentKey) {
            case "general_home":
                subFragment = new GeneralFragment.HomeGeneralPreference();
                break;
            case "homescreen":
                subFragment = new GeneralFragment.HomeScreenGeneralPreference();
                break;
            case "conversation":
                subFragment = new GeneralFragment.ConversationGeneralPreference();
                break;
        }
        
        if (subFragment != null && parentFragment.getView() != null) {
            final Fragment finalSubFragment = subFragment;
            // Replace the current child fragment
            parentFragment.getChildFragmentManager().beginTransaction()
                .replace(R.id.frag_container, subFragment)
                .commitNow();
            
            // Wait for fragment to be ready, then scroll
            parentFragment.getView().postDelayed(() -> {
                if (finalSubFragment instanceof BasePreferenceFragment) {
                    ((BasePreferenceFragment) finalSubFragment).scrollToPreference(childPreferenceKey);
                }
            }, 400);
        }
    }
    
    private void scrollInChildFragment(Fragment parentFragment, String preferenceKey) {
        Fragment childFragment = parentFragment.getChildFragmentManager().findFragmentById(R.id.frag_container);
        if (childFragment instanceof BasePreferenceFragment) {
            ((BasePreferenceFragment) childFragment).scrollToPreference(preferenceKey);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.header_menu, menu);
        var powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
            menu.findItem(R.id.batteryoptimization).setVisible(false);
        }
        return true;
    }

    @SuppressLint("BatteryLife")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_search) {
            var options = ActivityOptionsCompat.makeCustomAnimation(
                    this, R.anim.slide_in_right, R.anim.slide_out_left);
            startActivity(new Intent(this, SearchActivity.class), options.toBundle());
            return true;
        } else if (item.getItemId() == R.id.batteryoptimization) {
            if (batteryPermissionHelper.isBatterySaverPermissionAvailable(this, true)) {
                batteryPermissionHelper.getPermission(this, true, true);
            } else {
                var intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 0);
            }
        }
        if (item.getItemId() == R.id.navigation_recordings) { binding.viewPager.setCurrentItem(5, true); return true; } return super.onOptionsItemSelected(item);
    }

    public static boolean isXposedEnabled() {
        return Boolean.parseBoolean("false");
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        backupConfiguration();
    }

    private void backupConfiguration() {
        try {
            var prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
            java.util.Map<String, ?> allEntries = prefs.getAll();
            org.json.JSONObject jsonObject = new org.json.JSONObject();
            for (java.util.Map.Entry<String, ?> entry : allEntries.entrySet()) {
                jsonObject.put(entry.getKey(), entry.getValue());
            }
            
            java.io.File dir = new java.io.File(android.os.Environment.getExternalStorageDirectory(), "RHPatch");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            java.io.File backupFile = new java.io.File(dir, "config_backup.json");
            try (java.io.FileWriter fileWriter = new java.io.FileWriter(backupFile)) {
                fileWriter.write(jsonObject.toString(4));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class DepthPageTransformer implements ViewPager2.PageTransformer {
        private static final float MIN_SCALE = 0.85f;

        @Override
        public void transformPage(@NonNull android.view.View page, float position) {
            int pageWidth = page.getWidth();

            if (position < -1) {
                page.setAlpha(0f);
            } else if (position <= 0) {
                page.setAlpha(1f);
                page.setTranslationX(0f);
                page.setTranslationZ(0f);
                page.setScaleX(1f);
                page.setScaleY(1f);
            } else if (position <= 1) {
                page.setAlpha(1 - position);
                page.setTranslationX(pageWidth * -position);
                page.setTranslationZ(-1f);
                float scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position));
                page.setScaleX(scaleFactor);
                page.setScaleY(scaleFactor);
            } else {
                page.setAlpha(0f);
            }
        }
    }
}

