package com.rhdevs.rhpatch.ui.fragments;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.rhdevs.rhpatch.App;
import com.rhdevs.rhpatch.BuildConfig;
import com.rhdevs.rhpatch.R;
import com.rhdevs.rhpatch.activities.MainActivity;
import com.rhdevs.rhpatch.adapter.LogLineAdapter;
import com.rhdevs.rhpatch.databinding.DialogDiagnosticsLogBinding;
import com.rhdevs.rhpatch.databinding.FragmentHomeBinding;
import com.rhdevs.rhpatch.ui.fragments.base.BaseFragment;
import com.rhdevs.rhpatch.utils.FilePicker;
import com.rhdevs.rhpatch.utils.RootDiagnostics;
import com.rhdevs.rhpatch.xposed.core.FeatureLoader;
import com.rhdevs.rhpatch.xposed.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

 import java.net.UnknownHostException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;

import rikka.core.util.IOUtils;

public class HomeFragment extends BaseFragment {

    private FragmentHomeBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        var intentFilter = new IntentFilter(BuildConfig.APPLICATION_ID + ".RECEIVER_WPP");
        ContextCompat.registerReceiver(requireContext(), new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    if (FeatureLoader.PACKAGE_WPP.equals(intent.getStringExtra("PKG")))
                        receiverBroadcastWpp(context, intent);
                    else
                        receiverBroadcastBusiness(context, intent);
                } catch (Exception ignored) {
                }
            }
        }, intentFilter, ContextCompat.RECEIVER_EXPORTED);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        checkStateWpp(requireActivity());

        binding.rebootBtn.setOnClickListener(view -> {
            animateClick(view);
            App.instance.restartApp(FeatureLoader.PACKAGE_WPP);
            disableWpp(requireActivity());
        });

        binding.scrollDiagBtn.setOnClickListener(view -> {
            animateClick(view);
            showDiagnosticsDialog();
        });

        binding.rebootBtn2.setOnClickListener(view -> {
            animateClick(view);
            App.instance.restartApp(FeatureLoader.PACKAGE_BUSINESS);
        App.instance.restartApp("com.instagram.android");
        App.instance.restartApp("com.facebook.katana");
        App.instance.restartApp("com.zhiliaoapp.musically");
            disableBusiness(requireActivity());
        });

        

        

        

        // Removed updateCard and diagBtn references

        checkForUpdates();

        startCardAnimations();

        return binding.getRoot();
    }

    private void startCardAnimations() {
        var context = getContext();
        if (context == null) return;
        var slideUp = AnimationUtils.loadAnimation(context, R.anim.slide_up);
        var fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in);

        binding.status2.postDelayed(() -> {
            if (!isAdded() || binding == null) return;
            var anim = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up);
            binding.status2.startAnimation(anim);
        }, 100);

        binding.status3.postDelayed(() -> {
            if (!isAdded() || binding == null) return;
            var anim = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up);
            binding.status3.startAnimation(anim);
        }, 200);
    }

    private void animateClick(View view) {
        var scaleIn = AnimationUtils.loadAnimation(getContext(), R.anim.scale_in);
        view.startAnimation(scaleIn);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @SuppressLint("StringFormatInvalid")
    private void receiverBroadcastBusiness(Context context, Intent intent) {
        if (App.isOriginalPackage()) binding.status3.setVisibility(View.VISIBLE);
        binding.statusTitle3.setText(R.string.business_in_background);
        var version = intent.getStringExtra("VERSION");
        var supported_list = Arrays.asList(context.getResources().getStringArray(R.array.supported_versions_business));
        if (version != null && supported_list.stream().anyMatch(s -> version.startsWith(s.replace(".xx", "")))) {
            binding.statusSummary3.setText(getString(R.string.version_s, version));
        } else {
            binding.statusSummary3.setText(getString(R.string.version_s_not_listed, version));
        }
        binding.rebootBtn2.setVisibility(View.VISIBLE);
        binding.statusSummary3.setVisibility(View.VISIBLE);
        binding.statusIcon3.setImageResource(R.drawable.ic_round_check_circle_24);
        binding.statusIcon3.setColorFilter(Color.parseColor("#10B981"));
    }

    @SuppressLint("StringFormatInvalid")
    private void receiverBroadcastWpp(Context context, Intent intent) {
        binding.statusTitle2.setText(R.string.whatsapp_in_background);
        var version = intent.getStringExtra("VERSION");
        var supported_list = Arrays.asList(context.getResources().getStringArray(R.array.supported_versions_wpp));

        if (version != null && supported_list.stream().anyMatch(s -> version.startsWith(s.replace(".xx", "")))) {
            binding.statusSummary1.setText(getString(R.string.version_s, version));
        } else {
            binding.statusSummary1.setText(getString(R.string.version_s_not_listed, version));
        }
        binding.rebootBtn.setVisibility(View.VISIBLE);
        binding.statusSummary1.setVisibility(View.VISIBLE);
        binding.statusIcon2.setImageResource(R.drawable.ic_round_check_circle_24);
        binding.statusIcon2.setColorFilter(Color.parseColor("#10B981"));
    }

    @SuppressLint("StringFormatInvalid")
    private void checkStateWpp(FragmentActivity activity) {
        if (isInstalled(FeatureLoader.PACKAGE_WPP) && App.isOriginalPackage()) {
            disableWpp(activity);
        } else {
            binding.status2.setVisibility(View.GONE);
        }
        if (App.isOriginalPackage())
            binding.status3.setVisibility(View.GONE);
        checkWpp(activity);
    }

    private boolean isInstalled(String packageWpp) {
        try {
            App.instance.getPackageManager().getPackageInfo(packageWpp, 0);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    private void disableBusiness(FragmentActivity activity) {
        binding.statusIcon3.setImageResource(R.drawable.ic_round_error_outline_24);
        binding.statusIcon3.setColorFilter(Color.parseColor("#EF4444"));
        binding.statusTitle3.setText(R.string.business_is_not_running_or_has_not_been_activated_in_lsposed);
        binding.statusSummary3.setVisibility(View.GONE);
        binding.rebootBtn2.setVisibility(View.VISIBLE);
    }

    private void disableWpp(FragmentActivity activity) {
        binding.statusIcon2.setImageResource(R.drawable.ic_round_error_outline_24);
        binding.statusIcon2.setColorFilter(Color.parseColor("#EF4444"));
        binding.statusTitle2.setText(R.string.whatsapp_is_not_running_or_has_not_been_activated_in_lsposed);
        binding.statusSummary1.setVisibility(View.GONE);
        binding.rebootBtn.setVisibility(View.VISIBLE);
    }

    private static void checkWpp(FragmentActivity activity) {
        Intent checkWpp = new Intent(BuildConfig.APPLICATION_ID + ".CHECK_WPP");
        checkWpp.setPackage(FeatureLoader.PACKAGE_WPP);
        activity.sendBroadcast(checkWpp);

        Intent checkBusiness = new Intent(BuildConfig.APPLICATION_ID + ".CHECK_WPP");
        checkBusiness.setPackage(FeatureLoader.PACKAGE_BUSINESS);
        activity.sendBroadcast(checkBusiness);
    }

    private void checkForUpdates() {
        // Redesigned: Handled by UpdateChecker.kt
    }

    private void updateCardState(boolean success, boolean isUpToDate, @Nullable String newVersion) {
        // Removed UI components
    }

    private void showDiagnosticsDialog() {
        if (!isAdded()) return;
        var context = requireContext();
        var dialogBinding = DialogDiagnosticsLogBinding.inflate(LayoutInflater.from(context));
        var adapter = new LogLineAdapter();

        dialogBinding.logRecycler.setLayoutManager(new LinearLayoutManager(context));
        dialogBinding.logRecycler.setAdapter(adapter);

        var dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.diag_dialog_title)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.diag_close, null)
                .setNeutralButton(R.string.diag_share, (dialogInterface, which) -> {
                    try {
                        var logHeader = new StringBuilder();
                        logHeader.append("=== DIAGNOSTIK RHPATCH ===\n\n");
                        for (var entry : adapter.getLogs()) {
                            logHeader.append(entry.getMessage()).append("\n");
                        }
                        logHeader.append("\n--- BEGIN LOGCAT ---\n");

                        var cacheHeaderFile = new java.io.File(context.getCacheDir(), "diag_header.tmp");
                        java.nio.file.Files.write(cacheHeaderFile.toPath(), logHeader.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));

                        var targetLogFile = new java.io.File(context.getCacheDir(), "logAndroid_Rhpatch.txt");
                        var cmd = "cat " + cacheHeaderFile.getAbsolutePath() + " > " + targetLogFile.getAbsolutePath() +
                                " && logcat -d >> " + targetLogFile.getAbsolutePath() +
                                " && cat /data/adb/lspd/log/error.log >> " + targetLogFile.getAbsolutePath() +
                                " && cat /data/adb/lspd/log/modules.log >> " + targetLogFile.getAbsolutePath() +
                                " && cat /data/adb/lspd/log/verbose.log >> " + targetLogFile.getAbsolutePath() +
                                " && chmod 666 " + targetLogFile.getAbsolutePath() +
                                " && cp " + targetLogFile.getAbsolutePath() + " /sdcard/Download/logAndroid_Rhpatch.txt 2>/dev/null; " +
                                "cp " + targetLogFile.getAbsolutePath() + " /sdcard/logAndroid_Rhpatch.txt 2>/dev/null";

                        com.topjohnwu.superuser.Shell.cmd(cmd).exec();

                        var uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                context.getPackageName() + ".fileprovider",
                                targetLogFile
                        );

                        var shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Log Diagnosa Rhpatch");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, "Terlampir file log Diagnosa Root & Sistem Rhpatch secara lengkap.");
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.diag_share)));
                    } catch (Exception e) {
                        try {
                            var fallbackIntent = new Intent(Intent.ACTION_SEND);
                            fallbackIntent.setType("text/plain");
                            var fallbackText = new StringBuilder("Gagal melampirkan file.\n\nLog Diagnosa Root:\n\n");
                            for (var entry : adapter.getLogs()) {
                                fallbackText.append(entry.getMessage()).append("\n");
                            }
                            fallbackIntent.putExtra(Intent.EXTRA_TEXT, fallbackText.toString());
                            startActivity(Intent.createChooser(fallbackIntent, getString(R.string.diag_share)));
                        } catch (Exception ignored) {
                        }
                    }
                })
                .setCancelable(true)
                .show();

        var queue = new java.util.ArrayList<RootDiagnostics.LogEntry>();
        RootDiagnostics.INSTANCE.runDiagnostics(context, entry -> {
            synchronized (queue) {
                queue.add(entry);
            }
        });

        var handler = new Handler(Looper.getMainLooper());
        var poller = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || !dialog.isShowing()) return;
                var batch = new java.util.ArrayList<RootDiagnostics.LogEntry>();
                synchronized (queue) {
                    batch.addAll(queue);
                    queue.clear();
                }
                if (!batch.isEmpty()) {
                    for (var e : batch) {
                        adapter.add(e);
                    }
                    int count = adapter.getItemCount();
                    if (count > 0) {
                        dialogBinding.logRecycler.scrollToPosition(count - 1);
                    }
                }
                handler.postDelayed(this, 120);
            }
        };
        handler.postDelayed(poller, 120);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


