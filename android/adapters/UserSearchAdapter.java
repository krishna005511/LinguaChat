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
import com.nakama.linguachat.models.User;

import java.util.List;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.VH> {

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    private final List<User> users;
    private final OnUserClickListener listener;

    public UserSearchAdapter(List<User> users, OnUserClickListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_search, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        User user = users.get(pos);
        h.tvDisplayName.setText(user.getDisplayName());
        h.tvUsername.setText("@" + user.getUsername());
        if (user.getBio() != null && !user.getBio().isEmpty()) {
            h.tvBio.setText(user.getBio());
            h.tvBio.setVisibility(View.VISIBLE);
        } else {
            h.tvBio.setVisibility(View.GONE);
        }
        // Online dot
        h.viewOnlineDot.setVisibility(user.isOnline() ? View.VISIBLE : View.GONE);

        // Avatar
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(h.ivAvatar.getContext())
                    .load(user.getAvatarUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_default_avatar)
                    .into(h.ivAvatar);
        } else {
            h.ivAvatar.setImageResource(R.drawable.ic_default_avatar);
        }

        h.itemView.setOnClickListener(v -> listener.onUserClick(user));
    }

    @Override
    public int getItemCount() { return users.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvDisplayName, tvUsername, tvBio;
        View viewOnlineDot;

        VH(View v) {
            super(v);
            ivAvatar      = v.findViewById(R.id.ivAvatar);
            tvDisplayName = v.findViewById(R.id.tvDisplayName);
            tvUsername    = v.findViewById(R.id.tvUsername);
            tvBio         = v.findViewById(R.id.tvBio);
            viewOnlineDot = v.findViewById(R.id.viewOnlineDot);
        }
    }
}
