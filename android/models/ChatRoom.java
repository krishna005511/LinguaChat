package com.nakama.linguachat.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ChatRoom {

    @SerializedName("_id")
    private String id;

    @SerializedName("type")
    private String type;   // "direct" or "group"

    @SerializedName("name")
    private String name;

    @SerializedName("groupAvatar")
    private String groupAvatar;

    @SerializedName("members")
    private List<User> members;

    @SerializedName("lastMessage")
    private ChatMessage lastMessage;

    @SerializedName("lastActivity")
    private String lastActivity;

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * For direct chats, return the other participant (not the current user).
     */
    public User getOtherMember(String myUserId) {
        if (members == null) return null;
        for (User m : members) {
            if (!m.getId().equals(myUserId)) return m;
        }
        return null;
    }

    /**
     * Display name: for group = group name; for direct = the other user's displayName.
     */
    public String getDisplayName(String myUserId) {
        if ("group".equals(type)) return name != null ? name : "Group";
        User other = getOtherMember(myUserId);
        return other != null ? other.getDisplayName() : "Unknown";
    }

    public String getAvatarUrl(String myUserId) {
        if ("group".equals(type)) return groupAvatar;
        User other = getOtherMember(myUserId);
        return other != null ? other.getAvatarUrl() : null;
    }

    public boolean isOnline(String myUserId) {
        User other = getOtherMember(myUserId);
        return other != null && other.isOnline();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getId() { return id; }
    public String getType() { return type; }
    public String getName() { return name; }
    public String getGroupAvatar() { return groupAvatar; }
    public List<User> getMembers() { return members; }
    public ChatMessage getLastMessage() { return lastMessage; }
    public String getLastActivity() { return lastActivity; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setLastMessage(ChatMessage msg) { this.lastMessage = msg; }
    public void setLastActivity(String t) { this.lastActivity = t; }
    public void setOnlineStatus(String userId, boolean online) {
        if (members == null) return;
        for (User m : members) {
            if (m.getId().equals(userId)) { m.setOnline(online); break; }
        }
    }
}
