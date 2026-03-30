package com.nakama.linguachat.models.responses;
import com.google.gson.annotations.SerializedName;
import com.nakama.linguachat.models.User;
import java.util.List;
public class UsersResponse {
    @SerializedName("users") private List<User> users;
    public List<User> getUsers() { return users; }
}
