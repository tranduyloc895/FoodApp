package com.example.appfood;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
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
    private ImageView iVdob, ivProfilePicture;
    private TextView dobTextView, tvChangePassword, tvChangePicture;
    private Spinner countrySpinner;
    private FrameLayout loadingOverlay;

    // State variables
    private String country;
    private String currentAvatarUrl;
    private boolean isDataLoading = false;

    // API token
    private String token;

    // Permission request code
    private static final int STORAGE_PERMISSION_CODE = 100;

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

        token = getIntent().getStringExtra("token");
        if (token != null) {
            // Show loading before data loads
            showLoading(true);
            loadUserData();
        } else {
            etName.setText("Token is missing.");
            showLoading(false);
        }

        btnLogout.setOnClickListener(v -> LogoutDialogFragment.newInstance(token)
                .show(getSupportFragmentManager(), "logoutDialog"));

        btnUpdate.setOnClickListener(v -> {
            showLoading(true); // Show loading when update starts
            updateProfile(
                    token,
                    etName.getText().toString(),
                    etEmail.getText().toString(),
                    dobTextView.getText().toString(),
                    country
            );
        });

        tvChangePassword.setOnClickListener(v -> {
            sendEmail(etEmail.getText().toString());
            Intent intent = new Intent(UserProfileActivity.this, VerifyOTPChangePass.class);
            intent.putExtra("email", etEmail.getText().toString());
            intent.putExtra("token", token);
            startActivity(intent);
            finish();
        });

        tvChangePicture.setOnClickListener(v -> changeProfilePicture());
    }

    /**
     * Load user data from API
     */
    private void loadUserData() {
        isDataLoading = true;
        showLoading(true);

        getUserInfo(token, new HomeFragment.OnUserInfoCallback() {
            @Override
            public void onUserInfoReceived(String name, String email, String dateOfBirth, String country, String urlAvatar) {
                etName.setText(name);
                etEmail.setText(email);
                dobTextView.setText(dateOfBirth);

                // Save avatar URL
                currentAvatarUrl = urlAvatar;

                // Load avatar using Glide
                loadProfileAvatar(urlAvatar);

                // Set country in spinner if it exists
                if (country != null && !country.isEmpty()) {
                    int position = ((ArrayAdapter<String>) countrySpinner.getAdapter()).getPosition(country);
                    if (position >= 0) {
                        countrySpinner.setSelection(position);
                    }
                }

                // Hide loading when data is loaded
                isDataLoading = false;
                showLoading(false);
            }

            @Override
            public void onError(String errorMessage) {
                etName.setText("Failed to load user info");
                Toast.makeText(UserProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();

                // Hide loading even on error
                isDataLoading = false;
                showLoading(false);
            }
        });
    }

    /**
     * Show or hide loading overlay
     * @param isLoading true to show loading, false to hide
     */
    private void showLoading(boolean isLoading) {
        // Run on UI thread to avoid potential threading issues
        runOnUiThread(() -> {
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);

                // Disable/enable UI interactions based on loading state
                btnUpdate.setEnabled(!isLoading);
                tvChangePicture.setEnabled(!isLoading);
                tvChangePassword.setEnabled(!isLoading);
                btnLogout.setEnabled(!isLoading);
            }
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
        tvChangePicture = findViewById(R.id.tv_change_picture);
        ivProfilePicture = findViewById(R.id.iv_profile);
        loadingOverlay = findViewById(R.id.loading_overlay);
    }

    /**
     * Load profile avatar from URL using Glide
     */
    private void loadProfileAvatar(String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile)
                            .circleCrop())
                    .into(ivProfilePicture);
        } else {
            ivProfilePicture.setImageResource(R.drawable.ic_profile);
        }
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
                    callback.onUserInfoReceived(user.getName(), user.getEmail(), user.getDateOfBirth(), user.getCountry(), user.getUrlAvatar());
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
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(UserProfileActivity.this,
                                    "Profile updated successfully",
                                    Toast.LENGTH_SHORT).show();

                            // Reload user data to reflect changes
                            loadUserData();
                        } else {
                            Toast.makeText(UserProfileActivity.this,
                                    "Failed to update profile",
                                    Toast.LENGTH_SHORT).show();

                            // Hide loading on error
                            showLoading(false);
                        }
                    }
                    @Override
                    public void onFailure(Call<ModelResponse.UpdateUserResponse> call, Throwable t) {
                        Toast.makeText(UserProfileActivity.this, "Request failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        // Hide loading on failure
                        showLoading(false);
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
        // Show loading while sending email
        showLoading(true);

        ApiService apiService = RetrofitClient.getApiService();
        apiService.forgotPassword(email).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                // Hide loading once we get a response
                showLoading(false);

                Toast.makeText(UserProfileActivity.this,
                        response.isSuccessful() ? "Please check your email!" : "Failed to send email",
                        Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Hide loading on failure
                showLoading(false);
            }
        });
    }

    /**
     * Handles function to change profile picture with proper permission checking.
     */
    private void changeProfilePicture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+ (API 33+), use new photo picker or request READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, STORAGE_PERMISSION_CODE);
            } else {
                openImagePicker();
            }
        } else {
            // For Android 12 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
            } else {
                openImagePicker();
            }
        }
    }

    /**
     * Opens the image picker after permissions are granted.
     */
    private void openImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
            imagePicker.launch(intent);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePicker.launch(intent);
        }
    }

    /**
     * Handle permission request results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with image picking
                openImagePicker();
            } else {
                // Permission denied
                Toast.makeText(this, "Storage permission is required to change profile picture", Toast.LENGTH_LONG).show();
            }
        }
    }

    private final androidx.activity.result.ActivityResultLauncher<Intent> imagePicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        // Show loading while uploading
                        showLoading(true);

                        // Show selected image immediately for better UX while uploading
                        Glide.with(this)
                                .load(imageUri)
                                .apply(new RequestOptions().circleCrop())
                                .into(ivProfilePicture);

                        uploadProfilePicture(imageUri, token);
                        Toast.makeText(this, "Image selected, uploading...", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    /**
     * Improved method to get real file path from URI with better handling
     */
    private String getRealPathFromURI(Uri uri) {
        String result = null;

        // Check if the URI scheme is content
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    result = cursor.getString(columnIndex);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // If the scheme is file or we couldn't get the path from content, use the path directly
        if (result == null) {
            result = uri.getPath();
        }

        return result;
    }

    /**
     * Improved method to upload profile picture using ContentResolver and temp files
     */
    private void uploadProfilePicture(Uri imageUri, String token) {
        try {
            // Create a temporary file from the URI using ContentResolver
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                File tempFile = File.createTempFile("profile_picture", ".jpg", getCacheDir());
                FileOutputStream fos = new FileOutputStream(tempFile);

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }

                inputStream.close();
                fos.close();

                // Create MultipartBody.Part from the temp file
                RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), tempFile);
                MultipartBody.Part avatarPart = MultipartBody.Part.createFormData("avatar", tempFile.getName(), requestFile);

                // Call API
                ApiService apiService = RetrofitClient.getApiService();
                Call<ModelResponse.UserResponse> call = apiService.uploadAvatar("Bearer " + token, avatarPart);

                call.enqueue(new Callback<ModelResponse.UserResponse>() {
                    @Override
                    public void onResponse(Call<ModelResponse.UserResponse> call, Response<ModelResponse.UserResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(UserProfileActivity.this, "Profile picture updated!", Toast.LENGTH_SHORT).show();

                            // Reload entire user data after successful avatar update
                            loadUserData();
                        } else {
                            Toast.makeText(UserProfileActivity.this,
                                    "Upload failed: " + (response.message() != null ? response.message() : "Unknown error"),
                                    Toast.LENGTH_SHORT).show();

                            // If upload failed, revert to previous avatar
                            loadProfileAvatar(currentAvatarUrl);

                            // Hide loading overlay
                            showLoading(false);
                        }
                    }

                    @Override
                    public void onFailure(Call<ModelResponse.UserResponse> call, Throwable t) {
                        Toast.makeText(UserProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();

                        // If upload failed, revert to previous avatar
                        loadProfileAvatar(currentAvatarUrl);

                        // Hide loading overlay
                        showLoading(false);
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();

            // If there was an error, revert to previous avatar
            loadProfileAvatar(currentAvatarUrl);

            // Hide loading overlay
            showLoading(false);
        }
    }
}