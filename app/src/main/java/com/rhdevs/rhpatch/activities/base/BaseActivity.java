package com.rhdevs.rhpatch.activities.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.rhdevs.rhpatch.App;
import com.rhdevs.rhpatch.R;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(App.changeLanguage(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int mode = 0;
        try {
            mode = Integer.parseInt(prefs.getString("thememode", "0"));
        } catch (Exception ignored) {}
        App.setThemeMode(mode);

        boolean isDark = (mode == 1);
        if (mode == 0) {
            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            isDark = (currentNightMode == Configuration.UI_MODE_NIGHT_YES);
        }

        if (isDark) {
            setTheme(R.style.Theme);
        } else {
            setTheme(R.style.Theme_Light);
        }

        getTheme().applyStyle(rikka.material.preference.R.style.ThemeOverlay_Rikka_Material3_Preference, true);
        getTheme().applyStyle(R.style.ThemeOverlay, true);
        super.onCreate(savedInstanceState);
    }
}
