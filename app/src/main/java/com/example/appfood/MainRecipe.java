package com.example.appfood;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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

        // Load recipe data
        getRecipe(token, recipeId);
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
     * Load the user's avatar into the profile image view
     */
    private void loadProfileAvatar() {
        if (url_avatar != null && !url_avatar.isEmpty()) {
            Glide.with(this)
                    .load(url_avatar)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile))
                    .into(imgProfile);
        }
    }

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

                        // Calculate the average rating from the ratings list
                        averageRating = recipe.getAverageRating();
                        reviewCount = recipe.getReviewCount();

                        // Update UI with recipe data
                        updateUI();
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

        // Set rating
        tvRating.setText(String.format("%.1f", averageRating));

        // Set reviews count
        tvReviews.setText(String.format("(%d Reviews)", reviewCount));

        // Set user information
        tvUserName.setText(author);

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
            unsaveRecipe();
        });

        dialog.show();
    }

    // Implementation of the actions
    private void shareRecipe() {
        // TODO: Implement share functionality
        Toast.makeText(this, "Share Recipe functionality", Toast.LENGTH_SHORT).show();
    }

    private void showRatingDialog() {
        // TODO: Implement rating dialog
        Toast.makeText(this, "Rate Recipe functionality", Toast.LENGTH_SHORT).show();
    }

    private void navigateToComments() {
        // Navigate to comments/review screen
        Intent intent = new Intent(MainRecipe.this, CommentsRecipe.class);
        intent.putExtra("token", token);
        intent.putExtra("recipeId", recipeId);
        startActivity(intent);
    }

    private void unsaveRecipe() {
        // TODO: Implement unsave functionality
        Toast.makeText(this, "Recipe unsaved", Toast.LENGTH_SHORT).show();
    }

}