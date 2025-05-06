package com.example.appfood;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;

public class VerifyOTPActivity extends AppCompatActivity {
    EditText etOtpCode;
    Button btnVerifyOTP;
    ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        // Get the email from the intent
        String email = getIntent().getStringExtra("email");

        etOtpCode = findViewById(R.id.etOtpCode);
        btnVerifyOTP = findViewById(R.id.btnVerifyOtp);

        // Handle back button click
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        // Handle button verify OTP click
        btnVerifyOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String otp = etOtpCode.getText().toString().trim();
                if (otp.isEmpty()) {
                    Toast.makeText(VerifyOTPActivity.this, "Please enter the OTP", Toast.LENGTH_SHORT).show();
                } else {
                    verifyOTP(email, otp);
                }
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
                    Toast.makeText(VerifyOTPActivity.this, "OTP verified successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(VerifyOTPActivity.this, NewPasswordActivity.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                    finish();
                } else {
                    // Handle failure
                    Toast.makeText(VerifyOTPActivity.this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.VerifyOtpResponse> call, Throwable t) {
                // Handle network failure
                Toast.makeText(VerifyOTPActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


} 