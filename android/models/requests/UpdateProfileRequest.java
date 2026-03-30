package com.nakama.linguachat.models.requests;
import com.google.gson.annotations.SerializedName;
public class UpdateProfileRequest {
    @SerializedName("displayName") private String displayName;
    @SerializedName("bio") private String bio;
    @SerializedName("preferredLanguage") private String preferredLanguage;
    @SerializedName("avatarUrl") private String avatarUrl;
    public UpdateProfileRequest(String displayName, String bio, String preferredLanguage, String avatarUrl) {
        this.displayName = displayName; this.bio = bio;
        this.preferredLanguage = preferredLanguage; this.avatarUrl = avatarUrl;
    }
}
