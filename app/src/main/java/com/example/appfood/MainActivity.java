package com.example.appfood;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import fragment.HomeFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabAdd;
    private boolean isNavigationHidden = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize navigation components
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fabAdd = findViewById(R.id.fab_add);
    }

    // Interface for fragments to communicate with activity
    public interface NavigationBarController {
        void hideNavigationBar();
        void showNavigationBar();
    }

    /**
     * Hide the navigation bar with animation
     */
    public void hideNavigationBar() {
        if (!isNavigationHidden) {
            bottomNavigationView.animate()
                    .translationY(bottomNavigationView.getHeight())
                    .setDuration(200)
                    .start();

            if (fabAdd != null) {
                fabAdd.animate()
                        .translationY(bottomNavigationView.getHeight())
                        .setDuration(200)
                        .start();
            }
            isNavigationHidden = true;
        }
    }

    /**
     * Show the navigation bar with animation
     */
    public void showNavigationBar() {
        if (isNavigationHidden) {
            bottomNavigationView.animate()
                    .translationY(0)
                    .setDuration(200)
                    .start();

            if (fabAdd != null) {
                fabAdd.animate()
                        .translationY(0)
                        .setDuration(200)
                        .start();
            }
            isNavigationHidden = false;
        }
    }

    /**
     * Get the navigation controller for fragments to use
     */
    public NavigationBarController getNavigationBarController() {
        return new NavigationBarController() {
            @Override
            public void hideNavigationBar() {
                MainActivity.this.hideNavigationBar();
            }

            @Override
            public void showNavigationBar() {
                MainActivity.this.showNavigationBar();
            }
        };
    }
}