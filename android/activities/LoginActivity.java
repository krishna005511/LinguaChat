package com.nakama.linguachat.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.nakama.linguachat.databinding.ActivityLoginBinding;
import com.nakama.linguachat.models.requests.LoginRequest;
import com.nakama.linguachat.models.responses.AuthResponse;
import com.nakama.linguachat.network.RetrofitClient;
import com.nakama.linguachat.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Skip login if already logged in
        if (SessionManager.getInstance(this).isLoggedIn()) {
            goToMain();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void attemptLogin() {
        String email    = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter your email and password");
            return;
        }

        setLoading(true);

        RetrofitClient.getInstance().getApiService()
                .login(new LoginRequest(email, password))
                .enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            AuthResponse body = response.body();
                            SessionManager.getInstance(LoginActivity.this)
                                    .saveSession(body.getToken(), body.getUser());
                            goToMain();
                        } else if (response.code() == 403) {
                            // Unverified account — send to OTP screen
                            try {
                                if (response.errorBody() != null) {
                                    String err = response.errorBody().string();
                                    // Extract userId from error JSON (simple string search)
                                    int idx = err.indexOf("\"userId\":\"");
                                    if (idx != -1) {
                                        String sub = err.substring(idx + 10);
                                        String extractedId = sub.substring(0, sub.indexOf("\""));
                                        Intent intent = new Intent(LoginActivity.this, OtpVerificationActivity.class);
                                        intent.putExtra(OtpVerificationActivity.EXTRA_USER_ID, extractedId);
                                        intent.putExtra(OtpVerificationActivity.EXTRA_EMAIL, email);
                                        startActivity(intent);
                                        return;
                                    }
                                }
                            } catch (Exception ignored) {}
                            showError("Email not verified. Please check your inbox.");
                        } else {
                            showError("Invalid email or password");
                        }
                    }

                    @Override
                    public void onFailure(Call<AuthResponse> call, Throwable t) {
                        setLoading(false);
                        showError("Network error. Is the server running?");
                    }
                });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void setLoading(boolean loading) {
        binding.btnLogin.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setText(loading ? "" : "Login");
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
