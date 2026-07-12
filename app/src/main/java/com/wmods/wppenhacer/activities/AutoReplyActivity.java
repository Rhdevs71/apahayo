package com.wmods.wppenhacer.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.wmods.wppenhacer.R;
import com.wmods.wppenhacer.activities.base.BaseActivity;
import com.wmods.wppenhacer.database.AppDatabase;
import com.wmods.wppenhacer.database.AutoReplyRule;
import com.wmods.wppenhacer.databinding.ActivityAutoReplyBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutoReplyActivity extends BaseActivity {

    private ActivityAutoReplyBinding binding;
    private AutoReplyRuleAdapter adapter;
    private List<AutoReplyRule> rulesList = new ArrayList<>();
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAutoReplyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Setup Global Switch
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        binding.switchGlobalAutoReply.setChecked(prefs.getBoolean("auto_reply_enabled", false));
        binding.switchGlobalAutoReply.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("auto_reply_enabled", isChecked).apply();
            Toast.makeText(this, isChecked ? "Auto Reply enabled globally" : "Auto Reply disabled globally", Toast.LENGTH_SHORT).show();
        });

        // Setup RecyclerView
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AutoReplyRuleAdapter();
        binding.recyclerView.setAdapter(adapter);

        binding.fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditAutoReplyRuleActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAutoReplyRules();
    }

    private void loadAutoReplyRules() {
        dbExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            final List<AutoReplyRule> list = db.autoReplyRuleDao().getAll();
            runOnUiThread(() -> {
                rulesList = list;
                if (rulesList.isEmpty()) {
                    binding.emptyView.setVisibility(View.VISIBLE);
                    binding.recyclerView.setVisibility(View.GONE);
                } else {
                    binding.emptyView.setVisibility(View.GONE);
                    binding.recyclerView.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                }
            });
        });
    }

    public static void syncRulesToSharedPreferences(Context context) {
        // Run database query on separate thread to prevent main thread blocking
        final Context appContext = context.getApplicationContext();
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(appContext);
            List<AutoReplyRule> rules = db.autoReplyRuleDao().getAll();
            JSONArray array = new JSONArray();

            for (AutoReplyRule rule : rules) {
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("id", rule.getId());
                    obj.put("keywords", rule.getKeywords());
                    obj.put("matchingType", rule.getMatchingType());
                    obj.put("replyText", rule.getReplyText());
                    obj.put("quoteOriginal", rule.getQuoteOriginal());
                    obj.put("delaySeconds", rule.getDelaySeconds());
                    obj.put("targetType", rule.getTargetType());
                    obj.put("activeHoursStart", rule.getActiveHoursStart());
                    obj.put("activeHoursEnd", rule.getActiveHoursEnd());
                    obj.put("isEnabled", rule.isEnabled());
                    obj.put("isForward", rule.isForward());
                    obj.put("forwardJid", rule.getForwardJid());
                    obj.put("isAi", rule.isAi());
                    array.put(obj);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
            prefs.edit().putString("auto_reply_rules", array.toString()).apply();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }

    private class AutoReplyRuleHolder extends RecyclerView.ViewHolder {
        TextView textTrigger, textReply, textTargetType, textActiveHours, textMatchingType;
        MaterialSwitch switchRuleEnabled;
        ImageButton btnDelete;

        public AutoReplyRuleHolder(@NonNull View itemView) {
            super(itemView);
            textTrigger = itemView.findViewById(R.id.text_trigger);
            textReply = itemView.findViewById(R.id.text_reply);
            textTargetType = itemView.findViewById(R.id.text_target_type);
            textActiveHours = itemView.findViewById(R.id.text_active_hours);
            textMatchingType = itemView.findViewById(R.id.text_matching_type);
            switchRuleEnabled = itemView.findViewById(R.id.switch_rule_enabled);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        public void bind(AutoReplyRule rule) {
            textTrigger.setText("Keyword: " + rule.getKeywords());
            textReply.setText("Reply: " + rule.getReplyText());
            textTargetType.setText("Target: " + rule.getTargetType());
            textMatchingType.setText("Matching: " + rule.getMatchingType());

            if (rule.getActiveHoursStart() != null && rule.getActiveHoursEnd() != null) {
                textActiveHours.setText("Hours: " + rule.getActiveHoursStart() + " - " + rule.getActiveHoursEnd());
            } else {
                textActiveHours.setText("Hours: 24/7");
            }

            switchRuleEnabled.setOnCheckedChangeListener(null);
            switchRuleEnabled.setChecked(rule.isEnabled());
            switchRuleEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                dbExecutor.execute(() -> {
                    AppDatabase db = AppDatabase.getInstance(itemView.getContext());
                    AutoReplyRule updatedRule = new AutoReplyRule(
                        rule.getId(),
                        rule.getKeywords(),
                        rule.getMatchingType(),
                        rule.getReplyText(),
                        rule.getQuoteOriginal(),
                        rule.getDelaySeconds(),
                        rule.getTargetType(),
                        rule.getActiveHoursStart(),
                        rule.getActiveHoursEnd(),
                        isChecked,
                        rule.isForward(),
                        rule.getForwardJid(),
                        rule.isAi()
                    );
                    db.autoReplyRuleDao().update(updatedRule);
                    syncRulesToSharedPreferences(itemView.getContext());
                    runOnUiThread(() -> loadAutoReplyRules());
                });
            });

            btnDelete.setOnClickListener(v -> {
                dbExecutor.execute(() -> {
                    AppDatabase db = AppDatabase.getInstance(itemView.getContext());
                    db.autoReplyRuleDao().delete(rule);
                    syncRulesToSharedPreferences(itemView.getContext());
                    runOnUiThread(() -> {
                        Toast.makeText(itemView.getContext(), "Rule deleted", Toast.LENGTH_SHORT).show();
                        loadAutoReplyRules();
                    });
                });
            });

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), EditAutoReplyRuleActivity.class);
                intent.putExtra("rule_id", rule.getId());
                itemView.getContext().startActivity(intent);
            });
        }
    }

    private class AutoReplyRuleAdapter extends RecyclerView.Adapter<AutoReplyRuleHolder> {

        @NonNull
        @Override
        public AutoReplyRuleHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_auto_reply_rule, parent, false);
            return new AutoReplyRuleHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AutoReplyRuleHolder holder, int position) {
            holder.bind(rulesList.get(position));
        }

        @Override
        public int getItemCount() {
            return rulesList.size();
        }
    }
}
