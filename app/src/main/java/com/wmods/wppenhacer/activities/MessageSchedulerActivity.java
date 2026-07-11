package com.wmods.wppenhacer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wmods.wppenhacer.R;
import com.wmods.wppenhacer.activities.base.BaseActivity;
import com.wmods.wppenhacer.database.AppDatabase;
import com.wmods.wppenhacer.database.SchedulerHelper;
import com.wmods.wppenhacer.database.ScheduledMessage;
import com.wmods.wppenhacer.databinding.ActivityMessageSchedulerBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageSchedulerActivity extends BaseActivity {

    private ActivityMessageSchedulerBinding binding;
    private ScheduledMessageAdapter adapter;
    private List<ScheduledMessage> messagesList = new ArrayList<>();
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMessageSchedulerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScheduledMessageAdapter();
        binding.recyclerView.setAdapter(adapter);

        binding.fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditScheduledMessageActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadScheduledMessages();
    }

    private void loadScheduledMessages() {
        dbExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            final List<ScheduledMessage> list = db.scheduledMessageDao().getAll();
            runOnUiThread(() -> {
                messagesList = list;
                if (messagesList.isEmpty()) {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }

    private class ScheduledMessageHolder extends RecyclerView.ViewHolder {
        TextView textContactName, textStatus, textMessage, textTime, textMediaPath;
        ImageView imageMediaType, imageRecurring;
        ImageButton btnDelete;
        LinearLayout layoutMedia;

        public ScheduledMessageHolder(@NonNull View itemView) {
            super(itemView);
            textContactName = itemView.findViewById(R.id.text_contact_name);
            textStatus = itemView.findViewById(R.id.text_status);
            textMessage = itemView.findViewById(R.id.text_message);
            textTime = itemView.findViewById(R.id.text_time);
            textMediaPath = itemView.findViewById(R.id.text_media_path);
            imageMediaType = itemView.findViewById(R.id.image_media_type);
            imageRecurring = itemView.findViewById(R.id.image_recurring);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            layoutMedia = itemView.findViewById(R.id.layout_media);
        }

        public void bind(ScheduledMessage message) {
            textContactName.setText(message.getContactName());
            textStatus.setText(message.getStatus());

            if (message.getStatus().equals("PENDING")) {
                textStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else if (message.getStatus().equals("SENT")) {
                textStatus.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            } else {
                textStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }

            textMessage.setText(message.getMessageText());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy, HH:mm", Locale.getDefault());
            textTime.setText(sdf.format(new Date(message.getScheduledTime())));

            if (message.isRecurring()) {
                imageRecurring.setVisibility(View.VISIBLE);
            } else {
                imageRecurring.setVisibility(View.GONE);
            }

            if (message.getMediaPath() != null && !message.getMediaPath().isEmpty()) {
                layoutMedia.setVisibility(View.VISIBLE);
                textMediaPath.setText(message.getMediaPath());
                if ("VIDEO".equals(message.getMediaType())) {
                    imageMediaType.setImageResource(android.R.drawable.presence_video_busy);
                } else if ("DOCUMENT".equals(message.getMediaType())) {
                    imageMediaType.setImageResource(android.R.drawable.ic_menu_save);
                } else if ("AUDIO".equals(message.getMediaType())) {
                    imageMediaType.setImageResource(android.R.drawable.ic_media_play);
                } else {
                    imageMediaType.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            } else {
                layoutMedia.setVisibility(View.GONE);
            }

            btnDelete.setOnClickListener(v -> {
                dbExecutor.execute(() -> {
                    AppDatabase db = AppDatabase.getInstance(itemView.getContext());
                    db.scheduledMessageDao().delete(message);
                    SchedulerHelper.INSTANCE.scheduleNextAlarm(itemView.getContext());
                    runOnUiThread(() -> {
                        Toast.makeText(itemView.getContext(), "Schedule deleted", Toast.LENGTH_SHORT).show();
                        loadScheduledMessages();
                    });
                });
            });

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), EditScheduledMessageActivity.class);
                intent.putExtra("message_id", message.getId());
                itemView.getContext().startActivity(intent);
            });
        }
    }

    private class ScheduledMessageAdapter extends RecyclerView.Adapter<ScheduledMessageHolder> {

        @NonNull
        @Override
        public ScheduledMessageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scheduled_message, parent, false);
            return new ScheduledMessageHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ScheduledMessageHolder holder, int position) {
            holder.bind(messagesList.get(position));
        }

        @Override
        public int getItemCount() {
            return messagesList.size();
        }
    }
}
