package com.nakama.linguachat.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.nakama.linguachat.databinding.ActivityRegisterBinding;
import com.nakama.linguachat.models.requests.RegisterRequest;
import com.nakama.linguachat.models.responses.RegisterResponse;
import com.nakama.linguachat.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnRegister.setOnClickListener(v -> attemptRegister());
        binding.tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void attemptRegister() {
        String username = binding.etUsername.getText().toString().trim();
        String email    = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();
        String confirmPassword = binding.etConfirmPassword.getText().toString();
        String displayName = binding.etDisplayName.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all required fields");
            return;
        }
        if (username.length() < 3) {
            showError("Username must be at least 3 characters");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address");
            return;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        setLoading(true);

        RegisterRequest request = new RegisterRequest(
                username, email, password,
                displayName.isEmpty() ? username : displayName
        );

        RetrofitClient.getInstance().getApiService()
                .register(request)
                .enqueue(new Callback<RegisterResponse>() {
                    @Override
                    public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            String userId = response.body().getUserId();
                            // Navigate to OTP verification
                            Intent intent = new Intent(RegisterActivity.this, OtpVerificationActivity.class);
                            intent.putExtra(OtpVerificationActivity.EXTRA_USER_ID, userId);
                            intent.putExtra(OtpVerificationActivity.EXTRA_EMAIL, email);
                            startActivity(intent);
                            finish();
                        } else {
                            String msg = "Registration failed";
                            try {
                                if (response.errorBody() != null) {
                                    String errorJson = response.errorBody().string();
                                    // Simple parse for "message" field
                                    if (errorJson.contains("Email already")) msg = "Email already registered";
                                    else if (errorJson.contains("Username already")) msg = "Username already taken";
                                }
                            } catch (Exception ignored) {}
                            showError(msg);
                        }
                    }

                    @Override
                    public void onFailure(Call<RegisterResponse> call, Throwable t) {
                        setLoading(false);
                        showError("Network error. Is the server running?");
                    }
                });
    }

    private void setLoading(boolean loading) {
        binding.btnRegister.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnRegister.setText(loading ? "" : "Create Account");
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
