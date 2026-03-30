package com.nakama.linguachat.models.responses;

import com.google.gson.annotations.SerializedName;
import com.nakama.linguachat.models.ChatMessage;
import com.nakama.linguachat.models.ChatRoom;
import java.util.List;

public class MessagesResponse {
    @SerializedName("messages")
    private List<ChatMessage> messages;
    public List<ChatMessage> getMessages() { return messages; }
}
