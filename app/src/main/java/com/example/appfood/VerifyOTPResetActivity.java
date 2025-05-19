package com.example.appfood;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;

/**
 * Activity for verifying OTP sent to user's email for password reset.
 * Handles OTP input, validation, and API verification.
 */
public class VerifyOTPResetActivity extends AppCompatActivity {
    private EditText etOtpCode;
    private Button btnVerifyOTP;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp_reset);

        // Get the email from the intent
        String email = getIntent().getStringExtra("email");

        // Initialize UI components
        etOtpCode = findViewById(R.id.etOtpCode);
        btnVerifyOTP = findViewById(R.id.btnVerifyOtp);
        btnBack = findViewById(R.id.btnBack);

        // Handle back button click to finish activity
        btnBack.setOnClickListener(v -> finish());

        // Handle verify OTP button click
        btnVerifyOTP.setOnClickListener(v -> {
            String otp = etOtpCode.getText().toString().trim();
            if (otp.isEmpty()) {
                Toast.makeText(this, "Please enter the OTP", Toast.LENGTH_SHORT).show();
            } else {
                verifyOTP(email, otp);
            }
        });
    }

    /**
     * Calls the API to verify the OTP code and navigates to password reset on success.
     *
     * @param email The user's email address.
     * @param otp   The OTP code entered by the user.
     */
    private void verifyOTP(String email, String otp) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.VerifyOtpResponse> call = apiService.verifyOtp(email, otp);
        call.enqueue(new Callback<ModelResponse.VerifyOtpResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.VerifyOtpResponse> call, retrofit2.Response<ModelResponse.VerifyOtpResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(VerifyOTPResetActivity.this, "OTP verified successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(VerifyOTPResetActivity.this, NewPasswordResetActivity.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(VerifyOTPResetActivity.this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.VerifyOtpResponse> call, Throwable t) {
                Toast.makeText(VerifyOTPResetActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
