package com.example.appfood;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainRecipe extends AppCompatActivity {
    private static final String TAG = "MainRecipe";

    // Recipe data
    private String title;
    private String author;
    private String image_url;
    private String url_avatar;
    private String country;
    private List<String> ingredients;
    private List<String> instructions;
    private double averageRating;
    private int reviewCount;
    private String token;
    private String recipeId;
    private String recipeTime;
    private boolean isRecipeSaved = false; // Track if recipe is saved

    // Loading counter to track multiple API calls
    private AtomicInteger loadingCounter = new AtomicInteger(0);

    // UI elements
    private ImageButton btnBack, btnMore;
    private CardView cardImage;
    private TextView tvRating, tvTime, tvRecipeTitle, tvReviews;
    private CircleImageView imgProfile;
    private TextView tvUserName, tvUserLocation, tvItemCount;
    private Button btnIngredient, btnProcedure;
    private NestedScrollView scrollIngredients, scrollProcedure;
    private FrameLayout loadingOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_recipe);

        // Get recipeId and token from intent
        recipeId = getIntent().getStringExtra("recipe_id");
        token = getIntent().getStringExtra("token");

        if (recipeId == null || token == null) {
            Toast.makeText(this, "Missing recipe ID or authentication token", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI elements
        initializeViews();

        // Set up click listeners
        setupClickListeners();

        // Show loading state - we're starting multiple API requests
        showLoading(true);

        // Get user profile to load avatar and country
        getUserProfile(token);

        // Check if the recipe is saved
        checkIfRecipeIsSaved();

        // Load recipe data
        getRecipe(token, recipeId);
    }

    /**
     * Check if the current recipe is saved by the user
     */
    private void checkIfRecipeIsSaved() {
        // Increment loading counter
        loadingCounter.incrementAndGet();

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.UserResponse> call = apiService.getUserInfo("Bearer " + token);

        call.enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.UserResponse> call, Response<ModelResponse.UserResponse> response) {
                // Decrement loading counter and check if we should hide loading overlay
                checkAndUpdateLoadingState();

                if (response.isSuccessful() && response.body() != null) {
                    ModelResponse.UserResponse userProfile = response.body();
                    List<String> savedRecipes = userProfile.getData().getUser().getSavedRecipes();

                    // Check if the current recipe is in the saved recipes list
                    if (savedRecipes != null) {
                        isRecipeSaved = savedRecipes.contains(recipeId);
                        Log.d(TAG, "Recipe " + recipeId + " is " + (isRecipeSaved ? "saved" : "not saved"));
                    }
                } else {
                    Log.e(TAG, "Failed to check if recipe is saved: " +
                            (response.code() + " " + (response.errorBody() != null ?
                                    "Error body available" : "No error body")));
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.UserResponse> call, Throwable t) {
                // Decrement loading counter and check if we should hide loading overlay
                checkAndUpdateLoadingState();
                Log.e(TAG, "Network error checking saved status: " + t.getMessage());
            }
        });
    }

    private void initializeViews() {
        // Initialize all UI components from layout
        btnBack = findViewById(R.id.btnBack);
        btnMore = findViewById(R.id.btnMore);
        cardImage = findViewById(R.id.cardImage);
        tvRating = findViewById(R.id.tvRating);
        tvTime = findViewById(R.id.tvTime);
        tvRecipeTitle = findViewById(R.id.tvRecipeTitle);
        tvReviews = findViewById(R.id.tvReviews);
        imgProfile = findViewById(R.id.imgProfile);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserLocation = findViewById(R.id.tvUserLocation);
        tvItemCount = findViewById(R.id.tvItemCount);
        btnIngredient = findViewById(R.id.btnIngredient);
        btnProcedure = findViewById(R.id.btnProcedure);
        scrollIngredients = findViewById(R.id.scrollIngredients);
        scrollProcedure = findViewById(R.id.scrollProcedure);
        loadingOverlay = findViewById(R.id.loadingOverlay);
    }

    private void setupClickListeners() {
        // Back button click
        btnBack.setOnClickListener(v -> finish());

        // More options button click
        btnMore.setOnClickListener(v -> {
            showOptionsMenu();
        });

        // Tab button clicks
        btnIngredient.setOnClickListener(v -> {
            scrollIngredients.setVisibility(View.VISIBLE);
            scrollProcedure.setVisibility(View.GONE);
            btnIngredient.setAlpha(1.0f);
            btnProcedure.setAlpha(0.7f);
            // Update item count to show number of ingredients
            if (ingredients != null) {
                tvItemCount.setText(ingredients.size() + " Items");
            }
        });

        btnProcedure.setOnClickListener(v -> {
            scrollIngredients.setVisibility(View.GONE);
            scrollProcedure.setVisibility(View.VISIBLE);
            btnIngredient.setAlpha(0.7f);
            btnProcedure.setAlpha(1.0f);
            // Update item count to show number of steps
            if (instructions != null) {
                tvItemCount.setText(instructions.size() + " Steps");
            }
        });

        // Add the new click listener for the Reviews TextView
        tvReviews.setOnClickListener(v -> {
            // Create intent to navigate to CommentsRecipe
            Intent intent = new Intent(MainRecipe.this, CommentsRecipe.class);

            // Pass the necessary data
            intent.putExtra("token", token);
            intent.putExtra("recipeId", recipeId);

            // Start the CommentsRecipe activity
            startActivity(intent);
        });
    }

    /**
     * Get user profile information from API to retrieve the avatar URL and country
     * @param token Authentication token
     */
    private void getUserProfile(String token) {
        // Increment loading counter
        loadingCounter.incrementAndGet();

        ApiService apiService = RetrofitClient.getApiService();
        apiService.getUserInfo("Bearer " + token).enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.UserResponse> call, Response<ModelResponse.UserResponse> response) {
                // Decrement loading counter and check if we should hide loading overlay
                checkAndUpdateLoadingState();

                if (response.isSuccessful() && response.body() != null) {
                    ModelResponse.UserResponse userProfile = response.body();
                    url_avatar = userProfile.getData().getUser().getUrlAvatar();
                    country = userProfile.getData().getUser().getCountry();

                    // Load avatar image once we have the URL
                    loadProfileAvatar();

                    // Update the location with country info
                    updateUserLocation();
                } else {
                    showError("Failed to load user profile");
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.UserResponse> call, Throwable t) {
                // Decrement loading counter and check if we should hide loading overlay
                checkAndUpdateLoadingState();

                showError("Network error: " + t.getMessage());
            }
        });
    }

    /**
     * Update the user location TextView with country information
     */
    private void updateUserLocation() {
        if (country != null && !country.isEmpty()) {
            tvUserLocation.setText(country);
        } else {
            tvUserLocation.setText("Location not specified");
        }
    }

    /**
     * Fetch author details by ID from the API
     * @param authorId ID of the author
     */
    private void fetchAuthorDetails(String authorId) {
        // Show loading indicator for the profile section
        if (imgProfile != null) {
            imgProfile.setAlpha(0.5f);
        }

        Log.d(TAG, "Fetching author details for ID: " + authorId);

        ApiService apiService = RetrofitClient.getApiService();
        apiService.getUserById("Bearer " + token, authorId).enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.UserResponse> call, Response<ModelResponse.UserResponse> response) {
                if (imgProfile != null) {
                    imgProfile.setAlpha(1.0f);
                }

                if (response.isSuccessful() && response.body() != null &&
                        response.body().getData() != null &&
                        response.body().getData().getUser() != null) {

                    // Get user data
                    ModelResponse.UserResponse.User user = response.body().getData().getUser();
                    String userName = user.getName();
                    String userAvatar = user.getUrlAvatar();

                    // Update UI with author information
                    tvUserName.setText(userName != null && !userName.isEmpty() ? userName : authorId);

                    // Load the avatar with Glide
                    if (userAvatar != null && !userAvatar.isEmpty()) {
                        Glide.with(MainRecipe.this)
                                .load(userAvatar)
                                .apply(new RequestOptions()
                                        .placeholder(R.drawable.ic_profile)
                                        .error(R.drawable.ic_profile))
                                .into(imgProfile);
                    } else {
                        imgProfile.setImageResource(R.drawable.ic_profile);
                    }

                    // Update user location if country is available
                    String userCountry = user.getCountry();
                    if (userCountry != null && !userCountry.isEmpty()) {
                        tvUserLocation.setText(userCountry);
                    }
                } else {
                    // Default for failed requests
                    imgProfile.setImageResource(R.drawable.ic_profile);
                    Log.e(TAG, "Failed to get author details: " +
                            (response.code() + " - " + (response.errorBody() != null ?
                                    "Error body available" : "No error body")));
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.UserResponse> call, Throwable t) {
                if (imgProfile != null) {
                    imgProfile.setAlpha(1.0f);
                }
                imgProfile.setImageResource(R.drawable.ic_profile);
                Log.e(TAG, "Network error fetching author details: " + t.getMessage());
            }
        });
    }

    /**
     * Load the user's avatar into the profile image view
     */
    private void loadProfileAvatar() {
        // Check if author is "helenrecipes" - set special profile image
        if (author != null && author.equals("helenrecipes")) {
            // Set the Helen-specific profile image
            imgProfile.setImageResource(R.drawable.ic_helen);
        }
        // For other authors, fetch their details using the API
        else if (author != null && !author.isEmpty()) {
            // Use the author ID to fetch user details
            fetchAuthorDetails(author);
        }
        // If no author is available, use the current user's avatar if available
        else if (url_avatar != null && !url_avatar.isEmpty()) {
            Glide.with(this)
                    .load(url_avatar)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile))
                    .into(imgProfile);
        } else {
            // Set default avatar if no URL is available
            imgProfile.setImageResource(R.drawable.ic_profile);
        }
    }

    /**
     * Get recipe details from API
     * @param token Authentication token
     * @param id Recipe ID
     */
    /**
     * Get recipe details from API
     * @param token Authentication token
     * @param id Recipe ID
     */
    public void getRecipe(String token, String id) {
        // Increment loading counter
        loadingCounter.incrementAndGet();

        Log.d(TAG, "Fetching recipe with ID: " + id);

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.RecipeDetailResponse> call = apiService.getRecipeDetail("Bearer " + token, id);

        call.enqueue(new Callback<ModelResponse.RecipeDetailResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.RecipeDetailResponse> call,
                                   @NonNull Response<ModelResponse.RecipeDetailResponse> response) {
                // Decrement loading counter and check if we should hide loading overlay
                checkAndUpdateLoadingState();

                if (response.isSuccessful() && response.body() != null) {
                    ModelResponse.RecipeDetailResponse recipeResponse = response.body();
                    Log.d(TAG, "API Response status: " + recipeResponse.getStatus());

                    if (recipeResponse.getData() != null &&
                            recipeResponse.getData().getRecipe() != null) {

                        ModelResponse.RecipeDetailResponse.Recipe recipe = recipeResponse.getData().getRecipe();

                        // Store recipe data
                        title = recipe.getTitle();
                        author = recipe.getAuthor();
                        image_url = recipe.getImageUrl();
                        ingredients = recipe.getIngredients();
                        instructions = recipe.getInstructions();
                        recipeTime = recipe.getTime();

                        // Update UI with recipe data (except rating)
                        updateUI();

                        // Fetch separate rating data using the new API
                        fetchRecipeRating(token, id);
                    } else {
                        showError("Recipe data structure is invalid");
                        Log.e(TAG, "Recipe data structure is invalid: " + response.body());
                    }
                } else {
                    String errorMsg = "Error loading recipe";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += ": " + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error response", e);
                    }
                    showError(errorMsg);
                    Log.e(TAG, errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.RecipeDetailResponse> call, @NonNull Throwable t) {
                // Decrement loading counter and check if we should hide loading overlay
                checkAndUpdateLoadingState();

                showError("Network error: " + t.getMessage());
                Log.e(TAG, "API call failed", t);
            }
        });
    }

    /**
     * Fetch rating information for the recipe using the new rating API
     * @param token Authentication token
     * @param id Recipe ID
     */
    private void fetchRecipeRating(String token, String id) {
        // Increment loading counter
        loadingCounter.incrementAndGet();

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.getRatingResponse> call = apiService.getRecipeRating("Bearer " + token, id);

        call.enqueue(new Callback<ModelResponse.getRatingResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.getRatingResponse> call,
                                   @NonNull Response<ModelResponse.getRatingResponse> response) {
                // Decrement loading counter
                checkAndUpdateLoadingState();

                if (response.isSuccessful() && response.body() != null &&
                        response.body().getData() != null) {

                    ModelResponse.getRatingResponse.Data ratingData = response.body().getData();

                    // Update rating information
                    averageRating = ratingData.getAverageRating();
                    reviewCount = ratingData.getTotalRatings();

                    // Log the received rating data
                    Log.d(TAG, "Received rating data: average=" + averageRating +
                            ", totalRatings=" + reviewCount);

                    // Update UI with rating information
                    updateRatingUI();
                } else {
                    Log.e(TAG, "Failed to get rating info: " +
                            (response.code() + " " + (response.errorBody() != null ?
                                    "Error body available" : "No error body")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.getRatingResponse> call, @NonNull Throwable t) {
                // Decrement loading counter
                checkAndUpdateLoadingState();
                Log.e(TAG, "Network error fetching rating: " + t.getMessage());
            }
        });
    }

    /**
     * Update the UI with recipe rating information
     */
    private void updateRatingUI() {
        runOnUiThread(() -> {
            // Set rating
            tvRating.setText(String.format("%.1f", averageRating));

            // Set reviews count
            tvReviews.setText(String.format("(%d Reviews)", reviewCount));
        });
    }

    /**
     * Update UI with recipe data (excluding rating which is handled separately)
     */

    /**
     * Check if all loading operations are complete and update loading state
     */
    private void checkAndUpdateLoadingState() {
        int count = loadingCounter.decrementAndGet();
        if (count <= 0) {
            // All loads complete, hide loading overlay
            showLoading(false);
        }
    }

    /**
     * Update UI with recipe data
     */
    private void updateUI() {
        // Set recipe title
        tvRecipeTitle.setText(title);

        // Load profile avatar based on author - this will handle setting the username too
        loadProfileAvatar();

        // Set recipe time
        if (recipeTime != null && !recipeTime.isEmpty()) {
            tvTime.setText(recipeTime);
        } else {
            tvTime.setText("No time specified");
        }

        // Set recipe image using Glide
        if (image_url != null && !image_url.isEmpty()) {
            // Get the FrameLayout within the CardView
            FrameLayout frameLayout = null;
            if (cardImage.getChildCount() > 0 && cardImage.getChildAt(0) instanceof FrameLayout) {
                frameLayout = (FrameLayout) cardImage.getChildAt(0);
            }

            if (frameLayout != null) {
                // Create an ImageView for the recipe image
                ImageView recipeImage = new ImageView(this);
                recipeImage.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
                recipeImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

                // Add the ImageView at index 0 to be below other elements
                frameLayout.addView(recipeImage, 0);

                // Load image with Glide
                Glide.with(this)
                        .load(image_url)
                        .apply(new RequestOptions()
                                .centerCrop()
                                .placeholder(R.drawable.placeholder_image)
                                .error(R.drawable.error_image))
                        .into(recipeImage);
            }
        }

        // Update ingredients count - default view is ingredients
        if (ingredients != null) {
            tvItemCount.setText(ingredients.size() + " Items");
            updateIngredientsView();
        }

        // Update procedures
        if (instructions != null) {
            updateProcedureView();
        }
    }

    /**
     * Create and display the ingredients list
     */
    private void updateIngredientsView() {
        // Get the container LinearLayout inside the ScrollView
        LinearLayout container = (LinearLayout) scrollIngredients.getChildAt(0);

        // Clear existing views
        container.removeAllViews();

        // Add each ingredient as a CardView
        for (String ingredient : ingredients) {
            // Create a new CardView for this ingredient
            CardView cardView = new CardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, dpToPx(12));
            cardView.setLayoutParams(cardParams);
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            cardView.setRadius(dpToPx(12));
            cardView.setCardElevation(0);

            // Create a LinearLayout to hold text
            LinearLayout layout = new LinearLayout(this);
            layout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            layout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setGravity(android.view.Gravity.CENTER_VERTICAL);

            // Create text view for ingredient name
            TextView tvName = new TextView(this);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            textParams.setMargins(dpToPx(16), 0, 0, 0);
            tvName.setLayoutParams(textParams);
            tvName.setText(ingredient);
            tvName.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            tvName.setTextSize(16);

            // Add views to layout
            layout.addView(tvName);
            cardView.addView(layout);
            container.addView(cardView);
        }
    }

    /**
     * Create and display the procedure steps
     */
    private void updateProcedureView() {
        // Get the container LinearLayout inside the ScrollView
        LinearLayout container = (LinearLayout) scrollProcedure.getChildAt(0);

        // Clear existing views
        container.removeAllViews();

        // Add each instruction as a step
        for (int i = 0; i < instructions.size(); i++) {
            // Step header
            TextView stepHeader = new TextView(this);
            stepHeader.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            stepHeader.setText("Step " + (i+1));
            stepHeader.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            stepHeader.setTextSize(16);
            stepHeader.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams headerParams = (LinearLayout.LayoutParams) stepHeader.getLayoutParams();
            headerParams.bottomMargin = dpToPx(8);
            stepHeader.setLayoutParams(headerParams);

            // Step content CardView
            CardView cardView = new CardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, dpToPx(16));
            cardView.setLayoutParams(cardParams);
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            cardView.setRadius(dpToPx(12));
            cardView.setCardElevation(0);

            // Create text view for step instruction
            TextView tvInstruction = new TextView(this);
            tvInstruction.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            tvInstruction.setText(instructions.get(i));
            tvInstruction.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            tvInstruction.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

            // Add views
            cardView.addView(tvInstruction);
            container.addView(stepHeader);
            container.addView(cardView);
        }
    }

    /**
     * Convert dp to pixels
     */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * Show or hide loading state
     * @param isLoading true to show loading overlay, false to hide it
     */
    private void showLoading(boolean isLoading) {
        runOnUiThread(() -> {
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Handle back button options
     */
    private void showOptionsMenu() {
        // Create the dialog
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_recipe_options);

        // Set dialog window width and position
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.END | Gravity.TOP);

            // Set margin from top and right
            WindowManager.LayoutParams params = window.getAttributes();
            params.x = 50; // distance from right
            params.y = 120; // distance from top
            window.setAttributes(params);
        }

        // Get option views
        View layoutShare = dialog.findViewById(R.id.layoutShare);
        View layoutRate = dialog.findViewById(R.id.layoutRate);
        View layoutReview = dialog.findViewById(R.id.layoutReview);
        View layoutUnsave = dialog.findViewById(R.id.layoutUnsave);
        TextView tvSaveOption = dialog.findViewById(R.id.tvSaveOption);
        ImageView ivSaveIcon = dialog.findViewById(R.id.ivSaveIcon);

        // Update save/unsave text and icon based on current state
        if (isRecipeSaved) {
            tvSaveOption.setText("Unsave");
            // Change icon if needed
            if (ivSaveIcon != null) {
                ivSaveIcon.setImageResource(R.drawable.ic_bookmark_fill);
            }
        } else {
            tvSaveOption.setText("Save");
            // Change icon if needed
            if (ivSaveIcon != null) {
                ivSaveIcon.setImageResource(R.drawable.ic_bookmark_outline);
            }
        }

        // Set click listeners for each option
        layoutShare.setOnClickListener(v -> {
            dialog.dismiss();
            shareRecipe();
        });

        layoutRate.setOnClickListener(v -> {
            dialog.dismiss();
            showRatingDialog();
        });

        layoutReview.setOnClickListener(v -> {
            dialog.dismiss();
            navigateToComments();
        });

        layoutUnsave.setOnClickListener(v -> {
            dialog.dismiss();
            toggleSaveRecipe();
        });

        dialog.show();
    }

    // Implementation of the actions
    private void shareRecipe() {
        // TODO: Implement share functionality
        Toast.makeText(this, "Share Recipe functionality", Toast.LENGTH_SHORT).show();
    }

    /**
     * Toggle between saving and unsaving the recipe
     */
//    private void toggleSaveRecipe() {
//        showLoading(true);
//
//        ApiService apiService = RetrofitClient.getApiService();
//
//        if (isRecipeSaved) {
//            // Recipe is saved, so unsave it
//            Call<ModelResponse.DeleteSavedRecipeResponse> call =
//                    apiService.deleteSavedRecipe("Bearer " + token, recipeId);
//
//            call.enqueue(new Callback<ModelResponse.DeleteSavedRecipeResponse>() {
//                @Override
//                public void onResponse(Call<ModelResponse.DeleteSavedRecipeResponse> call,
//                                       Response<ModelResponse.DeleteSavedRecipeResponse> response) {
//                    showLoading(false);
//
//                    if (response.isSuccessful()) {
//                        isRecipeSaved = false;
//                        Toast.makeText(MainRecipe.this, "Recipe removed from saved collection",
//                                Toast.LENGTH_SHORT).show();
//                    } else {
//                        showError("Failed to unsave recipe: " + response.code());
//                    }
//                }
//
//                @Override
//                public void onFailure(Call<ModelResponse.DeleteSavedRecipeResponse> call, Throwable t) {
//                    showLoading(false);
//                    showError("Network error while unsaving recipe");
//                    Log.e(TAG, "Network error: " + t.getMessage());
//                }
//            });
//        } else {
//            // Recipe is not saved, so save it
//            Call<ModelResponse.SavedRecipeResponse> call =
//                    apiService.saveRecipe("Bearer " + token, recipeId);
//
//            call.enqueue(new Callback<ModelResponse.SavedRecipeResponse>() {
//                @Override
//                public void onResponse(Call<ModelResponse.SavedRecipeResponse> call,
//                                       Response<ModelResponse.SavedRecipeResponse> response) {
//                    showLoading(false);
//
//                    if (response.isSuccessful() && response.body() != null) {
//                        isRecipeSaved = true;
//                        Toast.makeText(MainRecipe.this, "Recipe saved successfully",
//                                Toast.LENGTH_SHORT).show();
//                    } else {
//                        showError("Failed to save recipe: " + response.code());
//                    }
//                }
//
//                @Override
//                public void onFailure(Call<ModelResponse.SavedRecipeResponse> call, Throwable t) {
//                    showLoading(false);
//                    showError("Network error while saving recipe");
//                    Log.e(TAG, "Network error: " + t.getMessage());
//                }
//            });
//        }
//    }

    /**
     * Toggle between saving and unsaving the recipe
     */
    private void toggleSaveRecipe() {
        // Increment loading counter to show loading overlay
        loadingCounter.incrementAndGet();

        ApiService apiService = RetrofitClient.getApiService();

        if (isRecipeSaved) {
            // Recipe is saved, so unsave it
            Call<ModelResponse.DeleteSavedRecipeResponse> call =
                    apiService.deleteSavedRecipe("Bearer " + token, recipeId);

            call.enqueue(new Callback<ModelResponse.DeleteSavedRecipeResponse>() {
                @Override
                public void onResponse(Call<ModelResponse.DeleteSavedRecipeResponse> call,
                                       Response<ModelResponse.DeleteSavedRecipeResponse> response) {
                    // Use the checkAndUpdateLoadingState method instead of direct showLoading(false)
                    checkAndUpdateLoadingState();

                    if (response.isSuccessful()) {
                        isRecipeSaved = false;
                        Toast.makeText(MainRecipe.this, "Recipe removed from saved collection",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        showError("Failed to unsave recipe: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<ModelResponse.DeleteSavedRecipeResponse> call, Throwable t) {
                    // Use the checkAndUpdateLoadingState method instead of direct showLoading(false)
                    checkAndUpdateLoadingState();

                    showError("Network error while unsaving recipe");
                    Log.e(TAG, "Network error: " + t.getMessage());
                }
            });
        } else {
            // Recipe is not saved, so save it
            Call<ModelResponse.SavedRecipeResponse> call =
                    apiService.saveRecipe("Bearer " + token, recipeId);

            call.enqueue(new Callback<ModelResponse.SavedRecipeResponse>() {
                @Override
                public void onResponse(Call<ModelResponse.SavedRecipeResponse> call,
                                       Response<ModelResponse.SavedRecipeResponse> response) {
                    // Use the checkAndUpdateLoadingState method instead of direct showLoading(false)
                    checkAndUpdateLoadingState();

                    if (response.isSuccessful() && response.body() != null) {
                        isRecipeSaved = true;
                        Toast.makeText(MainRecipe.this, "Recipe saved successfully",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        showError("Failed to save recipe: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<ModelResponse.SavedRecipeResponse> call, Throwable t) {
                    // Use the checkAndUpdateLoadingState method instead of direct showLoading(false)
                    checkAndUpdateLoadingState();

                    showError("Network error while saving recipe");
                    Log.e(TAG, "Network error: " + t.getMessage());
                }
            });
        }
    }

    private void showRatingDialog() {
        // Create custom dialog
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_rate_recipe);

        // Set transparent background to get rounded corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Initialize views
        TextView tvTitle = dialog.findViewById(R.id.tvRateTitle);
        ImageView star1 = dialog.findViewById(R.id.star1);
        ImageView star2 = dialog.findViewById(R.id.star2);
        ImageView star3 = dialog.findViewById(R.id.star3);
        ImageView star4 = dialog.findViewById(R.id.star4);
        ImageView star5 = dialog.findViewById(R.id.star5);
        Button btnSendRating = dialog.findViewById(R.id.btnSendRating);
        ProgressBar progressBar = dialog.findViewById(R.id.progressBar);

        // Add ImageViews to array for easier handling
        final ImageView[] stars = {star1, star2, star3, star4, star5};

        // Track the current rating
        final int[] currentRating = {0};

        // Set up click listeners for stars
        for (int i = 0; i < stars.length; i++) {
            final int starIndex = i;
            stars[i].setOnClickListener(v -> {
                // Update the rating
                currentRating[0] = starIndex + 1;

                // Update star UI based on selection
                for (int j = 0; j < stars.length; j++) {
                    stars[j].setSelected(j <= starIndex);
                }

                // Enable the send button
                btnSendRating.setEnabled(true);
                btnSendRating.setBackgroundResource(R.drawable.send_button_enable);
            });
        }

        // Set click listener for send button
        btnSendRating.setOnClickListener(v -> {
            if (currentRating[0] > 0) {
                // Show loading indicator and disable button
                progressBar.setVisibility(View.VISIBLE);
                btnSendRating.setEnabled(false);
                btnSendRating.setText("");  // Remove text while loading
                btnSendRating.setBackgroundResource(R.drawable.send_button_disable);

                // Disable star interactions during loading
                for (ImageView star : stars) {
                    star.setClickable(false);
                }

                // Call API to submit rating
                submitRating(currentRating[0], dialog, progressBar, btnSendRating, stars);
            } else {
                Toast.makeText(MainRecipe.this, "Please select a rating", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void navigateToComments() {
        // Navigate to comments/review screen
        Intent intent = new Intent(MainRecipe.this, CommentsRecipe.class);
        intent.putExtra("token", token);
        intent.putExtra("recipeId", recipeId);
        startActivity(intent);
    }

    /**
     * Submit a rating to the API
     * @param rating The rating value (1-5)
     * @param dialog The dialog instance
     * @param progressBar The progress bar to indicate loading
     * @param sendButton The send button
     * @param stars Array of star ImageViews
     */
    private void submitRating(int rating, Dialog dialog, ProgressBar progressBar,
                              Button sendButton, ImageView[] stars) {
        // Get API service
        ApiService apiService = RetrofitClient.getApiService();

        // Call the rate recipe API
        Call<ModelResponse.RatingResponse> call = apiService.rateRecipe(
                "Bearer " + token,
                recipeId,
                rating);

        call.enqueue(new Callback<ModelResponse.RatingResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.RatingResponse> call, Response<ModelResponse.RatingResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Rating submitted successfully
                    Toast.makeText(MainRecipe.this, "Rating submitted successfully", Toast.LENGTH_SHORT).show();

                    // Update the UI with the new rating if available
                    if (response.body().getData() != null) {
                        averageRating = response.body().getData().getAverageRating();
                        tvRating.setText(String.format("%.1f", averageRating));
                    }

                    // Dismiss the dialog with a slight delay to show success
                    new Handler().postDelayed(dialog::dismiss, 300);
                } else {
                    // Handle error - reset the dialog to allow retrying
                    progressBar.setVisibility(View.GONE);
                    sendButton.setEnabled(true);
                    sendButton.setText("Send");
                    sendButton.setBackgroundResource(R.drawable.send_button_enable);

                    // Re-enable star interactions
                    for (ImageView star : stars) {
                        star.setClickable(true);
                    }

                    // Show error message
                    String errorMsg = "Failed to submit rating";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += ": " + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error response", e);
                    }

                    showError(errorMsg);
                    Log.e(TAG, errorMsg);
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.RatingResponse> call, Throwable t) {
                // Handle network failure - reset the dialog to allow retrying
                progressBar.setVisibility(View.GONE);
                sendButton.setEnabled(true);
                sendButton.setText("Send");
                sendButton.setBackgroundResource(R.drawable.send_button_enable);

                // Re-enable star interactions
                for (ImageView star : stars) {
                    star.setClickable(true);
                }

                // Show error message
                showError("Network error: " + t.getMessage());
                Log.e(TAG, "Network error: " + t.getMessage(), t);
            }
        });
    }
}