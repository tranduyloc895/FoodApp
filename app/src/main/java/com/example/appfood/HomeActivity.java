package com.example.appfood;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import fragment.HomeFragment;
import fragment.SavedRecipeFragment;
import fragment.NotificationsFragment;
import fragment.ProfileFragment;
import android.Manifest;

import services.NotificationService;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabAddRecipe;
    private String token;
    private NotificationService notificationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize notification service early
        notificationService = NotificationService.getInstance(this);

        // Request notification permission for Android 13+
        requestNotificationPermissionIfNeeded();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fabAddRecipe = findViewById(R.id.fab_add);

        // Get token from intent first
        token = getIntent().getStringExtra("token");

        // If token is null, try to get from SharedPreferences
        if (token == null || token.isEmpty()) {
            token = getTokenFromPreferences();
        }

        // Initial notification check
        if (token != null && !token.isEmpty()) {
            // We'll replace this with continuous checking in onResume
            notificationService.checkForNotifications(token);
        }

        setupBottomNavigation();
        setupAddRecipeButton(token);

        // Display HomeFragment by default when app starts
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Start continuous notification checking when app comes to foreground
        if (token != null && !token.isEmpty()) {
            Log.d(TAG, "Starting continuous notification checking in onResume");
            notificationService.startContinuousChecking(token);
        } else {
            Log.d(TAG, "Cannot start notification checking: No token available");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop continuous notification checking when app goes to background
        Log.d(TAG, "Stopping continuous notification checking in onPause");
        notificationService.stopContinuousChecking();
    }

    private String getTokenFromPreferences() {
        String token = "";

        // Try first preferences location
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        token = sharedPreferences.getString("token", "");

        // If not found, try another possible location
        if (token == null || token.isEmpty()) {
            sharedPreferences = getSharedPreferences("user_data", MODE_PRIVATE);
            token = sharedPreferences.getString("token", "");
        }

        // Save token in both places to ensure consistency
        if (token != null && !token.isEmpty()) {
            SharedPreferences.Editor editor = getSharedPreferences("user_prefs", MODE_PRIVATE).edit();
            editor.putString("token", token);
            editor.apply();
        }

        return token;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        Log.d(TAG, "Notification permission granted");
                    } else {
                        Log.d(TAG, "Notification permission denied");
                        Toast.makeText(this, "Please enable notifications to receive updates",
                                Toast.LENGTH_LONG).show();
                    }
                }).launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void setupAddRecipeButton(String token) {
        fabAddRecipe.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AddRecipeActivity.class);
            intent.putExtra("token", token);
            startActivity(intent);
        });
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_saved) {
                selectedFragment = SavedRecipeFragment.newInstance(token);
            } else if (itemId == R.id.nav_notifications) {
                selectedFragment = NotificationsFragment.newInstance(token);
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            return true;
        });
    }
}