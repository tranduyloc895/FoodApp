package com.example.appfood;

import android.app.DatePickerDialog;
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
import fragment.LogoutDialogFragment;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserProfileActivity extends AppCompatActivity {

    EditText etName, etEmail;
    ImageButton btnBack, btnLogout;
    Button btnUpdate;
    ImageView iVdob, iVcountry;
    TextView dobTextView;
    int year, month, day;
    String country;
    Spinner countrySpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        setContentView(R.layout.activity_user_profile);

        etName = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        btnBack = findViewById(R.id.btn_back);
        btnUpdate = findViewById(R.id.btn_update);
        iVdob = findViewById(R.id.iv_dropdown);
        dobTextView = findViewById(R.id.tv_dateOfbirth_selected);
        countrySpinner = findViewById(R.id.spinner_country);

        // Set up back button
        btnBack.setOnClickListener(v -> {
            finish();
        });

        // Set up dropdown for date of birth
        iVdob.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            day = calendar.get(Calendar.DAY_OF_MONTH);
            month = calendar.get(Calendar.MONTH);
            year = calendar.get(Calendar.YEAR);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    UserProfileActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        dobTextView.setText((selectedMonth + 1) + "/" + selectedDay + "/" + selectedYear);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });
        // Set up spinner for country
        List<String> countries = getCountryList();
        CountryAdapter adapter = new CountryAdapter(this, countries);
        countrySpinner.setAdapter(adapter);
        adapter.setOnItemSelectedListener(new CountryAdapter.OnItemSelectedListener() {
            @Override
            public void onItemSelected(String Country) {
                country = Country;
            }
            @Override
            public void onNothingSelected() {
                // Do nothing
            }
        });

        //Get token from intent
        String token = getIntent().getStringExtra("token");
        //Fill in user info
        if (token != null) {
            getUserInfo(token, new HomeActivity.OnUserInfoCallback() {
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

        // Set up logout button
        btnLogout = findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> {
            LogoutDialogFragment.newInstance(token)
                    .show(getSupportFragmentManager(), "logoutDialog");
        });

        // Handle event update
        btnUpdate.setOnClickListener( v -> {
            String name = etName.getText().toString();
            String email = etEmail.getText().toString();
            String dateOfBirth = dobTextView.getText().toString();

            updateProfile(token, name, email, dateOfBirth, country);
        });

    }

    public void getUserInfo(String token, HomeActivity.OnUserInfoCallback callback) {
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

    public void updateProfile(String token, String name, String email, String dateOfBirth, String country) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.UpdateUserResponse> call = apiService.updateProfile("Bearer " + token, name, email, dateOfBirth, country);

        call.enqueue(new Callback<ModelResponse.UpdateUserResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.UpdateUserResponse> call, Response<ModelResponse.UpdateUserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(UserProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(UserProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.UpdateUserResponse> call, Throwable t) {
                Toast.makeText(UserProfileActivity.this, "Request failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

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
}