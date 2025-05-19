package com.example.appfood;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity for user sign-in.
 * Handles user authentication, navigation to sign-up and password reset,
 * and manages login API interaction.
 */
public class SignInActivity extends AppCompatActivity {
    // UI components for user input and actions
    private EditText etEmail, etPassword;
    private Button btnSignIn;
    private TextView forgotPassword;

    /**
     * Called when the activity is starting. Initializes UI components,
     * sets up listeners for sign-in and navigation actions.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize views
        initViews();

        // Handle sign-in button click
        btnSignIn.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // Validate input fields
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(email, password);
            }
        });
    }

    /**
     * Initializes the UI components for user input and actions.
     */
    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        forgotPassword = findViewById(R.id.tvForgotPassword);
    }

    /**
     * Navigates to the sign-up activity.
     *
     * @param view The view that triggered this method.
     */
    public void SignUp(View view) {
        Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
        startActivity(intent);
    }

    /**
     * Navigates to the forgot password (email input) activity.
     *
     * @param view The view that triggered this method.
     */
    public void ForgotPassword(View view) {
        Intent intent = new Intent(SignInActivity.this, EmailInput.class);
        startActivity(intent);
    }

    /**
     * Calls the API to authenticate the user.
     * Handles both success and error responses.
     *
     * @param email    The user's email address.
     * @param password The user's password.
     */
    private void loginUser(String email, String password) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.LoginResponse> call = apiService.login(email, password);

        call.enqueue(new Callback<ModelResponse.LoginResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.LoginResponse> call, Response<ModelResponse.LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ModelResponse.LoginResponse loginResponse = response.body();

                    // Check if login was successful based on message
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
                Toast.makeText(SignInActivity.this, "Không thể kết nối đến máy chủ: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
