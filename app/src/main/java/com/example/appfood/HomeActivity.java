package com.example.appfood;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {
    private TextView tvGreeting;
    private ImageView ivProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tvGreeting = findViewById(R.id.tv_greeting);
        ivProfile = findViewById(R.id.iv_profile);

        String token = getIntent().getStringExtra("token");

        //Welcome message
        if (token != null) {
            getUserInfo(token, new OnUserInfoCallback() {
                @Override
                public void onUserInfoReceived(String name, String email) {
                    tvGreeting.setText("Hello, " + name + "!");
                }

                @Override
                public void onError(String errorMessage) {
                    tvGreeting.setText("Failed to load user info");
                }
            });
        } else {
            tvGreeting.setText("Token is missing.");
        }

        ivProfile.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, UserProfileActivity.class);
            intent.putExtra("token", token);
            startActivity(intent);
        });
    }

    // Gọi API để lấy user info
    public void getUserInfo(String token, OnUserInfoCallback callback) {
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

    public interface OnUserInfoCallback {
        void onUserInfoReceived(String name, String email);
        void onError(String errorMessage);
    }
}
