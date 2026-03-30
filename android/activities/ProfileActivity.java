package com.nakama.linguachat.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.nakama.linguachat.databinding.ActivityProfileBinding;
import com.nakama.linguachat.models.User;
import com.nakama.linguachat.models.requests.UpdateProfileRequest;
import com.nakama.linguachat.models.responses.UserResponse;
import com.nakama.linguachat.network.RetrofitClient;
import com.nakama.linguachat.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private SessionManager sessionManager;

    // Supported UI languages
    private static final String[] LANGUAGE_LABELS = {
            "English", "Hindi", "Spanish", "French", "German",
            "Japanese", "Korean", "Chinese (Simplified)", "Arabic", "Portuguese"
    };
    private static final String[] LANGUAGE_CODES = {
            "en", "hi", "es", "fr", "de", "ja", "ko", "zh", "ar", "pt"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Profile");
        }

        sessionManager = SessionManager.getInstance(this);

        // Set up language spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, LANGUAGE_LABELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerLanguage.setAdapter(adapter);

        // Load saved user data
        populateFromSession();

        binding.btnSave.setOnClickListener(v -> saveProfile());
    }

    private void populateFromSession() {
        User user = sessionManager.getUser();
        if (user == null) return;

        binding.etDisplayName.setText(user.getDisplayName());
        binding.etBio.setText(user.getBio());
        binding.tvUsername.setText("@" + user.getUsername());
        binding.tvEmail.setText(user.getEmail());

        // Set language spinner selection
        String lang = user.getPreferredLanguage();
        for (int i = 0; i < LANGUAGE_CODES.length; i++) {
            if (LANGUAGE_CODES[i].equals(lang)) {
                binding.spinnerLanguage.setSelection(i);
                break;
            }
        }
    }

    private void saveProfile() {
        String displayName = binding.etDisplayName.getText().toString().trim();
        String bio = binding.etBio.getText().toString().trim();
        int selectedLangIdx = binding.spinnerLanguage.getSelectedItemPosition();
        String langCode = LANGUAGE_CODES[selectedLangIdx];

        if (displayName.isEmpty()) {
            showError("Display name cannot be empty");
            return;
        }

        setLoading(true);

        UpdateProfileRequest request = new UpdateProfileRequest(displayName, bio, langCode, null);

        RetrofitClient.getInstance().getApiService()
                .updateProfile(sessionManager.getBearerToken(), request)
                .enqueue(new Callback<UserResponse>() {
                    @Override
                    public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            // Update local session with new user data
                            sessionManager.saveUser(response.body().getUser());
                            Toast.makeText(ProfileActivity.this,
                                    "Profile updated!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            showError("Could not update profile");
                        }
                    }

                    @Override
                    public void onFailure(Call<UserResponse> call, Throwable t) {
                        setLoading(false);
                        showError("Network error");
                    }
                });
    }

    private void setLoading(boolean loading) {
        binding.btnSave.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
