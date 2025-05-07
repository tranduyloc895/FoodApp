package com.example.appfood;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;

public class NewPasswordActivity extends AppCompatActivity {
    private EditText etPassword, etConfirmPassword;
    ImageButton btnBack;
    Button btnUpdatePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_password);

        // Initialize views
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);
        btnBack = findViewById(R.id.btnBack);

        // Back button click handler
        btnBack.setOnClickListener(v -> finish());

        //Get email from intent
        String email = getIntent().getStringExtra("email");

        // Update password button click handler
        btnUpdatePassword.setOnClickListener(v -> {
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            updatePassword(email, password, confirmPassword);
        });
        ;
    }

    private void updatePassword(String email, String password, String confirmPassword) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.LoginResponse> call = apiService.resetPassword(email, password, confirmPassword);
        call.enqueue(new retrofit2.Callback<ModelResponse.LoginResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.LoginResponse> call, retrofit2.Response<ModelResponse.LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ModelResponse.LoginResponse loginResponse = response.body();
                    if (loginResponse.getMessage().equals("success")) {
                        Toast.makeText(NewPasswordActivity.this, "Mật khẩu cập nhật thành công", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(NewPasswordActivity.this, SignInActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(NewPasswordActivity.this, loginResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Handle error response
                    String errorMessage = "Error: " + response.code();
                    Toast.makeText(NewPasswordActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.LoginResponse> call, Throwable t) {
                Toast.makeText(NewPasswordActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


} 