package com.rhdevs.rhpatch.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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

import com.rhdevs.rhpatch.R;
import com.rhdevs.rhpatch.activities.base.BaseActivity;
import com.rhdevs.rhpatch.databinding.ActivityCustomFoldersBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CustomFoldersActivity extends BaseActivity {

    private ActivityCustomFoldersBinding binding;
    private FolderAdapter adapter;
    private List<JSONObject> foldersList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomFoldersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FolderAdapter();
        binding.recyclerView.setAdapter(adapter);

        binding.fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditFolderActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFolders();
    }

    private void loadFolders() {
        foldersList.clear();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String foldersJson = prefs.getString("custom_folders", "[]");
        try {
            JSONArray array = new JSONArray(foldersJson);
            for (int i = 0; i < array.length(); i++) {
                foldersList.add(array.getJSONObject(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (foldersList.isEmpty()) {
            binding.emptyView.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
        } else {
            binding.emptyView.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    private void deleteFolder(int index) {
        foldersList.remove(index);
        JSONArray array = new JSONArray();
        for (JSONObject folder : foldersList) {
            array.put(folder);
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putString("custom_folders", array.toString()).apply();
        Toast.makeText(this, "Folder deleted", Toast.LENGTH_SHORT).show();
        loadFolders();
    }

    private class FolderHolder extends RecyclerView.ViewHolder {
        View viewFolderColor;
        TextView textFolderName, textFolderSummary;
        ImageButton btnDelete;

        public FolderHolder(@NonNull View itemView) {
            super(itemView);
            viewFolderColor = itemView.findViewById(R.id.view_folder_color);
            textFolderName = itemView.findViewById(R.id.text_folder_name);
            textFolderSummary = itemView.findViewById(R.id.text_folder_summary);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        public void bind(JSONObject folder, int position) {
            String name = folder.optString("name", "");
            String colorStr = folder.optString("color", "#ff4faf50");
            String contacts = folder.optString("contacts", "");
            int contactsCount = contacts.isEmpty() ? 0 : contacts.split(",").length;

            textFolderName.setText(name);
            textFolderSummary.setText(contactsCount + " chat(s) categorized");

            try {
                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.OVAL);
                drawable.setColor(Color.parseColor(colorStr));
                viewFolderColor.setBackground(drawable);
            } catch (Exception e) {
                e.printStackTrace();
            }

            btnDelete.setOnClickListener(v -> deleteFolder(position));

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), EditFolderActivity.class);
                intent.putExtra("folder_index", position);
                itemView.getContext().startActivity(intent);
            });
        }
    }

    private class FolderAdapter extends RecyclerView.Adapter<FolderHolder> {

        @NonNull
        @Override
        public FolderHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder_row, parent, false);
            return new FolderHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FolderHolder holder, int position) {
            holder.bind(foldersList.get(position), position);
        }

        @Override
        public int getItemCount() {
            return foldersList.size();
        }
    }
}
