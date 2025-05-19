package com.example.appfood;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import adapter.TabAdapter_BottomNavigation;
import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * HomeActivity is the main screen after login.
 * It manages the bottom navigation, ViewPager, user greeting, and profile navigation.
 */
public class HomeActivity extends AppCompatActivity {
    // UI components
    private TextView tvGreeting;
    private ImageView ivProfile;
    private ViewPager2 viewPager_home;
    private TabAdapter_BottomNavigation tabAdapter_home;
    private BottomNavigationView bottomNavigationView;

    /**
     * Called when the activity is starting. Initializes UI components,
     * sets up navigation, handles user greeting, and profile image click.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupBottomNavigation();
        setupViewPagerSync();

        String token = getIntent().getStringExtra("token");
        handleGreeting(token);
        setupProfileClick(token);
    }

    /**
     * Initializes view references.
     */
    private void initViews() {
        viewPager_home = findViewById(R.id.viewPager_home);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        tabAdapter_home = new TabAdapter_BottomNavigation(this);
        viewPager_home.setAdapter(tabAdapter_home);
        tvGreeting = findViewById(R.id.tv_greeting);
        ivProfile = findViewById(R.id.iv_profile);
    }

    /**
     * Sets up the BottomNavigationView to switch ViewPager pages.
     */
    private void setupBottomNavigation() {
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
            } else {
                return false;
            }
        });
    }

    /**
     * Synchronizes BottomNavigationView selection with ViewPager page changes.
     */
    private void setupViewPagerSync() {
        viewPager_home.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                bottomNavigationView.getMenu().getItem(position).setChecked(true);
            }
        });
    }

    /**
     * Handles greeting logic: fetches user info and displays greeting.
     * @param token The authentication token.
     */
    private void handleGreeting(String token) {
        if (token != null) {
            getUserInfo(token, new OnUserInfoCallback() {
                @Override
                public void onUserInfoReceived(String name, String email, String dateOfBirth, String country) {
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
    }

    /**
     * Sets up the profile image click to open UserProfileActivity.
     * @param token The authentication token.
     */
    private void setupProfileClick(String token) {
        ivProfile.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, UserProfileActivity.class);
            intent.putExtra("token", token);
            startActivity(intent);
        });
    }

    /**
     * Fetches user information from the API using the provided token.
     * Calls the appropriate callback method based on the API response.
     *
     * @param token    The authentication token for the API request.
     * @param callback The callback to handle success or error.
     */
    public void getUserInfo(String token, OnUserInfoCallback callback) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.UserResponse> call = apiService.getUserInfo("Bearer " + token);

        call.enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.UserResponse> call, Response<ModelResponse.UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String name = response.body().getData().getUser().getName();
                    String email = response.body().getData().getUser().getEmail();
                    String dateOfBirth = response.body().getData().getUser().getDateOfBirth();
                    String country = response.body().getData().getUser().getCountry();
                    callback.onUserInfoReceived(name, email, dateOfBirth, country);
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

    /**
     * Callback interface for receiving user information or error from getUserInfo.
     */
    public interface OnUserInfoCallback {
        /**
         * Called when user information is successfully received.
         *
         * @param name         The user's name.
         * @param email        The user's email.
         * @param dateOfBirth  The user's date of birth.
         * @param country      The user's country.
         */
        void onUserInfoReceived(String name, String email, String dateOfBirth, String country);

        /**
         * Called when there is an error fetching user information.
         *
         * @param errorMessage The error message.
         */
        void onError(String errorMessage);
    }
}
