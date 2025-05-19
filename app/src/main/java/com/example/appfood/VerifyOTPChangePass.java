package com.example.appfood;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;

/**
 * Activity for verifying OTP sent to user's email for password change.
 * Handles OTP input, validation, and API verification.
 */
public class VerifyOTPChangePass extends AppCompatActivity {

    private TextView tvOTPmessage;
    private Button btnVerifyOTP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otpchange_pass);

        tvOTPmessage = findViewById(R.id.tvOtpMessage);
        btnVerifyOTP = findViewById(R.id.btnVerifyOtp);

        String email = getIntent().getStringExtra("email");
        String token = getIntent().getStringExtra("token");
        if (email == null || token == null) {
            Toast.makeText(this, "Invalid email or token", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvOTPmessage.setText("Mã xác nhận đã được gửi đến địa chỉ email: " + email);

        btnVerifyOTP.setOnClickListener(v -> {
            String otp = ((TextView) findViewById(R.id.etOtpCode)).getText().toString();
            if (otp.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập mã xác nhận", Toast.LENGTH_SHORT).show();
            } else {
                verifyOTP(email, otp, token);
            }
        });
    }

    /**
     * Calls the API to verify the OTP code and navigates to password change confirmation on success.
     */
    private void verifyOTP(String email, String otp, String token) {
        ApiService apiService = RetrofitClient.getApiService();
        apiService.verifyOtp(email, otp).enqueue(new Callback<ModelResponse.VerifyOtpResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.VerifyOtpResponse> call, retrofit2.Response<ModelResponse.VerifyOtpResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(VerifyOTPChangePass.this, "OTP verified successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(VerifyOTPChangePass.this, PasswordChangeConfirmActivity.class);
                    intent.putExtra("token", token);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(VerifyOTPChangePass.this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ModelResponse.VerifyOtpResponse> call, Throwable t) {
                Toast.makeText(VerifyOTPChangePass.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
