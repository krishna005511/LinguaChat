package com.nakama.linguachat.models.requests;
import com.google.gson.annotations.SerializedName;
public class ResendOtpRequest {
    @SerializedName("userId") private String userId;
    public ResendOtpRequest(String userId) { this.userId = userId; }
}
