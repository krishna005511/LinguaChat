package com.nakama.linguachat.models.requests;
import com.google.gson.annotations.SerializedName;
public class RegisterRequest {
    @SerializedName("username") private String username;
    @SerializedName("email") private String email;
    @SerializedName("password") private String password;
    @SerializedName("displayName") private String displayName;
    public RegisterRequest(String username, String email, String password, String displayName) {
        this.username = username; this.email = email;
        this.password = password; this.displayName = displayName;
    }
}
