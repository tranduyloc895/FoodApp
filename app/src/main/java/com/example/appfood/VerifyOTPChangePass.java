package com.example.appfood;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;

public class VerifyOTPChangePass extends AppCompatActivity {

    TextView tvOTPmessage;
    Button btnVerifyOTP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otpchange_pass);

        tvOTPmessage = findViewById(R.id.tvOtpMessage);
        btnVerifyOTP = findViewById(R.id.btnVerifyOtp);

        // Get values from Intent
        String email = getIntent().getStringExtra("email");
        String token = getIntent().getStringExtra("token");
        if (email == null || token == null) {
            Toast.makeText(this, "Invalid email or token", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set the message with email
        String message = "Mã xác nhận đã được gửi đến địa chỉ email: " + email;
        tvOTPmessage.setText(message);

        // Handle button click to verify OTP
        btnVerifyOTP.setOnClickListener(v -> {
            String otp = ((TextView) findViewById(R.id.etOtpCode)).getText().toString();
            if (otp.isEmpty()) {
                Toast.makeText(VerifyOTPChangePass.this, "Vui lòng nhập mã xác nhận", Toast.LENGTH_SHORT).show();
            } else {
                verifyOTP(email, otp);
                Intent intent = new Intent(VerifyOTPChangePass.this, PasswordChangeConfirmActivity.class);
                intent.putExtra("token", token);
                startActivity(intent);
                finish();
            }
        });


    }

    private void verifyOTP(String email, String otp) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.VerifyOtpResponse> call = apiService.verifyOtp(email, otp);
        call.enqueue(new Callback<ModelResponse.VerifyOtpResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.VerifyOtpResponse> call, retrofit2.Response<ModelResponse.VerifyOtpResponse> response) {
                if (response.isSuccessful()) {
                    // Handle success
                    Toast.makeText(VerifyOTPChangePass.this, "OTP verified successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    // Handle failure
                    Toast.makeText(VerifyOTPChangePass.this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.VerifyOtpResponse> call, Throwable t) {
                // Handle network failure
                Toast.makeText(VerifyOTPChangePass.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}