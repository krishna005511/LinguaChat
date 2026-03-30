package com.nakama.linguachat.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ChatMessage {

    @SerializedName("_id")
    private String id;

    @SerializedName("roomId")
    private String roomId;

    @SerializedName("sender")
    private User sender;

    @SerializedName("type")
    private String type;   // "text", "image", "voice", "system"

    @SerializedName("text")
    private String text;

    @SerializedName("originalLang")
    private String originalLang;

    @SerializedName("translations")
    private List<Translation> translations;

    @SerializedName("mediaUrl")
    private String mediaUrl;

    @SerializedName("mediaDuration")
    private Double mediaDuration;

    @SerializedName("deliveredTo")
    private List<String> deliveredTo;

    @SerializedName("seenBy")
    private List<SeenEntry> seenBy;

    @SerializedName("isDeleted")
    private boolean isDeleted;

    @SerializedName("createdAt")
    private String createdAt;

    // ── Inner classes ─────────────────────────────────────────────────────────

    public static class Translation {
        @SerializedName("lang")
        public String lang;
        @SerializedName("text")
        public String text;
    }

    public static class SeenEntry {
        @SerializedName("userId")
        public String userId;
        @SerializedName("seenAt")
        public String seenAt;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns translated text for the given language, falls back to original */
    public String getTextForLang(String lang) {
        if (lang == null || lang.equals(originalLang) || translations == null) return text;
        for (Translation t : translations) {
            if (lang.equals(t.lang)) return t.text;
        }
        return text;
    }

    public boolean isSeenBy(String userId) {
        if (seenBy == null) return false;
        for (SeenEntry s : seenBy) {
            if (userId.equals(s.userId)) return true;
        }
        return false;
    }

    public boolean isDeliveredTo(String userId) {
        return deliveredTo != null && deliveredTo.contains(userId);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getId() { return id; }
    public String getRoomId() { return roomId; }
    public User getSender() { return sender; }
    public String getType() { return type != null ? type : "text"; }
    public String getText() { return text != null ? text : ""; }
    public String getOriginalLang() { return originalLang; }
    public List<Translation> getTranslations() { return translations; }
    public String getMediaUrl() { return mediaUrl; }
    public Double getMediaDuration() { return mediaDuration; }
    public boolean isDeleted() { return isDeleted; }
    public String getCreatedAt() { return createdAt; }
    public List<String> getDeliveredTo() { return deliveredTo; }
    public List<SeenEntry> getSeenBy() { return seenBy; }
}
