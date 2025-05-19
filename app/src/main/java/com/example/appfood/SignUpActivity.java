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
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;

/**
 * Activity for user registration (sign up).
 * Handles user input, validation, API interaction, and navigation to sign-in.
 */
public class SignUpActivity extends AppCompatActivity {

    // UI components for user input and actions
    private EditText etUsername, etEmail, etPassword, etConfirmPassword;
    private Button btnSignUp;
    private ImageButton btnBack;
    private TextView tvSignIn;

    /**
     * Called when the activity is starting. Initializes UI components,
     * sets up listeners for sign-up and navigation actions.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize views
        initViews();

        // Handle back button click to finish activity
        btnBack.setOnClickListener(v -> finish());

        // Handle sign-up button click
        btnSignUp.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            // Validate input fields
            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin đăng ký", Toast.LENGTH_SHORT).show();
            } else if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show();
            } else {
                signUpUser(username, email, password, confirmPassword);
            }
        });

        // Handle sign-in text click to navigate to sign-in activity
        tvSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Initializes the UI components for user input and actions.
     */

    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnBack = findViewById(R.id.btnBack);
        tvSignIn = findViewById(R.id.tvSignIn);
    }

    /**
     * Calls the API to register a new user.
     * Handles both success and error responses.
     *
     * @param username        The user's username.
     * @param email           The user's email address.
     * @param password        The user's password.
     * @param confirmPassword The confirmation of the user's password.
     */
    private void signUpUser(String username, String email, String password, String confirmPassword) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.SignUpResponse> call = apiService.signUp(username, email, password, confirmPassword);

        call.enqueue(new retrofit2.Callback<ModelResponse.SignUpResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.SignUpResponse> call, retrofit2.Response<ModelResponse.SignUpResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Successful sign-up, navigate to sign-in screen
                    Toast.makeText(SignUpActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    // Handle error response from server
                    String errorMessage = "Đăng ký thất bại";
                    if (response.errorBody() != null) {
                        try {
                            errorMessage += ": " + response.errorBody().string();
                        } catch (Exception e) {
                            // Ignore parsing error
                        }
                    }
                    Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.SignUpResponse> call, Throwable t) {
                // Handle network or other errors
                Toast.makeText(SignUpActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
