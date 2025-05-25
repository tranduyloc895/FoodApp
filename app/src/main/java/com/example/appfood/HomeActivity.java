package com.example.appfood;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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

public class HomeActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabAddRecipe;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    // Handle permission result if needed
                }).launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fabAddRecipe = findViewById(R.id.fab_add);

        // Get token from intent first
        token = getIntent().getStringExtra("token");

        // If token is null, try to get from SharedPreferences
        if (token == null || token.isEmpty()) {
            SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
            token = sharedPreferences.getString("token", "");

            if (token == null || token.isEmpty()) {
                // Try another possible location
                sharedPreferences = getSharedPreferences("user_data", MODE_PRIVATE);
                token = sharedPreferences.getString("token", "");
            }

            // Save token in both places to ensure consistency
            if (token != null && !token.isEmpty()) {
                SharedPreferences.Editor editor = getSharedPreferences("user_prefs", MODE_PRIVATE).edit();
                editor.putString("token", token);
                editor.apply();
            }
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