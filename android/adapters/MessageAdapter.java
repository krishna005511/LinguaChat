package com.nakama.linguachat.adapters;

import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nakama.linguachat.R;
import com.nakama.linguachat.models.ChatMessage;

import java.io.IOException;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_SENT     = 1;
    private static final int VIEW_RECEIVED = 2;

    private final List<ChatMessage> messages;
    private final String myUserId;
    private final String preferredLang;
    private MediaPlayer currentPlayer;

    public MessageAdapter(List<ChatMessage> messages, String myUserId, String preferredLang) {
        this.messages = messages;
        this.myUserId = myUserId;
        this.preferredLang = preferredLang;
    }

    @Override
    public int getItemViewType(int pos) {
        ChatMessage m = messages.get(pos);
        return (m.getSender() != null && myUserId.equals(m.getSender().getId()))
                ? VIEW_SENT : VIEW_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_SENT) {
            return new MsgVH(inf.inflate(R.layout.item_message_sent, parent, false));
        } else {
            return new MsgVH(inf.inflate(R.layout.item_message_received, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
        ChatMessage msg = messages.get(pos);
        MsgVH h = (MsgVH) holder;
        boolean isSent = getItemViewType(pos) == VIEW_SENT;

        // Sender name (received messages only)
        if (h.tvSenderName != null) {
            if (msg.getSender() != null) {
                h.tvSenderName.setText(msg.getSender().getDisplayName());
                h.tvSenderName.setVisibility(View.VISIBLE);
            } else {
                h.tvSenderName.setVisibility(View.GONE);
            }
        }

        switch (msg.getType()) {
            case "image":
                h.tvMessage.setVisibility(View.GONE);
                h.ivMedia.setVisibility(View.VISIBLE);
                h.btnPlayVoice.setVisibility(View.GONE);
                if (msg.getMediaUrl() != null) {
                    Glide.with(h.ivMedia.getContext())
                            .load(msg.getMediaUrl())
                            .placeholder(R.drawable.ic_image_placeholder)
                            .into(h.ivMedia);
                }
                break;

            case "voice":
                h.tvMessage.setVisibility(View.GONE);
                h.ivMedia.setVisibility(View.GONE);
                h.btnPlayVoice.setVisibility(View.VISIBLE);
                h.btnPlayVoice.setOnClickListener(v -> playVoice(msg.getMediaUrl(), h.btnPlayVoice));
                break;

            default: // text
                h.tvMessage.setVisibility(View.VISIBLE);
                h.ivMedia.setVisibility(View.GONE);
                h.btnPlayVoice.setVisibility(View.GONE);
                // Show translated text if available
                h.tvMessage.setText(msg.getTextForLang(preferredLang));

                // Show original language tag if different
                if (h.tvOriginalLang != null
                        && msg.getOriginalLang() != null
                        && !msg.getOriginalLang().equals(preferredLang)) {
                    h.tvOriginalLang.setText("translated from " + msg.getOriginalLang());
                    h.tvOriginalLang.setVisibility(View.VISIBLE);
                } else if (h.tvOriginalLang != null) {
                    h.tvOriginalLang.setVisibility(View.GONE);
                }
        }

        // Timestamp
        if (h.tvTime != null && msg.getCreatedAt() != null) {
            h.tvTime.setText(formatTime(msg.getCreatedAt()));
        }

        // Seen / delivered ticks (sent messages only)
        if (isSent && h.tvTicks != null) {
            boolean seen = msg.getSeenBy() != null && !msg.getSeenBy().isEmpty();
            boolean delivered = msg.getDeliveredTo() != null && msg.getDeliveredTo().size() > 1;
            if (seen) {
                h.tvTicks.setText("✓✓");
                h.tvTicks.setTextColor(0xFF1A73E8); // blue
            } else if (delivered) {
                h.tvTicks.setText("✓✓");
                h.tvTicks.setTextColor(0xFF9E9E9E); // grey
            } else {
                h.tvTicks.setText("✓");
                h.tvTicks.setTextColor(0xFF9E9E9E);
            }
            h.tvTicks.setVisibility(View.VISIBLE);
        }
    }

    private void playVoice(String url, ImageView btn) {
        if (currentPlayer != null) {
            currentPlayer.stop();
            currentPlayer.release();
            currentPlayer = null;
        }
        if (url == null) return;
        currentPlayer = new MediaPlayer();
        try {
            currentPlayer.setDataSource(url);
            currentPlayer.prepareAsync();
            currentPlayer.setOnPreparedListener(mp -> {
                mp.start();
                btn.setImageResource(android.R.drawable.ic_media_pause);
            });
            currentPlayer.setOnCompletionListener(mp -> {
                btn.setImageResource(android.R.drawable.ic_media_play);
                mp.release();
                currentPlayer = null;
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String formatTime(String iso) {
        try {
            // Simple HH:mm from ISO string "2024-01-01T14:30:00.000Z"
            if (iso.length() >= 16) return iso.substring(11, 16);
        } catch (Exception ignored) {}
        return "";
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class MsgVH extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime, tvTicks, tvSenderName, tvOriginalLang;
        ImageView ivMedia, btnPlayVoice;

        MsgVH(View v) {
            super(v);
            tvMessage    = v.findViewById(R.id.tvMessage);
            tvTime       = v.findViewById(R.id.tvTime);
            tvTicks      = v.findViewById(R.id.tvTicks);
            tvSenderName = v.findViewById(R.id.tvSenderName);
            tvOriginalLang = v.findViewById(R.id.tvOriginalLang);
            ivMedia      = v.findViewById(R.id.ivMedia);
            btnPlayVoice = v.findViewById(R.id.btnPlayVoice);
        }
    }
}
