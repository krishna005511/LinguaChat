package com.nakama.linguachat.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.nakama.linguachat.R;
import com.nakama.linguachat.adapters.ChatListAdapter;
import com.nakama.linguachat.databinding.ActivityMainBinding;
import com.nakama.linguachat.models.ChatMessage;
import com.nakama.linguachat.models.ChatRoom;
import com.nakama.linguachat.models.responses.RoomsResponse;
import com.nakama.linguachat.network.RetrofitClient;
import com.nakama.linguachat.socket.SocketManager;
import com.nakama.linguachat.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity
        implements ChatListAdapter.OnRoomClickListener,
                   SocketManager.MessageListener,
                   SocketManager.OnlineListener {

    private ActivityMainBinding binding;
    private SessionManager sessionManager;
    private ChatListAdapter adapter;
    private List<ChatRoom> rooms = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = SessionManager.getInstance(this);
        if (!sessionManager.isLoggedIn()) { goToLogin(); return; }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle("LinguaChat");

        // RecyclerView
        adapter = new ChatListAdapter(rooms, sessionManager.getUserId(), this);
        binding.rvChats.setLayoutManager(new LinearLayoutManager(this));
        binding.rvChats.setAdapter(adapter);

        // Socket
        SocketManager sm = SocketManager.getInstance(this);
        sm.connect(sessionManager.getToken());
        sm.addMessageListener(this);
        sm.addOnlineListener(this);

        binding.fabNewChat.setOnClickListener(v ->
                startActivity(new Intent(this, UserSearchActivity.class)));

        loadRooms();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRooms();
    }

    private void loadRooms() {
        binding.progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getInstance().getApiService()
                .getRooms(sessionManager.getBearerToken())
                .enqueue(new Callback<RoomsResponse>() {
                    @Override
                    public void onResponse(Call<RoomsResponse> c, Response<RoomsResponse> r) {
                        binding.progressBar.setVisibility(View.GONE);
                        if (r.isSuccessful() && r.body() != null) {
                            rooms.clear();
                            rooms.addAll(r.body().getRooms());
                            adapter.notifyDataSetChanged();
                            binding.tvEmpty.setVisibility(rooms.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    }
                    @Override
                    public void onFailure(Call<RoomsResponse> c, Throwable t) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Could not load chats", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onRoomClick(ChatRoom room) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_ROOM_ID,   room.getId());
        intent.putExtra(ChatActivity.EXTRA_ROOM_NAME, room.getDisplayName(sessionManager.getUserId()));
        intent.putExtra(ChatActivity.EXTRA_ROOM_TYPE, room.getType());
        startActivity(intent);
    }

    // ── SocketManager.MessageListener ─────────────────────────────────────────
    @Override
    public void onNewMessage(ChatMessage msg) {
        runOnUiThread(() -> {
            // Move the matching room to top and update last message
            for (int i = 0; i < rooms.size(); i++) {
                if (rooms.get(i).getId().equals(msg.getRoomId())) {
                    ChatRoom r = rooms.remove(i);
                    r.setLastMessage(msg);
                    rooms.add(0, r);
                    adapter.notifyDataSetChanged();
                    return;
                }
            }
            // Room not loaded yet — reload list
            loadRooms();
        });
    }

    // ── SocketManager.OnlineListener ─────────────────────────────────────────
    @Override
    public void onUserOnline(String userId) {
        runOnUiThread(() -> {
            for (ChatRoom r : rooms) r.setOnlineStatus(userId, true);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onUserOffline(String userId, String lastSeen) {
        runOnUiThread(() -> {
            for (ChatRoom r : rooms) r.setOnlineStatus(userId, false);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_logout) {
            SocketManager.getInstance(this).disconnect();
            sessionManager.clearSession();
            goToLogin();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void goToLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SocketManager sm = SocketManager.getInstance(this);
        sm.removeMessageListener(this);
        sm.removeOnlineListener(this);
    }
}
