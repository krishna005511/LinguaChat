package com.nakama.linguachat.models.responses;
import com.google.gson.annotations.SerializedName;
public class RegisterResponse {
    @SerializedName("message") private String message;
    @SerializedName("userId") private String userId;
    public String getMessage() { return message; }
    public String getUserId() { return userId; }
}
