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
    Button btnUpdate;
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
        btnUpdate = findViewById(R.id.btn_update);

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

        // Handle event update
        btnUpdate.setOnClickListener( v -> {
            String name = etName.getText().toString();
            String email = etEmail.getText().toString();
            String dateOfBirth = "1990-01-01";
            String country = "USA";

            updateProfile(token, name, email, dateOfBirth, country);
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

    public void updateProfile(String token, String name, String email, String dateOfBirth, String country) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.UpdateUserResponse> call = apiService.updateProfile("Bearer " + token, name, email, dateOfBirth, country);

        call.enqueue(new Callback<ModelResponse.UpdateUserResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.UpdateUserResponse> call, Response<ModelResponse.UpdateUserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(UserProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(UserProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.UpdateUserResponse> call, Throwable t) {
                Toast.makeText(UserProfileActivity.this, "Request failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}