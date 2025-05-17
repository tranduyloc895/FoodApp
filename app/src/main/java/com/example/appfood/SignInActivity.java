package com.example.appfood;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
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

public class SignInActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private Button btnSignIn;
    private TextView forgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        forgotPassword = findViewById(R.id.tvForgotPassword);

        btnSignIn.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(email, password);
            }
        });
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
                if (response.isSuccessful() && response.body() != null) {
                    ModelResponse.LoginResponse loginResponse = response.body();

                    // Check if login was successful based on status
                    if (loginResponse.getMessage() != null && loginResponse.getMessage().equals("success")) {
                        // Save token and session id if needed
                        String token = loginResponse.getToken();

                        Toast.makeText(SignInActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

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