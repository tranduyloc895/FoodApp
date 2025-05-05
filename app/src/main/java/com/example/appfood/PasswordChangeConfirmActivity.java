package com.example.appfood;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PasswordChangeConfirmActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_change_confirm);

        // Initialize back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Initialize confirm button
        findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            // Here you would typically navigate to the new password setup screen
            // For now, we'll just show a toast
            Toast.makeText(this, "Navigating to password setup...", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(this, SetNewPasswordActivity.class);
            // startActivity(intent);
        });
    }
} 