package com.nakama.linguachat.models.responses;

import com.google.gson.annotations.SerializedName;
import com.nakama.linguachat.models.ChatRoom;
import java.util.List;

public class RoomsResponse {
    @SerializedName("rooms")
    private List<ChatRoom> rooms;
    public List<ChatRoom> getRooms() { return rooms; }
}
