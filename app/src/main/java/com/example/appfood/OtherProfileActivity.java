package com.example.appfood;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import adapter.OtherProfilePagerAdapter;
import api.ApiService;
import api.RetrofitClient;
import api.ModelResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class OtherProfileActivity extends AppCompatActivity {
    private static final String TAG = "OtherProfileActivity";
    private TextView tvProfileName, tvNumberUploadedRecipes, tvNumberSavedRecipes, tvCountry, tvLevel, tvUploaded;
    private String token, currentUserId;
    private ImageView ivProfileImage;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ImageButton ibBack;
    private View loadingOverlay;

    // Loading state tracking
    private AtomicInteger pendingLoads = new AtomicInteger(0);
    private boolean initialLoadComplete = false;

    private List<String> uploadedRecipeIds = new ArrayList<>();
    private List<String> savedRecipeIds = new ArrayList<>();
    private boolean isUploadedRecipesFetched = false;
    private boolean isSavedRecipesFetched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other_profile);

        initViews();
        setupListeners();

        if (extractToken()) {
            Log.d(TAG, "Final extracted author_id: " + currentUserId);
            Log.d(TAG, "Extracted token: " + token);
            loadProfileData();
        }
    }

    private void initViews() {
        tvProfileName = findViewById(R.id.tv_profile_name);
        tvNumberUploadedRecipes = findViewById(R.id.tv_number_uploaded_recipes);
        tvNumberSavedRecipes = findViewById(R.id.tv_number_saved_recipes);
        tvCountry = findViewById(R.id.tv_country_code);
        tvLevel = findViewById(R.id.tv_profile_level);
        ivProfileImage = findViewById(R.id.iv_profile_picture);
        tabLayout = findViewById(R.id.tl_category);
        viewPager = findViewById(R.id.vp_category);
        ibBack = findViewById(R.id.ib_other_profile_back);
        loadingOverlay = findViewById(R.id.loading_overlay);
        tvUploaded = findViewById(R.id.tv_uploaded_recipes);

        tvUploaded.setSelected(true);
    }

    // Replace the current tab selection listener with this improved version
    private void setupListeners() {
        ibBack.setOnClickListener(v -> {
            Intent intent = new Intent(OtherProfileActivity.this, HomeActivity.class);
            intent.putExtra("token", token);
            intent.putExtra("author_id", currentUserId);
            startActivity(intent);
            finish();
        });

        // Setup tab selection listener - FIXED VERSION
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Log.d(TAG, "Tab selected: " + tab.getPosition());
                // Don't show loading here - let the ViewPager handle it
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Not needed
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                Log.d(TAG, "Tab reselected: " + tab.getPosition());
                refreshData();
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                Log.d(TAG, "ViewPager page selected: " + position);

                // Show brief loading animation for visual feedback
                showBriefLoading();
            }
        });
    }

    /**
     * Shows a brief loading indicator for tab changes
     */
    private void showBriefLoading() {
        // Reset loading state
        pendingLoads.set(0);

        // Show loading
        showLoading();

        // Hide loading after a short delay
        new android.os.Handler().postDelayed(() -> {
            hideLoading();
        }, 300);
    }

    private void loadProfileData() {
        // Reset counter and show loading
        pendingLoads.set(0);
        showLoading();

        // Start data loading
        fetchUserProfile();
        fetchUploadedRecipesCount();
        fetchSavedRecipesCount();
    }

    private boolean extractToken() {
        Intent intent = getIntent();
        token = intent.getStringExtra("token");
        currentUserId = intent.getStringExtra("author_id");

        Log.d(TAG, "Extracted token: " + token);
        Log.d(TAG, "Extracted author_id: " + currentUserId);

        if (token == null || token.isEmpty() || currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "Invalid token or author ID!");
            Toast.makeText(this, "Invalid token or author ID!", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void fetchUserProfile() {
        registerPendingLoad();
        Log.d(TAG, "Fetching user profile");

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.UserResponse> call = apiService.getUserById("Bearer " + token, currentUserId);

        call.enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Response<ModelResponse.UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String userName = response.body().getData().getUser().getName();
                    tvProfileName.setText(userName);

                    String country = response.body().getData().getUser().getCountry();
                    String countryCode = (country != null && !country.isEmpty()) ? country.substring(0, Math.min(3, country.length())).toUpperCase() : "NT118";
                    tvCountry.setText(countryCode);

                    // Load profile image if available
                    String avatarUrl = response.body().getData().getUser().getUrlAvatar();
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        // Load image using Glide
                        Glide.with(OtherProfileActivity.this)
                                .load(avatarUrl)
                                .apply(new RequestOptions()
                                        .placeholder(R.drawable.ic_profile)
                                        .error(R.drawable.ic_profile)
                                        .circleCrop())
                                .into(ivProfileImage);
                    }

                    Log.d(TAG, "User profile fetched successfully: " + userName);
                } else {
                    Log.e(TAG, "Failed to fetch user profile: " + response.code());
                    Toast.makeText(OtherProfileActivity.this, "Failed to load user profile", Toast.LENGTH_SHORT).show();
                }
                completeLoad();
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching user profile: " + t.getMessage());
                Toast.makeText(OtherProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                completeLoad();
            }
        });
    }

    private void fetchUploadedRecipesCount() {
        registerPendingLoad();
        Log.d(TAG, "Fetching uploaded recipes count");

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.RecipeResponse> call = apiService.getAllRecipes("Bearer " + token);

        call.enqueue(new Callback<ModelResponse.RecipeResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Response<ModelResponse.RecipeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ModelResponse.RecipeResponse.Recipe> allRecipes = response.body().getData().getRecipes();

                    uploadedRecipeIds.clear();
                    int uploadedCount = 0;

                    for (ModelResponse.RecipeResponse.Recipe recipe : allRecipes) {
                        Log.d(TAG, "Recipe Author ID: " + recipe.getAuthor());
                        Log.d(TAG, "Current User ID: " + currentUserId);

                        if (recipe.getAuthor().equals(currentUserId)) {
                            uploadedCount++;
                            uploadedRecipeIds.add(recipe.getId());
                        }
                    }

                    Log.d(TAG, "Uploaded recipes count: " + uploadedCount);
                    tvNumberUploadedRecipes.setText(String.valueOf(uploadedCount));

                    tvLevel.setText(uploadedCount == 0 ? "Guest" :
                            uploadedCount < 5 ? "Beginner" :
                                    uploadedCount < 15 ? "Intermediate" :
                                            uploadedCount < 30 ? "Chef" : "Master Chef");

                    Log.d(TAG, "Uploaded Recipe IDs: " + uploadedRecipeIds);

                    isUploadedRecipesFetched = true;
                    checkAndSetupViewPager();
                } else {
                    Log.e(TAG, "Failed to fetch uploaded recipes: " + response.code());
                    Toast.makeText(OtherProfileActivity.this, "Failed to load uploaded recipes", Toast.LENGTH_SHORT).show();
                }
                completeLoad();
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching uploaded recipes: " + t.getMessage());
                Toast.makeText(OtherProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                completeLoad();
            }
        });
    }

    private void fetchSavedRecipesCount() {
        registerPendingLoad();
        Log.d(TAG, "Fetching saved recipes count");

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.UserResponse> call = apiService.getUserById("Bearer " + token, currentUserId);

        call.enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Response<ModelResponse.UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    savedRecipeIds = response.body().getData().getUser().getSavedRecipes();
                    if (savedRecipeIds == null) {
                        savedRecipeIds = new ArrayList<>();
                    }

                    Log.d(TAG, "Saved recipes count: " + savedRecipeIds.size());
                    tvNumberSavedRecipes.setText(String.valueOf(savedRecipeIds.size()));

                    isSavedRecipesFetched = true;
                    checkAndSetupViewPager();
                } else {
                    Log.e(TAG, "Failed to fetch saved recipes: " + response.code());
                    Toast.makeText(OtherProfileActivity.this, "Không thể tải số lượng công thức đã lưu.", Toast.LENGTH_SHORT).show();
                }
                completeLoad();
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching saved recipes: " + t.getMessage());
                Toast.makeText(OtherProfileActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                completeLoad();
            }
        });
    }

    private void checkAndSetupViewPager() {
        Log.d(TAG, "Checking if recipe lists are populated...");

        if (isUploadedRecipesFetched && isSavedRecipesFetched) {

            OtherProfilePagerAdapter adapter = new OtherProfilePagerAdapter(this, currentUserId, uploadedRecipeIds, savedRecipeIds);
            viewPager.setAdapter(adapter);
            Log.d(TAG, "ViewPager Adapter set successfully.");

            new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                SpannableString spannableString;
                switch (position) {
                    case 0:
                        spannableString = new SpannableString("Uploaded");
                        break;
                    case 1:
                        spannableString = new SpannableString("Saved");
                        break;
                    default:
                        spannableString = new SpannableString("");
                }

                spannableString.setSpan(new AbsoluteSizeSpan(16, true), 0, spannableString.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                tab.setText(spannableString);
            }).attach();

            Log.d(TAG, "ViewPager initialized successfully.");
            initialLoadComplete = true;
        } else {
            Log.d(TAG, "Waiting for both lists to be populated...");
        }
    }

    /**
     * Called when activity resumes to refresh data
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called, initialLoadComplete = " + initialLoadComplete);
        if (initialLoadComplete) {
            refreshData();
        }
    }

    /**
     * Refreshes all data
     */
    private void refreshData() {
        // Reset and start fresh
        pendingLoads.set(0);
        isUploadedRecipesFetched = false;
        isSavedRecipesFetched = false;

        Log.d(TAG, "Refreshing profile data");
        showLoading();

        // Each of these methods will call registerPendingLoad()
        fetchUserProfile();
        fetchUploadedRecipesCount();
        fetchSavedRecipesCount();
    }

    /**
     * Shows loading overlay with debugging
     */
    private void showLoading() {
        if (loadingOverlay != null) {
            // Make sure loading overlay is in front
            loadingOverlay.bringToFront();
            loadingOverlay.setVisibility(View.VISIBLE);
            Log.d(TAG, "Loading overlay shown - Pending loads: " + pendingLoads.get());
        }
    }

    /**
     * Hides loading overlay with debugging
     */
    private void hideLoading() {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.GONE);
            Log.d(TAG, "Loading overlay hidden");
        }
    }

    /**
     * Registers a pending load operation
     */
    private void registerPendingLoad() {
        int count = pendingLoads.incrementAndGet();
        Log.d(TAG, "Load operation registered - Pending loads: " + count);
        showLoading();
    }

    /**
     * Completes a loading operation and hides overlay if all are done
     */
    private void completeLoad() {
        int remaining = pendingLoads.decrementAndGet();
        Log.d(TAG, "Load operation completed - Remaining loads: " + remaining);

        if (remaining <= 0) {
            // Ensure we don't go below 0
            pendingLoads.set(0);
            hideLoading();
        }
    }
}