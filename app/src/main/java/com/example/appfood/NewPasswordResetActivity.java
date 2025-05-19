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

/**
 * Activity for resetting a user's password after a password reset request.
 * Handles user input, validation, and API interaction for password reset.
 */
public class NewPasswordResetActivity extends AppCompatActivity {
    // UI components for password input and actions
    private EditText etPassword, etConfirmPassword;
    private ImageButton btnBack;
    private Button btnUpdatePassword;

    /**
     * Called when the activity is starting. Initializes UI components,
     * sets up listeners, and retrieves the user's email from intent.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_password_reset);

        // Initialize views
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);
        btnBack = findViewById(R.id.btnBack);

        // Handle back button click to finish activity
        btnBack.setOnClickListener(v -> finish());

        // Get email from intent extras
        String email = getIntent().getStringExtra("email");

        // Handle update password button click
        btnUpdatePassword.setOnClickListener(v -> {
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            // Validate input before making API call
            if (password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            } else if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
            } else {
                updatePassword(email, password, confirmPassword);
            }
        });
    }

    /**
     * Calls the API to reset the user's password.
     * Handles both success and error responses.
     *
     * @param email           The user's email address.
     * @param password        The new password.
     * @param confirmPassword The confirmation of the new password.
     */
    private void updatePassword(String email, String password, String confirmPassword) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.LoginResponse> call = apiService.resetPassword(email, password, confirmPassword);

        call.enqueue(new retrofit2.Callback<ModelResponse.LoginResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.LoginResponse> call, retrofit2.Response<ModelResponse.LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ModelResponse.LoginResponse loginResponse = response.body();
                    // Check if password reset was successful
                    if ("success".equals(loginResponse.getMessage())) {
                        Toast.makeText(NewPasswordResetActivity.this, "Mật khẩu cập nhật thành công", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(NewPasswordResetActivity.this, SignInActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(NewPasswordResetActivity.this, loginResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Handle error response from server
                    String errorMessage = "Error: " + response.code();
                    Toast.makeText(NewPasswordResetActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.LoginResponse> call, Throwable t) {
                // Handle network or other errors
                Toast.makeText(NewPasswordResetActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
