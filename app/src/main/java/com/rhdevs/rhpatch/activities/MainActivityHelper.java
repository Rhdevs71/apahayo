package com.rhdevs.rhpatch.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.rhdevs.rhpatch.R;
import com.rhdevs.rhpatch.databinding.ActivityMainBinding;

import java.io.File;

public class MainActivityHelper {
    public static void setupHeroStatusCard(AppCompatActivity activity, ActivityMainBinding binding, String waPkg) {
        binding.btnRestartWa.setOnClickListener(v -> {
            try {
                Runtime.getRuntime().exec(new String[]{"su", "-c", "am force-stop " + waPkg});
                Toast.makeText(activity, "WhatsApp berhasil di-restart!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                try {
                    Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
                    intent.setData(Uri.parse("package:" + waPkg));
                    activity.startActivity(intent);
                    Toast.makeText(activity, "Silakan pilih 'Paksa Berhenti' untuk merestart WhatsApp", Toast.LENGTH_LONG).show();
                } catch (Exception ex) {
                    Toast.makeText(activity, "Gagal membuka pengaturan WhatsApp", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.btnCleanCache.setOnClickListener(v -> {
            try {
                File cacheDir = activity.getCacheDir();
                if (cacheDir != null && cacheDir.isDirectory()) {
                    File[] files = cacheDir.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            f.delete();
                        }
                    }
                }
                Toast.makeText(activity, "Cache modul berhasil dibersihkan!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(activity, "Pembersihan cache selesai", Toast.LENGTH_SHORT).show();
            }
        });
    }
}