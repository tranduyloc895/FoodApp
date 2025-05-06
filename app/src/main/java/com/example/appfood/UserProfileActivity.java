package com.example.appfood;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserProfileActivity extends AppCompatActivity {

    EditText etName, etEmail;
    ImageButton btnBack, btnLogout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        setContentView(R.layout.activity_user_profile);

        etName = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        btnBack = findViewById(R.id.btn_back);

        // Set up back button
        btnBack.setOnClickListener(v -> {
            finish();
        });

        //Get token from intent
        String token = getIntent().getStringExtra("token");
        //Fill in user info (name, email)
        if (token != null) {
            getUserInfo(token, new HomeActivity.OnUserInfoCallback() {
                @Override
                public void onUserInfoReceived(String name, String email) {
                    etName.setText(name);
                    etEmail.setText(email);
                }

                @Override
                public void onError(String errorMessage) {
                    etName.setText("Failed to load user info");
                }
            });
        } else {
            etName.setText("Token is missing.");
        }

        // Set up logout button
        btnLogout = findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> {
            LogoutDialogFragment.newInstance(token)
                    .show(getSupportFragmentManager(), "logoutDialog");
        });

    }

    public void getUserInfo(String token, HomeActivity.OnUserInfoCallback callback) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.UserResponse> call = apiService.getUserInfo("Bearer " + token);

        call.enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.UserResponse> call, Response<ModelResponse.UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String name = response.body().getData().getUser().getName();
                    String email = response.body().getData().getUser().getEmail();
                    callback.onUserInfoReceived(name, email);
                } else {
                    callback.onError("Response error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.UserResponse> call, Throwable t) {
                callback.onError("Request failed: " + t.getMessage());
            }
        });
    }
}