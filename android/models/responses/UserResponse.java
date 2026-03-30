package com.nakama.linguachat.models.responses;
import com.google.gson.annotations.SerializedName;
import com.nakama.linguachat.models.User;
public class UserResponse {
    @SerializedName("user") private User user;
    @SerializedName("message") private String message;
    public User getUser() { return user; }
    public String getMessage() { return message; }
}
