package com.nakama.linguachat.models;

import com.google.gson.annotations.SerializedName;

public class User {

    @SerializedName("id")
    private String id;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("displayName")
    private String displayName;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("preferredLanguage")
    private String preferredLanguage;

    @SerializedName("bio")
    private String bio;

    @SerializedName("isVerified")
    private boolean isVerified;

    @SerializedName("isOnline")
    private boolean isOnline;

    @SerializedName("lastSeen")
    private String lastSeen;

    // Constructors
    public User() {}

    // Getters
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName != null ? displayName : username; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getPreferredLanguage() { return preferredLanguage != null ? preferredLanguage : "en"; }
    public String getBio() { return bio != null ? bio : ""; }
    public boolean isVerified() { return isVerified; }
    public boolean isOnline() { return isOnline; }
    public String getLastSeen() { return lastSeen; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }
    public void setBio(String bio) { this.bio = bio; }
    public void setVerified(boolean verified) { isVerified = verified; }
    public void setOnline(boolean online) { isOnline = online; }
    public void setLastSeen(String lastSeen) { this.lastSeen = lastSeen; }
}
