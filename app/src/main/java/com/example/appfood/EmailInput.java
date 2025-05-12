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

public class EmailInput extends AppCompatActivity {
    EditText etEmailInput;
    Button btnSendCode;
    ImageButton btnBack;
    TextView signIn;
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

        btnBack.setOnClickListener(v -> {
            returnToLogin();
        });
    }

    public void sendEmail(String email) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<Void> call = apiService.forgotPassword(email);
        call.enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                if (response.isSuccessful()) {
                    // Handle success
                    Toast.makeText(EmailInput.this, "Please check your email!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(EmailInput.this, VerifyOTPResetActivity.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                    finish();
                } else {
                    // Handle failure
                    Toast.makeText(EmailInput.this, "Failed to send email", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Handle error
            }
        });
    }

    //Return to login
    public void returnToLogin() {
        Intent intent = new Intent(EmailInput.this, SignInActivity.class);
        startActivity(intent);
        finish();
    }

}