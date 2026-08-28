package com.rhdevs.rhpatch.ui.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.rhdevs.rhpatch.AppPatchInfoKt;
import com.rhdevs.rhpatch.AppPatchInfo;
import com.rhdevs.rhpatch.activity.AppPatchSettingsActivity;
import com.rhdevs.rhpatch.R;

public class ModulesFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_modules, container, false);
        LinearLayout containerLayout = view.findViewById(R.id.modules_container);

        if (containerLayout != null) {
            for (AppPatchInfo appPatchInfo : AppPatchInfoKt.getAppPatchConfigurations()) {
                boolean isInstalled = false;
                try {
                    requireContext().getPackageManager().getPackageInfo(appPatchInfo.getPackageName(), 0);
                    isInstalled = true;
                } catch (Exception ignored) {
                }

                View card = inflater.inflate(R.layout.item_module_card, containerLayout, false);
                TextView appNameText = card.findViewById(R.id.app_name);
                TextView appPackageText = card.findViewById(R.id.app_package);
                TextView badgeText = card.findViewById(R.id.status_badge);

                if (appNameText != null) appNameText.setText(appPatchInfo.getAppName());
                if (appPackageText != null) appPackageText.setText(appPatchInfo.getPackageName());

                ImageView iconView = card.findViewById(R.id.app_icon);
                if (iconView != null && isInstalled) {
                    try {
                        iconView.setImageDrawable(requireContext().getPackageManager().getApplicationIcon(appPatchInfo.getPackageName()));
                    } catch (Exception ignored) {
                    }
                }

                if (badgeText != null) {
                    if (isInstalled) {
                        badgeText.setText("Terpasang");
                        badgeText.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary));
                    } else {
                        badgeText.setText("Tidak Terpasang");
                        badgeText.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_muted));
                    }
                }

                card.setOnClickListener(v -> {
                    Intent intent = new Intent(requireContext(), AppPatchSettingsActivity.class);
                    intent.putExtra(AppPatchSettingsActivity.ARGUMENT_APP_NAME, appPatchInfo.getAppName());
                    startActivity(intent);
                });

                containerLayout.addView(card);
            }
        }
        return view;
    }
}
