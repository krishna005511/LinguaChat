package com.nakama.linguachat.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.nakama.linguachat.adapters.MessageAdapter;
import com.nakama.linguachat.databinding.ActivityChatBinding;
import com.nakama.linguachat.models.ChatMessage;
import com.nakama.linguachat.models.responses.MessagesResponse;
import com.nakama.linguachat.network.RetrofitClient;
import com.nakama.linguachat.socket.SocketManager;
import com.nakama.linguachat.utils.SessionManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity
        implements SocketManager.MessageListener,
                   SocketManager.TypingListener,
                   SocketManager.SeenListener {

    public static final String EXTRA_ROOM_ID   = "room_id";
    public static final String EXTRA_ROOM_NAME = "room_name";
    public static final String EXTRA_ROOM_TYPE = "room_type";

    private ActivityChatBinding binding;
    private SessionManager sessionManager;
    private MessageAdapter adapter;
    private List<ChatMessage> messages = new ArrayList<>();

    private String roomId;
    private String roomName;

    private MediaRecorder mediaRecorder;
    private File audioFile;
    private boolean isRecording = false;

    private Handler typingHandler = new Handler(Looper.getMainLooper());
    private Runnable typingStopRunnable;
    private boolean isTyping = false;

    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        roomId   = getIntent().getStringExtra(EXTRA_ROOM_ID);
        roomName = getIntent().getStringExtra(EXTRA_ROOM_NAME);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(roomName);
        }

        sessionManager = SessionManager.getInstance(this);

        // RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        adapter = new MessageAdapter(messages, sessionManager.getUserId(),
                sessionManager.getPreferredLanguage());
        binding.rvMessages.setLayoutManager(layoutManager);
        binding.rvMessages.setAdapter(adapter);

        // Image picker
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) uploadImage(uri);
                });

        // Socket
        SocketManager sm = SocketManager.getInstance(this);
        sm.addMessageListener(this);
        sm.addTypingListener(this);
        sm.addSeenListener(this);
        sm.joinRoom(roomId);

        setupInput();
        loadMessages(null);
    }

    private void setupInput() {
        // Text input + typing indicator
        binding.etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int i, int b, int c) {
                boolean hasText = s.toString().trim().length() > 0;
                binding.btnSend.setVisibility(hasText ? View.VISIBLE : View.GONE);
                binding.btnVoice.setVisibility(hasText ? View.GONE : View.VISIBLE);

                if (!isTyping && hasText) {
                    isTyping = true;
                    SocketManager.getInstance(ChatActivity.this).typingStart(roomId);
                }
                if (typingStopRunnable != null) typingHandler.removeCallbacks(typingStopRunnable);
                typingStopRunnable = () -> {
                    isTyping = false;
                    SocketManager.getInstance(ChatActivity.this).typingStop(roomId);
                };
                typingHandler.postDelayed(typingStopRunnable, 2000);
            }
        });

        // Send text
        binding.btnSend.setOnClickListener(v -> {
            String text = binding.etMessage.getText().toString().trim();
            if (text.isEmpty()) return;
            binding.etMessage.setText("");
            SocketManager.getInstance(this).sendTextMessage(roomId, text);
        });

        // Attach image
        binding.btnAttach.setOnClickListener(v ->
                imagePickerLauncher.launch("image/*"));

        // Voice record (hold to record)
        binding.btnVoice.setOnLongClickListener(v -> {
            startRecording();
            return true;
        });
        binding.btnVoice.setOnClickListener(v -> {
            if (isRecording) stopRecordingAndSend();
        });

        // Load more on scroll to top
        binding.rvMessages.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(androidx.recyclerview.widget.RecyclerView rv, int dx, int dy) {
                if (!rv.canScrollVertically(-1) && messages.size() >= 40) {
                    loadMessages(messages.get(0).getId());
                }
            }
        });
    }

    private void loadMessages(String before) {
        binding.progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getInstance().getApiService()
                .getMessages(sessionManager.getBearerToken(), roomId, 40, before)
                .enqueue(new Callback<MessagesResponse>() {
                    @Override
                    public void onResponse(Call<MessagesResponse> c, Response<MessagesResponse> r) {
                        binding.progressBar.setVisibility(View.GONE);
                        if (r.isSuccessful() && r.body() != null) {
                            List<ChatMessage> loaded = r.body().getMessages();
                            if (before == null) {
                                messages.clear();
                                messages.addAll(loaded);
                                adapter.notifyDataSetChanged();
                                scrollToBottom();
                                // Mark room as seen
                                RetrofitClient.getInstance().getApiService()
                                        .markRoomSeen(sessionManager.getBearerToken(), roomId)
                                        .enqueue(new Callback<com.nakama.linguachat.models.responses.MessageResponse>() {
                                            @Override public void onResponse(Call<com.nakama.linguachat.models.responses.MessageResponse> c2, Response<com.nakama.linguachat.models.responses.MessageResponse> r2) {}
                                            @Override public void onFailure(Call<com.nakama.linguachat.models.responses.MessageResponse> c2, Throwable t) {}
                                        });
                            } else {
                                // Prepend older messages
                                messages.addAll(0, loaded);
                                adapter.notifyItemRangeInserted(0, loaded.size());
                                binding.rvMessages.scrollToPosition(loaded.size());
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<MessagesResponse> c, Throwable t) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(ChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── Socket listeners ──────────────────────────────────────────────────────

    @Override
    public void onNewMessage(ChatMessage msg) {
        if (!msg.getRoomId().equals(roomId)) return;
        runOnUiThread(() -> {
            messages.add(msg);
            adapter.notifyItemInserted(messages.size() - 1);
            scrollToBottom();
            // Emit seen
            if (!msg.getSender().getId().equals(sessionManager.getUserId())) {
                SocketManager.getInstance(this).markSeen(roomId, msg.getId());
            }
        });
    }

    @Override
    public void onTypingStart(String rId, String username) {
        if (!rId.equals(roomId)) return;
        runOnUiThread(() -> {
            binding.tvTyping.setText(username + " is typing…");
            binding.tvTyping.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onTypingStop(String rId, String userId) {
        if (!rId.equals(roomId)) return;
        runOnUiThread(() -> binding.tvTyping.setVisibility(View.GONE));
    }

    @Override
    public void onMessageSeen(String messageId, String seenBy) {
        runOnUiThread(() -> {
            for (int i = 0; i < messages.size(); i++) {
                if (messages.get(i).getId().equals(messageId)) {
                    adapter.notifyItemChanged(i);
                    break;
                }
            }
        });
    }

    @Override
    public void onMessageDelivered(String messageId, String deliveredTo) {
        runOnUiThread(() -> adapter.notifyDataSetChanged());
    }

    // ── Image upload ──────────────────────────────────────────────────────────

    private void uploadImage(Uri uri) {
        try {
            byte[] bytes;
            try (java.io.InputStream is = getContentResolver().openInputStream(uri)) {
                bytes = new byte[is.available()];
                is.read(bytes);
            }
            String base64 = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
            uploadMedia(base64, "image");
        } catch (Exception e) {
            Toast.makeText(this, "Failed to read image", Toast.LENGTH_SHORT).show();
        }
    }

    // ── Voice recording ───────────────────────────────────────────────────────

    private void startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 101);
            return;
        }
        try {
            audioFile = new File(getCacheDir(), "voice_" + System.currentTimeMillis() + ".3gp");
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            binding.btnVoice.setImageResource(android.R.drawable.ic_media_pause);
            Toast.makeText(this, "Recording… tap to stop", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Could not start recording", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecordingAndSend() {
        if (!isRecording) return;
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            binding.btnVoice.setImageResource(android.R.drawable.ic_btn_speak_now);

            // Convert to base64 and upload
            FileInputStream fis = new FileInputStream(audioFile);
            byte[] bytes = new byte[(int) audioFile.length()];
            fis.read(bytes);
            fis.close();
            String base64 = "data:audio/3gpp;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
            uploadMedia(base64, "voice");
        } catch (Exception e) {
            Toast.makeText(this, "Recording failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadMedia(String base64Data, String type) {
        Map<String, String> body = new HashMap<>();
        body.put("data", base64Data);
        body.put("type", type);

        RetrofitClient.getInstance().getApiService()
                .uploadMedia(sessionManager.getBearerToken(), body)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> c, Response<Map<String, Object>> r) {
                        if (r.isSuccessful() && r.body() != null) {
                            String url = (String) r.body().get("url");
                            Object dur = r.body().get("duration");
                            Double duration = dur instanceof Double ? (Double) dur : null;
                            SocketManager.getInstance(ChatActivity.this)
                                    .sendMessage(roomId, null, type, url, duration, null);
                        } else {
                            Toast.makeText(ChatActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<Map<String, Object>> c, Throwable t) {
                        Toast.makeText(ChatActivity.this, "Upload error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void scrollToBottom() {
        if (!messages.isEmpty())
            binding.rvMessages.smoothScrollToPosition(messages.size() - 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (typingStopRunnable != null) typingHandler.removeCallbacks(typingStopRunnable);
        SocketManager sm = SocketManager.getInstance(this);
        sm.removeMessageListener(this);
        sm.removeTypingListener(this);
        sm.removeSeenListener(this);
        if (mediaRecorder != null) { mediaRecorder.release(); mediaRecorder = null; }
    }

    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
