package com.example.appfood;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import fragment.HomeFragment;
import fragment.SavedRecipeFragment;
import fragment.NotificationsFragment;
import fragment.ProfileFragment;

/**
 * HomeActivity là màn hình chính sau khi đăng nhập.
 * Nó sử dụng BottomNavigationView để điều hướng giữa các Fragment.
 */
public class HomeActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabAddRecipe;
    private String token;

    /**
     * Được gọi khi Activity khởi động. Thiết lập BottomNavigation.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fabAddRecipe = findViewById(R.id.fab_add);
        setupBottomNavigation();

        token = getIntent().getStringExtra("token");
        setupAddRecipeButton(token);

        // Hiển thị HomeFragment mặc định khi ứng dụng khởi động
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }

    /**
     * Handles the click event for the add recipe button.
     */
    private void setupAddRecipeButton(String token) {
        fabAddRecipe.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AddRecipeActivity.class);
            intent.putExtra("token", token);
            startActivity(intent);
        });
    }

    /**
     * Thiết lập BottomNavigation để chuyển đổi giữa các Fragment.
     */
    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_saved) {
                selectedFragment = new SavedRecipeFragment().newInstance(token);
            } else if (itemId == R.id.nav_notifications) {
                selectedFragment = new NotificationsFragment();
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