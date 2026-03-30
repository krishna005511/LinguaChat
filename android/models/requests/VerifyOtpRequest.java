package com.nakama.linguachat.models.requests;
import com.google.gson.annotations.SerializedName;
public class VerifyOtpRequest {
    @SerializedName("userId") private String userId;
    @SerializedName("otp") private String otp;
    public VerifyOtpRequest(String userId, String otp) {
        this.userId = userId; this.otp = otp;
    }
}
