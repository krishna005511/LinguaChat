package com.nakama.linguachat.adapters;

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
import com.nakama.linguachat.models.ChatRoom;

import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.VH> {

    public interface OnRoomClickListener {
        void onRoomClick(ChatRoom room);
    }

    private final List<ChatRoom> rooms;
    private final String myUserId;
    private final OnRoomClickListener listener;

    public ChatListAdapter(List<ChatRoom> rooms, String myUserId, OnRoomClickListener listener) {
        this.rooms = rooms;
        this.myUserId = myUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_room, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ChatRoom room = rooms.get(pos);
        h.tvName.setText(room.getDisplayName(myUserId));

        // Last message preview
        ChatMessage last = room.getLastMessage();
        if (last != null && !last.isDeleted()) {
            switch (last.getType()) {
                case "image": h.tvLastMsg.setText("📷 Photo"); break;
                case "voice": h.tvLastMsg.setText("🎤 Voice message"); break;
                default:
                    String preview = last.getText();
                    h.tvLastMsg.setText(preview.length() > 50
                            ? preview.substring(0, 50) + "…" : preview);
            }
        } else {
            h.tvLastMsg.setText("No messages yet");
        }

        // Online dot (direct chats only)
        boolean online = "direct".equals(room.getType()) && room.isOnline(myUserId);
        h.viewOnlineDot.setVisibility(online ? View.VISIBLE : View.GONE);

        // Avatar
        String avatarUrl = room.getAvatarUrl(myUserId);
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(h.ivAvatar.getContext())
                    .load(avatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_default_avatar)
                    .into(h.ivAvatar);
        } else {
            h.ivAvatar.setImageResource(R.drawable.ic_default_avatar);
        }

        h.itemView.setOnClickListener(v -> listener.onRoomClick(room));
    }

    @Override
    public int getItemCount() { return rooms.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName, tvLastMsg;
        View viewOnlineDot;

        VH(View v) {
            super(v);
            ivAvatar     = v.findViewById(R.id.ivAvatar);
            tvName       = v.findViewById(R.id.tvName);
            tvLastMsg    = v.findViewById(R.id.tvLastMsg);
            viewOnlineDot = v.findViewById(R.id.viewOnlineDot);
        }
    }
}
