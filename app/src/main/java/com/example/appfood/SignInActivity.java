package com.example.appfood;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignInActivity extends AppCompatActivity {
    // UI components for user input and actions
    private EditText etEmail, etPassword;
    private View btnSignInWithLoading;
    private TextView btnText;
    private ProgressBar btnProgressBar;
    private TextView forgotPassword;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize views
        initViews();

        // Handle sign-in button click
        btnSignInWithLoading.setOnClickListener(v -> {
            // Prevent multiple clicks during loading
            if (isLoading) return;

            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // Validate input fields
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            } else {
                setLoadingState(true);
                loginUser(email, password);
            }
        });
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        // Find our custom button view
        btnSignInWithLoading = findViewById(R.id.btnSignInWithLoading);

        // Get the child views from our custom button layout
        btnText = btnSignInWithLoading.findViewById(R.id.btnText);
        btnProgressBar = btnSignInWithLoading.findViewById(R.id.btnProgressBar);

        forgotPassword = findViewById(R.id.tvForgotPassword);
    }

    private void setLoadingState(boolean loading) {
        isLoading = loading;
        if (loading) {
            // Show loading state
            btnProgressBar.setVisibility(View.VISIBLE);
            btnText.setText("Signing In...");
            btnSignInWithLoading.setEnabled(false);
            btnSignInWithLoading.setAlpha(0.7f);
        } else {
            // Show normal state
            btnProgressBar.setVisibility(View.GONE);
            btnText.setText("Sign In");
            btnSignInWithLoading.setEnabled(true);
            btnSignInWithLoading.setAlpha(1.0f);
        }
    }

    public void SignUp(View view) {
        Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
        startActivity(intent);
    }

    public void ForgotPassword(View view) {
        Intent intent = new Intent(SignInActivity.this, EmailInput.class);
        startActivity(intent);
    }

    private void loginUser(String email, String password) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.LoginResponse> call = apiService.login(email, password);

        call.enqueue(new Callback<ModelResponse.LoginResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.LoginResponse> call, Response<ModelResponse.LoginResponse> response) {
                // Reset button state
                setLoadingState(false);

                if (response.isSuccessful() && response.body() != null) {
                    ModelResponse.LoginResponse loginResponse = response.body();

                    // Check if login was successful
                    if ("success".equals(loginResponse.getMessage())) {
                        String token = loginResponse.getToken();

                        Toast.makeText(SignInActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                        // Navigate to HomeActivity with token
                        Intent intent = new Intent(SignInActivity.this, HomeActivity.class);
                        intent.putExtra("token", token);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(SignInActivity.this, "Đăng nhập thất bại", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SignInActivity.this, "Sai tài khoản hoặc mật khẩu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.LoginResponse> call, Throwable t) {
                // Reset button state
                setLoadingState(false);

                Toast.makeText(SignInActivity.this, "Không thể kết nối đến máy chủ: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}