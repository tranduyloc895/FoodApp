package com.example.appfood;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import adapter.SavedRecipesAdapter;
import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ImageSearchResultsActivity extends AppCompatActivity {
    private static final String TAG = "ImageSearchResults";
    private static final String BEARER_PREFIX = "Bearer ";

    private RecyclerView recyclerView;
    private TextView titleTextView;
    private SavedRecipesAdapter adapter;
    private List<ModelResponse.RecipeResponse.Recipe> recipeList = new ArrayList<>();
    private View loadingOverlay;
    private ImageButton backButton;

    private ArrayList<String> recipeIds;
    private ArrayList<String> recipeTitles;
    private ArrayList<Double> similarities;
    private String token;

    // Map to track saved recipes
    private Map<String, Boolean> savedRecipesMap = new HashMap<>();

    // Create a ModelResponse instance to use for recipe creation
    private final ModelResponse modelResponse = new ModelResponse();

    // Track loading operations
    private AtomicInteger pendingLoads = new AtomicInteger(0);

    // Flag to track if we've been to a recipe detail screen
    private boolean hasVisitedRecipeDetail = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_search_results);

        // Initialize views
        initViews();

        // Extract data from intent
        extractDataFromIntent();

        // Load saved recipes first to know which ones the user has saved
        loadSavedRecipes(() -> {
            // Set up RecyclerView
            setupRecyclerView();

            // Load recipe details
            fetchRecipeDetails();
        });
    }

    private void initViews() {
        recyclerView = findViewById(R.id.savedRecipesRecyclerView);
        loadingOverlay = findViewById(R.id.loading_overlay);
        titleTextView = findViewById(R.id.titleTextView);
        backButton = findViewById(R.id.ib_add_recip_back);

        // Update the title
        titleTextView.setText("AI Image Search Results");

        // Setup back button
        backButton.setOnClickListener(v -> finish());
    }

    private void extractDataFromIntent() {
        Intent intent = getIntent();

        if (intent != null) {
            token = intent.getStringExtra("token");
            recipeIds = intent.getStringArrayListExtra("recipe_ids");
            recipeTitles = intent.getStringArrayListExtra("recipe_titles");
            similarities = (ArrayList<Double>) intent.getSerializableExtra("similarities");

            // Log what we received
            Log.d(TAG, "Received token: " + (token != null));
            Log.d(TAG, "Received recipe IDs: " + (recipeIds != null ? recipeIds.size() : 0));
        } else {
            Log.e(TAG, "Intent is null!");
            Toast.makeText(this, "Error loading search results", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupRecyclerView() {
        // Initialize adapter with empty list
        adapter = new SavedRecipesAdapter(this, recipeList) {
            @Override
            public void onBindViewHolder(@NonNull SavedRecipesAdapter.SavedRecipeViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);

                // Get the recipe at this position
                ModelResponse.RecipeResponse.Recipe recipe = recipeList.get(position);
                String recipeId = recipe.getId();

                // Set the bookmark icon based on whether the recipe is saved
                boolean isSaved = isRecipeSaved(recipeId);
                holder.saveButton.setImageResource(isSaved ?
                        R.drawable.ic_bookmark_fill : R.drawable.ic_bookmark_outline);
            }
        };

        adapter.setToken(token);

        // Set up RecyclerView with LinearLayoutManager
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Set click listeners for recipes
        adapter.setOnRecipeClickListener(new SavedRecipesAdapter.OnRecipeClickListener() {
            @Override
            public void onRecipeClick(ModelResponse.RecipeResponse.Recipe recipe, int position) {
                // Set flag that we're visiting a recipe detail
                hasVisitedRecipeDetail = true;
                navigateToRecipeDetails(recipe.getId());
            }

            @Override
            public void onSaveButtonClick(ModelResponse.RecipeResponse.Recipe recipe, int position) {
                // Handle save/unsave functionality
                toggleSavedState(recipe.getId(), position);
            }
        });
    }

    /**
     * Toggles saved/unsaved state for a recipe
     */
    private void toggleSavedState(String recipeId, int position) {
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Please log in to save recipes", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if recipe is currently saved
        boolean isCurrentlySaved = isRecipeSaved(recipeId);

        if (isCurrentlySaved) {
            unsaveRecipe(recipeId, position);
        } else {
            saveRecipe(recipeId, position);
        }
    }

    /**
     * Calls API to save recipe
     */
    private void saveRecipe(String recipeId, int position) {
        showLoading();
        Log.d(TAG, "Saving recipe: " + recipeId);

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.SavedRecipeResponse> call = apiService.saveRecipe(BEARER_PREFIX + token, recipeId);

        call.enqueue(new Callback<ModelResponse.SavedRecipeResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.SavedRecipeResponse> call,
                                   Response<ModelResponse.SavedRecipeResponse> response) {
                hideLoading();

                if (response.isSuccessful() && response.body() != null) {
                    // Update saved state in our map
                    savedRecipesMap.put(recipeId, true);

                    // Update adapter
                    adapter.notifyItemChanged(position);

                    // Show success message
                    Toast.makeText(ImageSearchResultsActivity.this,
                            "Recipe saved successfully", Toast.LENGTH_SHORT).show();
                } else {
                    // Show error message
                    Toast.makeText(ImageSearchResultsActivity.this,
                            "Failed to save recipe", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error saving recipe: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.SavedRecipeResponse> call, Throwable t) {
                hideLoading();
                Toast.makeText(ImageSearchResultsActivity.this,
                        "Network error while saving recipe", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Network error saving: " + t.getMessage());
            }
        });
    }

    /**
     * Calls API to unsave recipe
     */
    private void unsaveRecipe(String recipeId, int position) {
        showLoading();
        Log.d(TAG, "Unsaving recipe: " + recipeId);

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.DeleteSavedRecipeResponse> call =
                apiService.deleteSavedRecipe(BEARER_PREFIX + token, recipeId);

        call.enqueue(new Callback<ModelResponse.DeleteSavedRecipeResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.DeleteSavedRecipeResponse> call,
                                   Response<ModelResponse.DeleteSavedRecipeResponse> response) {
                hideLoading();

                if (response.isSuccessful()) {
                    // Update saved state in our map
                    savedRecipesMap.put(recipeId, false);

                    // Update adapter
                    adapter.notifyItemChanged(position);

                    // Show success message
                    Toast.makeText(ImageSearchResultsActivity.this,
                            "Recipe removed from saved collection", Toast.LENGTH_SHORT).show();
                } else {
                    // Show error message
                    Toast.makeText(ImageSearchResultsActivity.this,
                            "Failed to remove recipe from collection", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error unsaving recipe: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.DeleteSavedRecipeResponse> call, Throwable t) {
                hideLoading();
                Toast.makeText(ImageSearchResultsActivity.this,
                        "Network error while updating recipe", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Network error unsaving: " + t.getMessage());
            }
        });
    }

    /**
     * Checks if a recipe is saved by current user
     */
    public boolean isRecipeSaved(String recipeId) {
        return savedRecipesMap.containsKey(recipeId) && savedRecipesMap.get(recipeId);
    }

    /**
     * Loads saved recipe IDs for the current user
     */
    private void loadSavedRecipes(Runnable onComplete) {
        registerPendingLoad();

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.UserResponse> call = apiService.getUserInfo(BEARER_PREFIX + token);

        call.enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.UserResponse> call,
                                   @NonNull Response<ModelResponse.UserResponse> response) {
                if (response.isSuccessful() && response.body() != null &&
                        response.body().getData() != null &&
                        response.body().getData().getUser() != null) {

                    ModelResponse.UserResponse.User user = response.body().getData().getUser();
                    List<String> savedRecipes = user.getSavedRecipes();

                    // Update saved recipes map
                    savedRecipesMap.clear();
                    if (savedRecipes != null) {
                        for (String recipeId : savedRecipes) {
                            savedRecipesMap.put(recipeId, true);
                        }
                    }

                    Log.d(TAG, "Loaded " + (savedRecipes != null ? savedRecipes.size() : 0) + " saved recipes");

                    // If adapter exists, refresh it to update bookmark icons
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    Log.e(TAG, "Error loading saved recipes: " + response.code());
                }

                completeLoad();
                if (onComplete != null) {
                    onComplete.run();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to load saved recipes: " + t.getMessage());
                completeLoad();
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    private void fetchRecipeDetails() {
        if (recipeIds == null || recipeIds.isEmpty()) {
            hideLoading();
            Toast.makeText(this, "No recipes found", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading();

        // Clear existing recipes
        recipeList.clear();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        int totalRecipes = recipeIds.size();
        final int[] loadedCount = {0}; // Using array for mutable integer in lambda

        for (int i = 0; i < recipeIds.size(); i++) {
            String recipeId = recipeIds.get(i);
            final int index = i; // Final copy of index for lambda

            ApiService apiService = RetrofitClient.getApiService();
            Call<ModelResponse.RecipeDetailResponse> call =
                    apiService.getRecipeDetail(BEARER_PREFIX + token, recipeId);

            call.enqueue(new Callback<ModelResponse.RecipeDetailResponse>() {
                @Override
                public void onResponse(@NonNull Call<ModelResponse.RecipeDetailResponse> call,
                                       @NonNull Response<ModelResponse.RecipeDetailResponse> response) {

                    if (response.isSuccessful() && response.body() != null &&
                            response.body().getData() != null &&
                            response.body().getData().getRecipe() != null) {

                        // Convert RecipeDetail to Recipe for adapter
                        ModelResponse.RecipeDetailResponse.Recipe detailRecipe =
                                response.body().getData().getRecipe();

                        // Create a RecipeResponse instance from ModelResponse to build the Recipe object
                        ModelResponse.RecipeResponse recipeResponse = modelResponse.new RecipeResponse();

                        // Create the Recipe instance from the RecipeResponse instance
                        ModelResponse.RecipeResponse.Recipe recipe = recipeResponse.new Recipe();

                        // Copy all necessary fields
                        recipe.setId(detailRecipe.getId());
                        recipe.setTitle(detailRecipe.getTitle());
                        recipe.setAuthor(detailRecipe.getAuthor());
                        recipe.setImageUrl(detailRecipe.getImageUrl());
                        recipe.setIngredients(detailRecipe.getIngredients());
                        recipe.setInstructions(detailRecipe.getInstructions());
                        recipe.setTime(detailRecipe.getTime());

                        // Set initial rating from similarity score
                        if (index < similarities.size()) {
                            double similarityScore = similarities.get(index);
                            // Using similarity score as initial rating
                            recipe.setAverageRating(similarityScore * 5);
                        }

                        // Add to our list
                        recipeList.add(recipe);

                        // Update the adapter
                        runOnUiThread(() -> adapter.notifyDataSetChanged());

                        // Fetch real rating for this recipe
                        fetchRatingForRecipe(apiService, recipe);
                    }

                    // Count loaded recipes
                    loadedCount[0]++;
                    if (loadedCount[0] >= totalRecipes) {
                        hideLoading();

                        if (recipeList.isEmpty()) {
                            Toast.makeText(ImageSearchResultsActivity.this,
                                    "No recipes found", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ModelResponse.RecipeDetailResponse> call,
                                      @NonNull Throwable t) {
                    Log.e(TAG, "Error fetching recipe details: " + t.getMessage());

                    // Count failures too
                    loadedCount[0]++;
                    if (loadedCount[0] >= totalRecipes) {
                        hideLoading();

                        if (recipeList.isEmpty()) {
                            Toast.makeText(ImageSearchResultsActivity.this,
                                    "Failed to load recipes", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }
    }

    /**
     * Fetches detailed rating for a single recipe
     */
    private void fetchRatingForRecipe(ApiService apiService, ModelResponse.RecipeResponse.Recipe recipe) {
        String recipeId = recipe.getId();
        registerPendingLoad();

        Call<ModelResponse.getRatingResponse> call =
                apiService.getRecipeRating(BEARER_PREFIX + token, recipeId);

        call.enqueue(new Callback<ModelResponse.getRatingResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.getRatingResponse> call,
                                   @NonNull Response<ModelResponse.getRatingResponse> response) {
                if (response.isSuccessful() && response.body() != null &&
                        response.body().getData() != null) {

                    ModelResponse.getRatingResponse.Data ratingData = response.body().getData();

                    // Log the rating values for debugging
                    Log.d(TAG, "Recipe ID: " + recipeId +
                            " - Title: " + recipe.getTitle() +
                            " - Average Rating: " + ratingData.getAverageRating() +
                            " - Total Ratings: " + ratingData.getTotalRatings());

                    // Update with actual rating from the API
                    recipe.setAverageRating(ratingData.getAverageRating());

                    // Update UI
                    runOnUiThread(() -> adapter.notifyDataSetChanged());
                } else {
                    Log.e(TAG, "Failed to get rating for recipe ID: " + recipeId);
                }

                completeLoad();
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.getRatingResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "API Call Failed (Rating) - Recipe ID: " + recipeId + ", Error: " + t.getMessage());
                completeLoad();
            }
        });
    }

    /**
     * Completely refreshes all recipe data from scratch
     */
    private void refreshAllData() {
        Log.d(TAG, "Refreshing all recipe data");

        // Show loading
        showLoading();
        pendingLoads.set(0); // Reset counter

        // Load saved recipes first, then reload recipe details
        loadSavedRecipes(() -> {
            // Then reload recipe details
            fetchRecipeDetails();
        });

        // Reset flag
        hasVisitedRecipeDetail = false;
    }

    private void navigateToRecipeDetails(String recipeId) {
        Intent intent = new Intent(this, MainRecipe.class);
        intent.putExtra("recipe_id", recipeId);
        intent.putExtra("token", token);
        startActivity(intent);
    }

    private void showLoading() {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.GONE);
        }
    }

    /**
     * Registers a pending load operation
     */
    private void registerPendingLoad() {
        pendingLoads.incrementAndGet();
        showLoading();
    }

    /**
     * Completes a loading operation and hides overlay if all are done
     */
    private void completeLoad() {
        if (pendingLoads.decrementAndGet() <= 0) {
            hideLoading();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If we're returning from a recipe detail screen, do a full refresh
        if (hasVisitedRecipeDetail) {
            refreshAllData();
        } else {
            // Otherwise just refresh saved recipes to update bookmark icons
            loadSavedRecipes(null);
        }
    }

    /**
     * Save token to shared preferences for backup access
     */
    private void saveTokenToPreferences() {
        getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("token", token)
                .apply();
    }
}