package com.nakama.linguachat.models.responses;
import com.google.gson.annotations.SerializedName;
import com.nakama.linguachat.models.User;
public class AuthResponse {
    @SerializedName("message") private String message;
    @SerializedName("token") private String token;
    @SerializedName("user") private User user;
    @SerializedName("userId") private String userId;
    @SerializedName("needsVerification") private boolean needsVerification;
    public String getMessage() { return message; }
    public String getToken() { return token; }
    public User getUser() { return user; }
    public String getUserId() { return userId; }
    public boolean isNeedsVerification() { return needsVerification; }
}
