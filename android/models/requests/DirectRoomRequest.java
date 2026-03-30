package com.nakama.linguachat.models.requests;

import com.google.gson.annotations.SerializedName;

public class DirectRoomRequest {
    @SerializedName("targetUserId")
    private String targetUserId;
    public DirectRoomRequest(String targetUserId) { this.targetUserId = targetUserId; }
}
