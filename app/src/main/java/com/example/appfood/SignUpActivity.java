package com.example.appfood;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;

public class SignUpActivity extends AppCompatActivity {

    EditText etUsername, etEmail, etPassword, etConfirmPassword;
    Button btnSignUp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);

        btnSignUp.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                // Show error message
            } else if (!password.equals(confirmPassword)) {
                // Show error message
            } else {
                signUpUser(username, email, password, confirmPassword);
            }
        });
    }
    private void signUpUser(String username, String email, String password, String confirmPassword) {
        // Call API to sign up user
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.SignUpResponse> call = apiService.signUp(username, email, password, confirmPassword);
        call.enqueue(new retrofit2.Callback<ModelResponse.SignUpResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.SignUpResponse> call, retrofit2.Response<ModelResponse.SignUpResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ModelResponse.SignUpResponse signUpResponse = response.body();
                    // Handle successful sign-up
                    Toast.makeText(SignUpActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    // Handle error
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.SignUpResponse> call, Throwable t) {
                // Handle failure
            }
        });
        // Handle response
    }
}