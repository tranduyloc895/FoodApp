package com.example.appfood;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
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

public class NewPasswordChange extends AppCompatActivity {

    EditText currentPassword, newPassword, confirmNewPassword;
    Button btnUpdatePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_password_change);

        // Get token from intent
        String token = getIntent().getStringExtra("token");
        if (token == null) {
            Toast.makeText(this, "Invalid token", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI elements
        currentPassword = findViewById(R.id.etCurrentPassword);
        newPassword = findViewById(R.id.etPassword);
        confirmNewPassword = findViewById(R.id.etConfirmPassword);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);

        // Set up button click listener
        btnUpdatePassword.setOnClickListener(v -> {
            String currentPass = currentPassword.getText().toString();
            String newPass = newPassword.getText().toString();
            String confirmPass = confirmNewPassword.getText().toString();

            if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(NewPasswordChange.this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            } else if (!newPass.equals(confirmPass)) {
                Toast.makeText(NewPasswordChange.this, "Mật khẩu mới không khớp", Toast.LENGTH_SHORT).show();
            } else {
                updatePassword(token, currentPass, newPass, confirmPass);
            }
        });

    }

    private void updatePassword(String token, String currentPass, String newPass, String confirmPass) {

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.ChangePasswordResponse> call = apiService.updatePassword("Bearer " + token, currentPass, newPass, confirmPass);
        call.enqueue(new retrofit2.Callback<ModelResponse.ChangePasswordResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.ChangePasswordResponse> call, retrofit2.Response<ModelResponse.ChangePasswordResponse> response) {
                if (response.isSuccessful()) {
                    // Handle success
                    Toast.makeText(NewPasswordChange.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                    // Colse all activities and go to login screen
                    Intent intent = new Intent(NewPasswordChange.this, SignInActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    // Output error message
                    String errorMessage = "Error: " + response.code() + " - " + response.message();
                    if (response.errorBody() != null) {
                        try {
                            errorMessage += " - " + response.errorBody().string();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(NewPasswordChange.this, errorMessage, Toast.LENGTH_SHORT).show();

                }
            }

            @Override
            public void onFailure(Call<ModelResponse.ChangePasswordResponse> call, Throwable t) {
                // Handle error
                Toast.makeText(NewPasswordChange.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }
}