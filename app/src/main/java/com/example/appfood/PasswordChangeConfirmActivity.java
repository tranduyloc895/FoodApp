package com.example.appfood;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity to confirm the user's intent to change their password.
 * Handles navigation to the password change screen and validates the token.
 */
public class PasswordChangeConfirmActivity extends AppCompatActivity {

    /**
     * Called when the activity is starting. Sets up UI and handles button actions.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_change_confirm);

        // Initialize back button and set click listener to finish activity
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Retrieve token from intent extras
        String token = getIntent().getStringExtra("token");
        if (token == null) {
            // Show error and close activity if token is missing
            Toast.makeText(this, "Invalid token", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set up confirm button to navigate to NewPasswordChange activity
        findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            Intent intent = new Intent(PasswordChangeConfirmActivity.this, NewPasswordChange.class);
            intent.putExtra("token", token);
            startActivity(intent);
            finish();
        });
    }
}
