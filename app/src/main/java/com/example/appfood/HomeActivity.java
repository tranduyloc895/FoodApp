package com.example.appfood;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;

import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {
    private TextView tvGreeting;
    private ImageView ivProfile;
    private ViewPager2 viewPager_home;
    private TabAdapter_Home tabAdapter_home;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager_home = findViewById(R.id.viewPager_home);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        tabAdapter_home = new TabAdapter_Home(this);

        viewPager_home.setAdapter(tabAdapter_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                viewPager_home.setCurrentItem(0);
                return true;
            } else if (itemId == R.id.nav_saved) {
                viewPager_home.setCurrentItem(1);
                return true;
            } else if (itemId == R.id.nav_notifications) {
                viewPager_home.setCurrentItem(2);
                return true;
            } else if (itemId == R.id.nav_profile) {
                viewPager_home.setCurrentItem(3);
                return true;
            }
            return false;
        });

        viewPager_home.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                bottomNavigationView.getMenu().getItem(position).setChecked(true);
            }
        });

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
