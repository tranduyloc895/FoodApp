package fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appfood.MainRecipe;
import com.example.appfood.R;

import java.util.ArrayList;
import java.util.List;

import adapter.SavedRecipesAdapter;
import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SavedRecipeFragment extends Fragment {
    private static final String TAG = "SavedRecipeFragment";

    private RecyclerView recyclerView;
    private TextView titleTextView;
    private ImageButton btnBack;
    private SavedRecipesAdapter adapter;
    private List<ModelResponse.RecipeResponse.Recipe> savedRecipes;
    private String token;

    // Loading overlay
    private View loadingOverlay;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_savedrecipe, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get token from SharedPreferences or Arguments
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        token = sharedPreferences.getString("token", "");

        if (token.isEmpty() && getArguments() != null) {
            token = getArguments().getString("token", "");
        }

        if (token.isEmpty() && getActivity() != null) {
            token = getActivity().getIntent().getStringExtra("token");
        }

        // Initialize loading overlay
        loadingOverlay = view.findViewById(R.id.loading_overlay);

        // Initialize views
        recyclerView = view.findViewById(R.id.savedRecipesRecyclerView);
        titleTextView = view.findViewById(R.id.titleTextView);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        savedRecipes = new ArrayList<>();
        adapter = new SavedRecipesAdapter(getContext(), savedRecipes);

        adapter.setOnRecipeClickListener(new SavedRecipesAdapter.OnRecipeClickListener() {
            @Override
            public void onRecipeClick(ModelResponse.RecipeResponse.Recipe recipe, int position) {
                // Navigate to recipe details when clicked
                if (recipe != null && recipe.getId() != null) {
                    navigateToRecipeDetails(recipe.getId());
                }
            }

            @Override
            public void onSaveButtonClick(ModelResponse.RecipeResponse.Recipe recipe, int position) {
                // Call our new deleteSavedRecipe method
                if (recipe != null && recipe.getId() != null) {
                    deleteSavedRecipe(recipe.getId(), position);
                }
            }
        });

        recyclerView.setAdapter(adapter);

        // Show loading before loading data
        showLoading();

        // Load saved recipes data
        loadSavedRecipes();
    }


    /**
     * Show loading overlay
     */
    private void showLoading() {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Hide loading overlay
     */
    private void hideLoading() {
        if (loadingOverlay != null && isAdded()) {
            loadingOverlay.setVisibility(View.GONE);
        }
    }

    private void loadSavedRecipes() {
        if (token == null || token.isEmpty()) {
            Toast.makeText(getContext(), "Bạn cần đăng nhập để xem công thức đã lưu", Toast.LENGTH_SHORT).show();
            hideLoading();
            return;
        }

        ApiService apiService = RetrofitClient.getApiService();
        String authHeader = "Bearer " + token;
        Log.d(TAG, "Making API call with token");

        Call<ModelResponse.RecipeResponse> call = apiService.getSavedRecipes(authHeader);

        call.enqueue(new Callback<ModelResponse.RecipeResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.RecipeResponse> call, Response<ModelResponse.RecipeResponse> response) {
                hideLoading();

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "API response successful");
                    ModelResponse.RecipeResponse recipeResponse = response.body();

                    // Handle case when data is null - treat as empty list
                    if (recipeResponse.getData() == null) {
                        Log.d(TAG, "Recipe response data is null - showing empty state");
                        showEmptyState();
                        return;
                    }

                    // Handle case when recipes list is null - treat as empty list
                    if (recipeResponse.getData().getRecipes() == null) {
                        Log.d(TAG, "Recipes list is null - showing empty state");
                        showEmptyState();
                        return;
                    }

                    // Process recipes normally
                    savedRecipes.clear();
                    List<ModelResponse.RecipeResponse.Recipe> recipes = recipeResponse.getData().getRecipes();
                    Log.d(TAG, "Received " + recipes.size() + " saved recipes");

                    savedRecipes.addAll(recipes);
                    adapter.updateData(savedRecipes);

                    // Process recipes to get author names
                    processRecipesForAuthorNames(savedRecipes);

                    // Fetch ratings for all recipes
                    fetchRatingsForRecipes(savedRecipes);

                    // Check if the list is empty
                    if (savedRecipes.isEmpty()) {
                        Log.d(TAG, "Saved recipes list is empty - showing empty state");
                        showEmptyState();
                    }
                } else {
                    // Only show error if the response isn't successful
                    int code = response.code();
                    Log.e(TAG, "API error: " + code);

                    // For 404 errors, it could mean no recipes are found - show empty state
                    if (code == 404) {
                        Log.d(TAG, "404 error - likely no saved recipes - showing empty state");
                        showEmptyState();
                    }
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.RecipeResponse> call, Throwable t) {
                hideLoading();
                Log.e(TAG, "API call failure: " + t.getMessage());
                showError("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    /**
     * Fetch ratings for a list of recipes
     * @param recipes List of recipes to get ratings for
     */
    private void fetchRatingsForRecipes(List<ModelResponse.RecipeResponse.Recipe> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return;
        }

        ApiService apiService = RetrofitClient.getApiService();

        for (ModelResponse.RecipeResponse.Recipe recipe : recipes) {
            String recipeId = recipe.getId();

            // Don't show loading here to keep your existing loading behavior
            Call<ModelResponse.getRatingResponse> call =
                    apiService.getRecipeRating("Bearer " + token, recipeId);

            call.enqueue(new Callback<ModelResponse.getRatingResponse>() {
                @Override
                public void onResponse(@NonNull Call<ModelResponse.getRatingResponse> call,
                                       @NonNull Response<ModelResponse.getRatingResponse> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null &&
                                response.body().getData() != null) {

                            ModelResponse.getRatingResponse.Data ratingData = response.body().getData();

                            // Log the rating values for debugging
                            Log.d(TAG, "Recipe ID: " + recipeId +
                                    " - Title: " + recipe.getTitle() +
                                    " - Average Rating: " + ratingData.getAverageRating() +
                                    " - Total Ratings: " + ratingData.getTotalRatings());

                            // Update recipe with rating information
                            recipe.setAverageRating(ratingData.getAverageRating());

                            // Update UI for this specific recipe
                            if (isAdded() && adapter != null) {
                                requireActivity().runOnUiThread(() -> {
                                    int position = savedRecipes.indexOf(recipe);
                                    if (position >= 0) {
                                        adapter.notifyItemChanged(position);
                                    }
                                });
                            }
                        } else {
                            Log.e(TAG, "Failed to get ratings for Recipe ID: " + recipeId);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing rating: " + e.getMessage());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ModelResponse.getRatingResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "API Call Failed (Ratings) - Recipe ID: " + recipeId + ", Error: " + t.getMessage());
                }
            });
        }
    }

    private void showEmptyState() {
        Toast.makeText(getContext(), "Không có công thức nào đã lưu", Toast.LENGTH_SHORT).show();
    }

    private void showError(String message) {
        Log.e(TAG, "Error: " + message);
    }

    public static SavedRecipeFragment newInstance(String token) {
        SavedRecipeFragment fragment = new SavedRecipeFragment();
        Bundle args = new Bundle();
        args.putString("token", token);
        fragment.setArguments(args);
        return fragment;
    }

    private void navigateToRecipeDetails(String recipeId) {
        Intent intent = new Intent(requireContext(), MainRecipe.class);
        intent.putExtra("recipe_id", recipeId);
        intent.putExtra("token", token);
        startActivity(intent);
    }

    /**
     * Fetch user information by user ID
     * @param userId User ID to fetch info for
     * @param callback Callback to handle the response
     */
    private void getUserInfoById(String userId, OnUserInfoFetchedCallback callback) {
        if (userId == null || userId.isEmpty() || token == null || token.isEmpty()) {
            callback.onError("Invalid user ID or token");
            return;
        }

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.UserResponse> call = apiService.getUserById("Bearer " + token, userId);

        call.enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.UserResponse> call, Response<ModelResponse.UserResponse> response) {
                if (response.isSuccessful() && response.body() != null &&
                        response.body().getData() != null &&
                        response.body().getData().getUser() != null) {

                    ModelResponse.UserResponse.User user = response.body().getData().getUser();
                    callback.onUserInfoFetched(user);
                } else {
                    callback.onError("Failed to load user info");
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.UserResponse> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    /**
     * Interface for user info callbacks
     */
    public interface OnUserInfoFetchedCallback {
        void onUserInfoFetched(ModelResponse.UserResponse.User user);
        void onError(String errorMessage);
    }

    /**
     * Process recipes to fetch author names
     * @param recipes List of recipes to process
     */
    private void processRecipesForAuthorNames(List<ModelResponse.RecipeResponse.Recipe> recipes) {
        for (ModelResponse.RecipeResponse.Recipe recipe : recipes) {
            String author = recipe.getAuthor();

            // Check if author is not "helenrecipes" - it's likely a user ID
            if (author != null && !author.equals("helenrecipes")) {
                // Use author ID to fetch user info
                getUserInfoById(author, new OnUserInfoFetchedCallback() {
                    @Override
                    public void onUserInfoFetched(ModelResponse.UserResponse.User user) {
                        // Update the recipe with the real author name
                        String realName = user.getName();
                        // We can't modify recipe.author directly as it might be final/immutable
                        // So we'll use a workaround - store the name in a temp field
                        recipe.setAuthor(realName);

                        // Update the UI
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Error fetching author info: " + errorMessage);
                    }
                });
            }
        }
    }

    /**
     * Delete a saved recipe
     * @param recipeId ID of the recipe to unsave
     * @param position Position in the adapter
     */
    private void deleteSavedRecipe(String recipeId, int position) {
        if (token == null || token.isEmpty() || recipeId == null || recipeId.isEmpty()) {
            Toast.makeText(getContext(), "Cannot unsave recipe: Invalid token or recipe ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        showLoading();

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.DeleteSavedRecipeResponse> call = apiService.deleteSavedRecipe("Bearer " + token, recipeId);

        call.enqueue(new Callback<ModelResponse.DeleteSavedRecipeResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.DeleteSavedRecipeResponse> call, Response<ModelResponse.DeleteSavedRecipeResponse> response) {
                hideLoading();

                if (response.isSuccessful()) {
                    // Remove the recipe from the list
                    if (position >= 0 && position < savedRecipes.size()) {
                        savedRecipes.remove(position);
                        adapter.notifyItemRemoved(position);

                        // Show empty state if no more recipes
                        if (savedRecipes.isEmpty()) {
                            showEmptyState();
                        }

                        // Show success message
                        Toast.makeText(getContext(), "Recipe removed from saved collection", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Successfully removed recipe from saved collection");
                    }
                } else {
                    // Show error message
                    Toast.makeText(getContext(), "Failed to remove recipe from collection", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error removing recipe: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.DeleteSavedRecipeResponse> call, Throwable t) {
                hideLoading();
                Toast.makeText(getContext(), "Network error while removing recipe", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Network error: " + t.getMessage());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "SavedRecipeFragment onResume called");

        // Always refresh data when returning to this fragment
        if (isAdded() && token != null && !token.isEmpty() && adapter != null) {
            Log.d(TAG, "Refreshing saved recipes on resume");
            refreshSavedRecipes();
        }
    }

    /**
     * Refresh saved recipes with fresh ratings
     * This method ensures we get updated rating data
     */
    private void refreshSavedRecipes() {
        showLoading();

        ApiService apiService = RetrofitClient.getApiService();
        String authHeader = "Bearer " + token;

        Call<ModelResponse.RecipeResponse> call = apiService.getSavedRecipes(authHeader);

        call.enqueue(new Callback<ModelResponse.RecipeResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.RecipeResponse> call, Response<ModelResponse.RecipeResponse> response) {
                if (response.isSuccessful() && response.body() != null &&
                        response.body().getData() != null &&
                        response.body().getData().getRecipes() != null) {

                    // Update our recipe list with the freshly loaded recipes
                    savedRecipes.clear();
                    List<ModelResponse.RecipeResponse.Recipe> recipes = response.body().getData().getRecipes();
                    savedRecipes.addAll(recipes);

                    // Update adapter with the new recipe list
                    adapter.updateData(savedRecipes);

                    // IMPORTANT: Fetch fresh ratings for all recipes
                    // This will ensure we have the latest ratings
                    if (!savedRecipes.isEmpty()) {
                        refreshAllRecipeRatings(savedRecipes);
                    } else {
                        hideLoading();
                        showEmptyState();
                    }

                    // Also update author names
                    processRecipesForAuthorNames(savedRecipes);

                } else {
                    hideLoading();
                    showError("Không thể tải danh sách công thức đã lưu");
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.RecipeResponse> call, Throwable t) {
                hideLoading();
                showError("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    /**
     * Refresh ratings for all recipes with a dedicated counter
     * @param recipes List of recipes to refresh ratings for
     */
    private void refreshAllRecipeRatings(List<ModelResponse.RecipeResponse.Recipe> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            hideLoading();
            return;
        }

        final int[] completedRequests = {0};
        final int totalRequests = recipes.size();

        Log.d(TAG, "Refreshing ratings for " + totalRequests + " recipes");

        ApiService apiService = RetrofitClient.getApiService();

        for (ModelResponse.RecipeResponse.Recipe recipe : recipes) {
            String recipeId = recipe.getId();

            Call<ModelResponse.getRatingResponse> call =
                    apiService.getRecipeRating("Bearer " + token, recipeId);

            call.enqueue(new Callback<ModelResponse.getRatingResponse>() {
                @Override
                public void onResponse(@NonNull Call<ModelResponse.getRatingResponse> call,
                                       @NonNull Response<ModelResponse.getRatingResponse> response) {
                    if (response.isSuccessful() && response.body() != null &&
                            response.body().getData() != null) {

                        ModelResponse.getRatingResponse.Data ratingData = response.body().getData();

                        double newRating = ratingData.getAverageRating();
                        String recipeTitle = recipe.getTitle();

                        Log.d(TAG, "Updated rating for " + recipeTitle + ": " + newRating);

                        // Update recipe with fresh rating data
                        recipe.setAverageRating(newRating);

                        // Update UI for this specific recipe
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                int position = savedRecipes.indexOf(recipe);
                                if (position >= 0) {
                                    adapter.notifyItemChanged(position);
                                }
                            });
                        }
                    }

                    // Count this request as completed
                    completedRequests[0]++;

                    // If all requests are completed, hide loading
                    if (completedRequests[0] >= totalRequests) {
                        hideLoading();
                        Log.d(TAG, "All " + totalRequests + " rating refreshes completed");
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ModelResponse.getRatingResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "Failed to refresh rating for recipe: " + t.getMessage());

                    // Count failed request as completed
                    completedRequests[0]++;

                    // If all requests are completed, hide loading
                    if (completedRequests[0] >= totalRequests) {
                        hideLoading();
                    }
                }
            });
        }
    }
}