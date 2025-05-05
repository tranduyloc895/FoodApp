package com.example.appfood;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class VerifyOTPActivity extends AppCompatActivity {
    private EditText[] otpDigits;
    private TextView tvEmailSent;
    private TextView tvResendEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        // Initialize views
        otpDigits = new EditText[]{
                findViewById(R.id.etDigit1),
                findViewById(R.id.etDigit2),
                findViewById(R.id.etDigit3),
                findViewById(R.id.etDigit4),
                findViewById(R.id.etDigit5)
        };
        tvEmailSent = findViewById(R.id.tvEmailSent);
        tvResendEmail = findViewById(R.id.tvResendEmail);
        ImageButton btnBack = findViewById(R.id.btnBack);

        // Get email from intent
        String email = getIntent().getStringExtra("email");
        if (email != null) {
            tvEmailSent.setText("We sent a link to " + email);
        }

        // Set up OTP input handling
        setupOTPInputs();

        // Back button click handler
        btnBack.setOnClickListener(v -> finish());

        // Resend email click handler
        tvResendEmail.setOnClickListener(v -> {
            Toast.makeText(this, "Verification code resent", Toast.LENGTH_SHORT).show();
            // Add your resend logic here
        });

        // Verify button click handler
        findViewById(R.id.btnVerifyCode).setOnClickListener(v -> verifyCode());
    }

    private void setupOTPInputs() {
        for (int i = 0; i < otpDigits.length; i++) {
            final int currentIndex = i;
            otpDigits[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && currentIndex < otpDigits.length - 1) {
                        otpDigits[currentIndex + 1].requestFocus();
                    } else if (s.length() == 0 && currentIndex > 0) {
                        otpDigits[currentIndex - 1].requestFocus();
                    }
                }
            });
        }
    }

    private void verifyCode() {
        StringBuilder otp = new StringBuilder();
        boolean isComplete = true;

        for (EditText digit : otpDigits) {
            String text = digit.getText().toString();
            if (text.isEmpty()) {
                isComplete = false;
                break;
            }
            otp.append(text);
        }

        if (isComplete) {
            // Add your verification logic here
            Toast.makeText(this, "Verifying code: " + otp.toString(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please enter the complete verification code", Toast.LENGTH_SHORT).show();
        }
    }
} 