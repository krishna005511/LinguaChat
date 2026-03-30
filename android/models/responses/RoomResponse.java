package com.nakama.linguachat.models.responses;

import com.google.gson.annotations.SerializedName;
import com.nakama.linguachat.models.ChatRoom;

public class RoomResponse {
    @SerializedName("room")
    private ChatRoom room;
    public ChatRoom getRoom() { return room; }
}
