package com.nakama.linguachat.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.nakama.linguachat.adapters.UserSearchAdapter;
import com.nakama.linguachat.databinding.ActivityUserSearchBinding;
import com.nakama.linguachat.models.User;
import com.nakama.linguachat.models.requests.DirectRoomRequest;
import com.nakama.linguachat.models.responses.RoomResponse;
import com.nakama.linguachat.models.responses.UsersResponse;
import com.nakama.linguachat.network.RetrofitClient;
import com.nakama.linguachat.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserSearchActivity extends AppCompatActivity
        implements UserSearchAdapter.OnUserClickListener {

    private ActivityUserSearchBinding binding;
    private SessionManager sessionManager;
    private UserSearchAdapter adapter;
    private List<User> users = new ArrayList<>();
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("New Chat");
        }

        sessionManager = SessionManager.getInstance(this);

        adapter = new UserSearchAdapter(users, this);
        binding.rvUsers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvUsers.setAdapter(adapter);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                String q = s.toString().trim();
                if (q.length() < 2) {
                    users.clear();
                    adapter.notifyDataSetChanged();
                    return;
                }
                searchRunnable = () -> searchUsers(q);
                searchHandler.postDelayed(searchRunnable, 400);
            }
        });
    }

    private void searchUsers(String query) {
        binding.progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getInstance().getApiService()
                .searchUsers(sessionManager.getBearerToken(), query)
                .enqueue(new Callback<UsersResponse>() {
                    @Override
                    public void onResponse(Call<UsersResponse> c, Response<UsersResponse> r) {
                        binding.progressBar.setVisibility(View.GONE);
                        if (r.isSuccessful() && r.body() != null) {
                            users.clear();
                            users.addAll(r.body().getUsers());
                            adapter.notifyDataSetChanged();
                            binding.tvEmpty.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    }
                    @Override
                    public void onFailure(Call<UsersResponse> c, Throwable t) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(UserSearchActivity.this, "Search failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onUserClick(User user) {
        binding.progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getInstance().getApiService()
                .createDirectRoom(sessionManager.getBearerToken(),
                        new DirectRoomRequest(user.getId()))
                .enqueue(new Callback<RoomResponse>() {
                    @Override
                    public void onResponse(Call<RoomResponse> c, Response<RoomResponse> r) {
                        binding.progressBar.setVisibility(View.GONE);
                        if (r.isSuccessful() && r.body() != null) {
                            String roomId   = r.body().getRoom().getId();
                            String roomName = user.getDisplayName();
                            Intent intent = new Intent(UserSearchActivity.this, ChatActivity.class);
                            intent.putExtra(ChatActivity.EXTRA_ROOM_ID,   roomId);
                            intent.putExtra(ChatActivity.EXTRA_ROOM_NAME, roomName);
                            intent.putExtra(ChatActivity.EXTRA_ROOM_TYPE, "direct");
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(UserSearchActivity.this,
                                    "Could not open chat", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<RoomResponse> c, Throwable t) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(UserSearchActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}
