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

        // Get token from intent
        String token = getIntent().getStringExtra("token");
        if (token == null) {
            Toast.makeText(this, "Invalid token", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            Intent intent = new Intent(PasswordChangeConfirmActivity.this, NewPasswordChange.class);
            intent.putExtra("token", token);
            startActivity(intent);
            finish();
        });
    }
} 