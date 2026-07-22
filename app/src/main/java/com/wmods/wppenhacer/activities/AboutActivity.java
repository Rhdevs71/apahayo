package com.wmods.wppenhacer.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.wmods.wppenhacer.R;
import com.wmods.wppenhacer.activities.base.BaseActivity;
import com.wmods.wppenhacer.databinding.ActivityAboutBinding;

public class AboutActivity extends BaseActivity {

    private static final String[][] CONTRIBUTORS = {
            {"Rhdevs", "https://github.com/Rhdevs71"}
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityAboutBinding binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnTelegram.setOnClickListener(v -> openUrl("https://t.me/apahayo"));
        binding.btnGithub.setOnClickListener(view -> openUrl("https://github.com/Rhdevs71/apahayo"));

        int topMargin = getResources().getDimensionPixelSize(R.dimen.spacing_small);
        for (int i = 0; i < CONTRIBUTORS.length; i++) {
            String[] contributor = CONTRIBUTORS[i];
            MaterialButton button = new MaterialButton(new ContextThemeWrapper(this, R.style.ModernButton_Outlined));
            button.setText(contributor[0]);
            button.setIconResource(R.drawable.ic_github);
            button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
            button.setIconPadding(getResources().getDimensionPixelSize(R.dimen.spacing_small));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            if (i > 0) {
                params.topMargin = topMargin;
            }
            button.setLayoutParams(params);
            button.setOnClickListener(v -> openUrl(contributor[1]));
            binding.contributorsContainer.addView(button);
        }

    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);

    }
}
