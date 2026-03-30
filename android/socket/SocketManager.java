package com.nakama.linguachat.socket;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.nakama.linguachat.BuildConfig;
import com.nakama.linguachat.models.ChatMessage;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketManager {

    private static final String TAG = "SocketManager";
    private static SocketManager instance;
    private Socket socket;
    private final Gson gson = new Gson();

    // Listeners that Activities register to receive events
    public interface MessageListener {
        void onNewMessage(ChatMessage message);
    }
    public interface TypingListener {
        void onTypingStart(String roomId, String username);
        void onTypingStop(String roomId, String userId);
    }
    public interface SeenListener {
        void onMessageSeen(String messageId, String seenBy);
        void onMessageDelivered(String messageId, String deliveredTo);
    }
    public interface OnlineListener {
        void onUserOnline(String userId);
        void onUserOffline(String userId, String lastSeen);
    }

    private final CopyOnWriteArrayList<MessageListener> messageListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<TypingListener> typingListeners   = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<SeenListener> seenListeners       = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<OnlineListener> onlineListeners   = new CopyOnWriteArrayList<>();

    private SocketManager() {}

    public static synchronized SocketManager getInstance(Context ctx) {
        if (instance == null) instance = new SocketManager();
        return instance;
    }

    // ── Connect ───────────────────────────────────────────────────────────────

    public void connect(String jwtToken) {
        if (socket != null && socket.connected()) return;
        try {
            IO.Options opts = new IO.Options();
            Map<String, String> auth = new HashMap<>();
            auth.put("token", jwtToken);
            opts.auth = auth;
            opts.reconnection = true;
            opts.reconnectionAttempts = 10;
            opts.reconnectionDelay = 2000;

            socket = IO.socket(BuildConfig.SOCKET_URL, opts);
            registerListeners();
            socket.connect();
        } catch (Exception e) {
            Log.e(TAG, "Connect error: " + e.getMessage());
        }
    }

    private void registerListeners() {
        socket.on(Socket.EVENT_CONNECT, a ->
                Log.d(TAG, "Connected: " + socket.id()));

        socket.on(Socket.EVENT_DISCONNECT, a ->
                Log.d(TAG, "Disconnected"));

        socket.on(Socket.EVENT_CONNECT_ERROR, a ->
                Log.e(TAG, "Connect error: " + (a.length > 0 ? a[0] : "")));

        // New message
        socket.on("new_message", args -> {
            if (args.length == 0) return;
            try {
                ChatMessage msg = gson.fromJson(args[0].toString(), ChatMessage.class);
                for (MessageListener l : messageListeners) l.onNewMessage(msg);
            } catch (Exception e) {
                Log.e(TAG, "new_message parse error", e);
            }
        });

        // Typing
        socket.on("user_typing", args -> {
            if (args.length == 0) return;
            try {
                JSONObject d = new JSONObject(args[0].toString());
                String roomId = d.getString("roomId");
                String username = d.getString("username");
                for (TypingListener l : typingListeners) l.onTypingStart(roomId, username);
            } catch (Exception e) { Log.e(TAG, "typing parse error", e); }
        });

        socket.on("user_stopped_typing", args -> {
            if (args.length == 0) return;
            try {
                JSONObject d = new JSONObject(args[0].toString());
                String roomId = d.getString("roomId");
                String userId = d.getString("userId");
                for (TypingListener l : typingListeners) l.onTypingStop(roomId, userId);
            } catch (Exception e) { Log.e(TAG, "stop typing parse error", e); }
        });

        // Seen / delivered
        socket.on("message_seen", args -> {
            if (args.length == 0) return;
            try {
                JSONObject d = new JSONObject(args[0].toString());
                String messageId = d.getString("messageId");
                String seenBy = d.getString("seenBy");
                for (SeenListener l : seenListeners) l.onMessageSeen(messageId, seenBy);
            } catch (Exception e) { Log.e(TAG, "message_seen error", e); }
        });

        socket.on("message_delivered", args -> {
            if (args.length == 0) return;
            try {
                JSONObject d = new JSONObject(args[0].toString());
                String messageId = d.getString("messageId");
                String deliveredTo = d.getString("deliveredTo");
                for (SeenListener l : seenListeners) l.onMessageDelivered(messageId, deliveredTo);
            } catch (Exception e) { Log.e(TAG, "message_delivered error", e); }
        });

        // Online / offline
        socket.on("user_online", args -> {
            if (args.length == 0) return;
            try {
                JSONObject d = new JSONObject(args[0].toString());
                String userId = d.getString("userId");
                for (OnlineListener l : onlineListeners) l.onUserOnline(userId);
            } catch (Exception e) { Log.e(TAG, "user_online error", e); }
        });

        socket.on("user_offline", args -> {
            if (args.length == 0) return;
            try {
                JSONObject d = new JSONObject(args[0].toString());
                String userId = d.getString("userId");
                String lastSeen = d.optString("lastSeen", "");
                for (OnlineListener l : onlineListeners) l.onUserOffline(userId, lastSeen);
            } catch (Exception e) { Log.e(TAG, "user_offline error", e); }
        });
    }

    // ── Emit helpers ──────────────────────────────────────────────────────────

    public void sendMessage(String roomId, String text, String type,
                            String mediaUrl, Double mediaDuration,
                            Ack ack) {
        if (!isConnected()) return;
        try {
            JSONObject data = new JSONObject();
            data.put("roomId", roomId);
            data.put("text", text != null ? text : "");
            data.put("type", type != null ? type : "text");
            if (mediaUrl != null) data.put("mediaUrl", mediaUrl);
            if (mediaDuration != null) data.put("mediaDuration", mediaDuration);
            socket.emit("send_message", data, ack);
        } catch (Exception e) { Log.e(TAG, "sendMessage error", e); }
    }

    public void sendTextMessage(String roomId, String text) {
        sendMessage(roomId, text, "text", null, null, null);
    }

    public void typingStart(String roomId) {
        if (!isConnected()) return;
        try {
            JSONObject d = new JSONObject(); d.put("roomId", roomId);
            socket.emit("typing_start", d);
        } catch (Exception e) { Log.e(TAG, "typingStart error", e); }
    }

    public void typingStop(String roomId) {
        if (!isConnected()) return;
        try {
            JSONObject d = new JSONObject(); d.put("roomId", roomId);
            socket.emit("typing_stop", d);
        } catch (Exception e) { Log.e(TAG, "typingStop error", e); }
    }

    public void markSeen(String roomId, String messageId) {
        if (!isConnected()) return;
        try {
            JSONObject d = new JSONObject();
            d.put("roomId", roomId); d.put("messageId", messageId);
            socket.emit("mark_seen", d);
        } catch (Exception e) { Log.e(TAG, "markSeen error", e); }
    }

    public void joinRoom(String roomId) {
        if (!isConnected()) return;
        try {
            JSONObject d = new JSONObject(); d.put("roomId", roomId);
            socket.emit("join_room", d);
        } catch (Exception e) { Log.e(TAG, "joinRoom error", e); }
    }

    // ── Listener registration ─────────────────────────────────────────────────

    public void addMessageListener(MessageListener l)   { messageListeners.add(l); }
    public void removeMessageListener(MessageListener l) { messageListeners.remove(l); }
    public void addTypingListener(TypingListener l)     { typingListeners.add(l); }
    public void removeTypingListener(TypingListener l)  { typingListeners.remove(l); }
    public void addSeenListener(SeenListener l)         { seenListeners.add(l); }
    public void removeSeenListener(SeenListener l)      { seenListeners.remove(l); }
    public void addOnlineListener(OnlineListener l)     { onlineListeners.add(l); }
    public void removeOnlineListener(OnlineListener l)  { onlineListeners.remove(l); }

    // ── Utility ───────────────────────────────────────────────────────────────

    public boolean isConnected() { return socket != null && socket.connected(); }

    public void disconnect() {
        if (socket != null) { socket.disconnect(); socket.off(); socket = null; }
        messageListeners.clear(); typingListeners.clear();
        seenListeners.clear(); onlineListeners.clear();
    }
}
