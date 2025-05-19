package com.example.appfood;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import api.ApiService;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmailInput extends AppCompatActivity {
    private EditText etEmailInput;
    private Button btnSendCode;
    private ImageButton btnBack;
    private TextView signIn;

    /**
     * Called when the activity is starting. Initializes UI components and sets up listeners.
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down, this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_input);

        etEmailInput = findViewById(R.id.etEmail);
        btnSendCode = findViewById(R.id.btnSendCode);
        signIn = findViewById(R.id.tvSignIn);
        btnBack = findViewById(R.id.btn_back);

        btnSendCode.setOnClickListener(v -> {
            String email = etEmailInput.getText().toString().trim();
            if (email.isEmpty()) {
                etEmailInput.setError("Please enter your email");
            } else {
                sendEmail(email);
            }
        });

        btnBack.setOnClickListener(v -> returnToLogin());
    }

    /**
     * Sends a password reset email to the provided email address using the API service.
     * Shows a Toast message based on the result and navigates to the OTP verification screen if successful.
     * @param email The email address to send the reset code to.
     */
    private void sendEmail(String email) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<Void> call = apiService.forgotPassword(email);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EmailInput.this, "Please check your email!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(EmailInput.this, VerifyOTPResetActivity.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(EmailInput.this, "Failed to send email", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(EmailInput.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Navigates the user back to the SignInActivity and finishes the current activity.
     */
    private void returnToLogin() {
        Intent intent = new Intent(EmailInput.this, SignInActivity.class);
        startActivity(intent);
        finish();
    }
}

