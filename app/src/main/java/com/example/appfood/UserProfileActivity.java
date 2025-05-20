package com.example.appfood;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import adapter.CountryAdapter;
import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import fragment.HomeFragment;
import fragment.LogoutDialogFragment;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Displays and allows editing of the user's profile.
 * Handles user info display, editing, country selection, date of birth selection,
 * logout, and password change navigation.
 */
public class UserProfileActivity extends AppCompatActivity {

    // UI components
    private EditText etName, etEmail;
    private ImageButton btnBack, btnLogout;
    private Button btnUpdate;
    private ImageView iVdob;
    private TextView dobTextView, tvChangePassword;
    private Spinner countrySpinner;

    // State variables
    private String country;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Fullscreen mode
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_user_profile);

        // Initialize UI
        initViews();

        btnBack.setOnClickListener(v -> finish());
        iVdob.setOnClickListener(v -> showDatePicker());
        setupCountrySpinner();

        String token = getIntent().getStringExtra("token");
        if (token != null) {
            getUserInfo(token, new HomeFragment.OnUserInfoCallback() {
                @Override
                public void onUserInfoReceived(String name, String email, String dateOfBirth, String country) {
                    etName.setText(name);
                    etEmail.setText(email);
                    dobTextView.setText(dateOfBirth);
                    countrySpinner.setSelection(((ArrayAdapter<String>) countrySpinner.getAdapter()).getPosition(country));
                }
                @Override
                public void onError(String errorMessage) {
                    etName.setText("Failed to load user info");
                }
            });
        } else {
            etName.setText("Token is missing.");
        }

        btnLogout.setOnClickListener(v -> LogoutDialogFragment.newInstance(token)
                .show(getSupportFragmentManager(), "logoutDialog"));

        btnUpdate.setOnClickListener(v -> updateProfile(
                token,
                etName.getText().toString(),
                etEmail.getText().toString(),
                dobTextView.getText().toString(),
                country
        ));

        tvChangePassword.setOnClickListener(v -> {
            sendEmail(etEmail.getText().toString());
            Intent intent = new Intent(UserProfileActivity.this, VerifyOTPChangePass.class);
            intent.putExtra("email", etEmail.getText().toString());
            intent.putExtra("token", token);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Initializes the UI components and sets up listeners.
     */

    private void initViews() {
        etName = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        btnBack = findViewById(R.id.btn_back);
        btnUpdate = findViewById(R.id.btn_update);
        iVdob = findViewById(R.id.iv_dropdown);
        dobTextView = findViewById(R.id.tv_dateOfbirth_selected);
        countrySpinner = findViewById(R.id.spinner_country);
        tvChangePassword = findViewById(R.id.tv_change_password);
        btnLogout = findViewById(R.id.btn_logout);
    }

    /**
     * Shows a date picker dialog for selecting date of birth.
     */
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, day) -> dobTextView.setText((month + 1) + "/" + day + "/" + year),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    /**
     * Sets up the country spinner with a list of countries and handles selection.
     */
    private void setupCountrySpinner() {
        List<String> countries = getCountryList();
        CountryAdapter adapter = new CountryAdapter(this, countries);
        countrySpinner.setAdapter(adapter);
        adapter.setOnItemSelectedListener(new CountryAdapter.OnItemSelectedListener() {
            @Override
            public void onItemSelected(String selectedCountry) {
                country = selectedCountry;
            }
            @Override
            public void onNothingSelected() {}
        });
    }

    /**
     * Retrieves the user's information from the API and passes it to the callback.
     */
    public void getUserInfo(String token, HomeFragment.OnUserInfoCallback callback) {
        ApiService apiService = RetrofitClient.getApiService();
        apiService.getUserInfo("Bearer " + token).enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.UserResponse> call, Response<ModelResponse.UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ModelResponse.UserResponse.User user = response.body().getData().getUser();
                    callback.onUserInfoReceived(user.getName(), user.getEmail(), user.getDateOfBirth(), user.getCountry());
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
     * Updates the user's profile information via the API.
     */
    public void updateProfile(String token, String name, String email, String dateOfBirth, String country) {
        ApiService apiService = RetrofitClient.getApiService();
        apiService.updateProfile("Bearer " + token, name, email, dateOfBirth, country)
                .enqueue(new Callback<ModelResponse.UpdateUserResponse>() {
                    @Override
                    public void onResponse(Call<ModelResponse.UpdateUserResponse> call, Response<ModelResponse.UpdateUserResponse> response) {
                        Toast.makeText(UserProfileActivity.this,
                                response.isSuccessful() && response.body() != null
                                        ? "Profile updated successfully"
                                        : "Failed to update profile",
                                Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onFailure(Call<ModelResponse.UpdateUserResponse> call, Throwable t) {
                        Toast.makeText(UserProfileActivity.this, "Request failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Returns a sorted list of unique country names from available locales.
     */
    private List<String> getCountryList() {
        Locale[] locales = Locale.getAvailableLocales();
        List<String> countries = new ArrayList<>();

        for (Locale locale : locales) {
            String country = locale.getDisplayCountry();
            if (!country.isEmpty() && !countries.contains(country)) {
                countries.add(country);
            }
        }

        Collections.sort(countries);
        return countries;
    }

    /**
     * Sends a password reset email to the user via the API.
     */
    public void sendEmail(String email) {
        ApiService apiService = RetrofitClient.getApiService();
        apiService.forgotPassword(email).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Toast.makeText(UserProfileActivity.this,
                        response.isSuccessful() ? "Please check your email!" : "Failed to send email",
                        Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }
}
