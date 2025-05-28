package com.example.appfood;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
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

import androidx.activity.result.ActivityResultLauncher;
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
    private static final String TAG = "UserProfileActivity";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int STORAGE_PERMISSION_CODE = 100;

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
    private String token;
    private TextView tvDeleteAccount;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupWindowAndContentView();
        initViews();

        // Register image picker launcher
        registerImagePicker();

        setupListeners();

        token = getIntent().getStringExtra("token");
        if (token != null) {
            showLoading(true);
            loadUserData();
        } else {
            etName.setText("Token is missing.");
            showLoading(false);
        }
    }

    /**
     * Registers the image picker launcher
     */
    private void registerImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            handleSelectedImage(selectedImageUri);
                        }
                    }
                }
        );
    }

    /**
     * Sets up window flags and content view
     */
    private void setupWindowAndContentView() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_user_profile);
    }

    /**
     * Initializes all UI components
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
        tvDeleteAccount = findViewById(R.id.tv_delete_account);
    }

    /**
     * Sets up click listeners and other UI interactions
     */
    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        iVdob.setOnClickListener(v -> showDatePicker());
        setupCountrySpinner();

        btnLogout.setOnClickListener(v -> LogoutDialogFragment.newInstance(token)
                .show(getSupportFragmentManager(), "logoutDialog"));

        btnUpdate.setOnClickListener(v -> {
            showLoading(true);
            updateProfile(
                    token,
                    etName.getText().toString(),
                    etEmail.getText().toString(),
                    dobTextView.getText().toString(),
                    country
            );
        });

        tvChangePassword.setOnClickListener(v -> {
            String email = etEmail.getText().toString();
            sendEmail(email);
            navigateToVerifyOTP(email);
        });

        tvChangePicture.setOnClickListener(v -> openImagePicker());

        tvDeleteAccount.setOnClickListener(v -> showDeleteAccountConfirmationDialog());
    }

    /**
     * Opens the image picker - using the same approach as HomeFragment
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    /**
     * Navigate to OTP verification for password change
     */
    private void navigateToVerifyOTP(String email) {
        Intent intent = new Intent(UserProfileActivity.this, VerifyOTPChangePass.class);
        intent.putExtra("email", email);
        intent.putExtra("token", token);
        startActivity(intent);
        finish();
    }

    /**
     * Load user data from API
     */
    private void loadUserData() {
        isDataLoading = true;
        showLoading(true);

        getUserInfo(token, new UserInfoCallback() {
            @Override
            public void onSuccess(String name, String email, String dateOfBirth, String country, String avatarUrl) {
                etName.setText(name);
                etEmail.setText(email);
                dobTextView.setText(dateOfBirth);

                // Save avatar URL
                currentAvatarUrl = avatarUrl;

                // Load avatar using Glide
                loadProfileAvatar(avatarUrl);

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
                Log.e(TAG, "Error loading user info: " + errorMessage);

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
    public void getUserInfo(String token, UserInfoCallback callback) {
        ApiService apiService = RetrofitClient.getApiService();
        apiService.getUserInfo(BEARER_PREFIX + token).enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.UserResponse> call, Response<ModelResponse.UserResponse> response) {
                if (response.isSuccessful() && response.body() != null &&
                        response.body().getData() != null &&
                        response.body().getData().getUser() != null) {

                    ModelResponse.UserResponse.User user = response.body().getData().getUser();
                    callback.onSuccess(
                            user.getName(),
                            user.getEmail(),
                            user.getDateOfBirth(),
                            user.getCountry(),
                            user.getUrlAvatar()
                    );
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
        apiService.updateProfile(BEARER_PREFIX + token, name, email, dateOfBirth, country)
                .enqueue(new Callback<ModelResponse.UpdateUserResponse>() {
                    @Override
                    public void onResponse(Call<ModelResponse.UpdateUserResponse> call,
                                           Response<ModelResponse.UpdateUserResponse> response) {
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
                        Log.e(TAG, "Update profile failed", t);
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
        showLoading(true);

        ApiService apiService = RetrofitClient.getApiService();
        apiService.forgotPassword(email).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                showLoading(false);

                Toast.makeText(UserProfileActivity.this,
                        response.isSuccessful() ? "Please check your email!" : "Failed to send email",
                        Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Send email failed", t);
            }
        });
    }

    /**
     * Handle the selected image for upload
     */
    private void handleSelectedImage(Uri imageUri) {
        // Show loading while uploading
        showLoading(true);

        // Show selected image immediately for better UX while uploading
        Glide.with(this)
                .load(imageUri)
                .apply(new RequestOptions().circleCrop())
                .into(ivProfilePicture);

        uploadProfilePicture(imageUri, token);
    }

    /**
     * Converts Uri to File - similar to HomeFragment's method
     */
    private File getFileFromUri(Uri uri) {
        try {
            ContentResolver contentResolver = getContentResolver();
            String fileName = getFileNameFromUri(contentResolver, uri);

            // Create temporary file
            File outputDir = getCacheDir();
            File outputFile = File.createTempFile("profile_", ".jpg", outputDir);

            // Copy data to file
            InputStream inputStream = contentResolver.openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(outputFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            return outputFile;
        } catch (Exception e) {
            Log.e(TAG, "Error getting file from uri: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets filename from Uri
     */
    private String getFileNameFromUri(ContentResolver contentResolver, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = contentResolver.query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    /**
     * Improved method to upload profile picture using ContentResolver and temp files
     */
    private void uploadProfilePicture(Uri imageUri, String token) {
        try {
            // Get file from Uri using the same approach as HomeFragment
            File imageFile = getFileFromUri(imageUri);
            if (imageFile == null) {
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
                showLoading(false);
                return;
            }

            // Create multipart request
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
            MultipartBody.Part avatarPart = MultipartBody.Part.createFormData(
                    "avatar", imageFile.getName(), requestFile);

            // Call API
            ApiService apiService = RetrofitClient.getApiService();
            Call<ModelResponse.UserResponse> call = apiService.uploadAvatar(BEARER_PREFIX + token, avatarPart);

            call.enqueue(new Callback<ModelResponse.UserResponse>() {
                @Override
                public void onResponse(Call<ModelResponse.UserResponse> call,
                                       Response<ModelResponse.UserResponse> response) {
                    handleAvatarUploadResponse(response);
                }

                @Override
                public void onFailure(Call<ModelResponse.UserResponse> call, Throwable t) {
                    handleAvatarUploadFailure(t);
                }
            });
        } catch (Exception e) {
            handleImageProcessingError(e);
        }
    }

    /**
     * Handle API response for avatar upload
     */
    private void handleAvatarUploadResponse(Response<ModelResponse.UserResponse> response) {
        if (response.isSuccessful() && response.body() != null) {
            Toast.makeText(UserProfileActivity.this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
            // Reload entire user data after successful avatar update
            loadUserData();
        } else {
            String errorMsg = response.message() != null ? response.message() : "Unknown error";
            Toast.makeText(UserProfileActivity.this, "Upload failed: " + errorMsg, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Avatar upload failed: " + response.code() + " " + errorMsg);

            // If upload failed, revert to previous avatar
            loadProfileAvatar(currentAvatarUrl);
            showLoading(false);
        }
    }

    /**
     * Handle API failure for avatar upload
     */
    private void handleAvatarUploadFailure(Throwable t) {
        Toast.makeText(UserProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
        Log.e(TAG, "Avatar upload network failure", t);

        // If upload failed, revert to previous avatar
        loadProfileAvatar(currentAvatarUrl);
        showLoading(false);
    }

    /**
     * Handle image processing error
     */
    private void handleImageProcessingError(Exception e) {
        e.printStackTrace();
        Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        Log.e(TAG, "Error processing image", e);

        // If there was an error, revert to previous avatar
        loadProfileAvatar(currentAvatarUrl);
        showLoading(false);
    }

    /**
     * Callback interface for user information retrieval
     */
    public interface UserInfoCallback {
        void onSuccess(String name, String email, String dateOfBirth, String country, String avatarUrl);
        void onError(String errorMessage);
    }

    /**
     * Shows a confirmation dialog before deleting the account
     */
    private void showDeleteAccountConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Delete Account");
        builder.setMessage("Are you sure you want to delete your account? This action cannot be undone.");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            // Proceed with account deletion
            deleteAccount();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // User cancelled the operation
            dialog.dismiss();
        });

        // Create the alert dialog with red buttons for warning
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

        // Set delete button color to red
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_red_dark));
    }

    /**
     * Delete user account via API
     */
    private void deleteAccount() {
        showLoading(true);

        ApiService apiService = RetrofitClient.getApiService();
        apiService.deleteAccount(BEARER_PREFIX + token).enqueue(new Callback<ModelResponse.readNotificationResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.readNotificationResponse> call,
                                   Response<ModelResponse.readNotificationResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    if ("success".equals(response.body().getStatus())) {
                        Toast.makeText(UserProfileActivity.this,
                                "Account deleted successfully", Toast.LENGTH_SHORT).show();

                        // Navigate to login screen
                        navigateToLogin();
                    } else {
                        String errorMsg = response.body().getMessage() != null ?
                                response.body().getMessage() : "Failed to delete account";
                        Toast.makeText(UserProfileActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(UserProfileActivity.this,
                            "Error: " + response.code(), Toast.LENGTH_SHORT).show();

                    Log.e(TAG, "Failed to delete account: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.readNotificationResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(UserProfileActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();

                Log.e(TAG, "Network error while deleting account", t);
            }
        });
    }

    /**
     * Navigate to login screen after account deletion
     */
    private void navigateToLogin() {
        // Clear all activities and start fresh with login
        Intent intent = new Intent(UserProfileActivity.this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}