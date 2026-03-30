package com.nakama.linguachat.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.nakama.linguachat.databinding.ActivityOtpVerificationBinding;
import com.nakama.linguachat.models.requests.ResendOtpRequest;
import com.nakama.linguachat.models.requests.VerifyOtpRequest;
import com.nakama.linguachat.models.responses.AuthResponse;
import com.nakama.linguachat.models.responses.MessageResponse;
import com.nakama.linguachat.network.RetrofitClient;
import com.nakama.linguachat.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OtpVerificationActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "user_id";
    public static final String EXTRA_EMAIL = "email";

    private ActivityOtpVerificationBinding binding;
    private String userId;
    private String email;
    private CountDownTimer resendTimer;
    private static final long RESEND_COOLDOWN_MS = 60_000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        email  = getIntent().getStringExtra(EXTRA_EMAIL);

        if (userId == null) {
            Toast.makeText(this, "Invalid session. Please register again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        binding.tvEmailHint.setText("Enter the 6-digit code sent to\n" + email);

        binding.btnVerify.setOnClickListener(v -> verifyOtp());
        binding.btnResend.setOnClickListener(v -> resendOtp());

        startResendCooldown();
    }

    private void verifyOtp() {
        String otp = binding.etOtp.getText().toString().trim();
        if (otp.length() != 6) {
            showError("Please enter the 6-digit code");
            return;
        }

        setLoading(true);

        RetrofitClient.getInstance().getApiService()
                .verifyOtp(new VerifyOtpRequest(userId, otp))
                .enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            AuthResponse body = response.body();
                            SessionManager.getInstance(OtpVerificationActivity.this)
                                    .saveSession(body.getToken(), body.getUser());

                            Toast.makeText(OtpVerificationActivity.this,
                                    "Email verified!", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(OtpVerificationActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } else {
                            String msg = "Verification failed";
                            try {
                                if (response.errorBody() != null) {
                                    String err = response.errorBody().string();
                                    if (err.contains("expired")) msg = "OTP expired. Please request a new one.";
                                    else if (err.contains("Incorrect")) msg = "Incorrect code. Try again.";
                                }
                            } catch (Exception ignored) {}
                            showError(msg);
                        }
                    }

                    @Override
                    public void onFailure(Call<AuthResponse> call, Throwable t) {
                        setLoading(false);
                        showError("Network error");
                    }
                });
    }

    private void resendOtp() {
        binding.btnResend.setEnabled(false);
        setLoading(true);

        RetrofitClient.getInstance().getApiService()
                .resendOtp(new ResendOtpRequest(userId))
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful()) {
                            Toast.makeText(OtpVerificationActivity.this,
                                    "New code sent to " + email, Toast.LENGTH_SHORT).show();
                            startResendCooldown();
                        } else {
                            showError("Could not resend OTP. Try again later.");
                            binding.btnResend.setEnabled(true);
                        }
                    }

                    @Override
                    public void onFailure(Call<MessageResponse> call, Throwable t) {
                        setLoading(false);
                        showError("Network error");
                        binding.btnResend.setEnabled(true);
                    }
                });
    }

    private void startResendCooldown() {
        binding.btnResend.setEnabled(false);
        if (resendTimer != null) resendTimer.cancel();

        resendTimer = new CountDownTimer(RESEND_COOLDOWN_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                binding.btnResend.setText("Resend in " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                binding.btnResend.setEnabled(true);
                binding.btnResend.setText("Resend Code");
            }
        }.start();
    }

    private void setLoading(boolean loading) {
        binding.btnVerify.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) resendTimer.cancel();
    }
}
