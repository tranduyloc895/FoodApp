package fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.concurrent.atomic.AtomicInteger;

import adapter.Profile_SavedAdapter;
import api.ModelResponse;
import api.ApiService;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Profile_SavedFragment extends Fragment {
    private static final String TAG = "Profile_SavedFragment";
    private TextView titleTextView;
    private RecyclerView recyclerView;
    private Profile_SavedAdapter adapter;
    private String token;
    private String currentUserId;
    private List<ModelResponse.RecipeResponse.Recipe> savedRecipes;
    private View loadingOverlay;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_saved, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.rv_saved_profile);
        titleTextView = view.findViewById(R.id.titleTextView);
        // Check if loading overlay exists in the layout
        loadingOverlay = view.findViewById(R.id.loading_overlay);

        savedRecipes = new ArrayList<>();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        if (extractToken()) {
            // Show loading if available
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(View.VISIBLE);
            }
            fetchUserProfile();
        }
    }

    private boolean extractToken() {
        if (getActivity() == null) return false;

        token = getActivity().getIntent().getStringExtra("token");
        if (token == null || token.isEmpty()) {
            Toast.makeText(requireContext(), "Invalid token!", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void fetchUserProfile() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.UserResponse> call = apiService.getUserInfo("Bearer " + token);

        call.enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Response<ModelResponse.UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUserId = response.body().getData().getUser().getId();
                    fetchSavedRecipes();
                } else {
                    hideLoading();
                    Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Throwable t) {
                hideLoading();
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchSavedRecipes() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.RecipeResponse> call = apiService.getSavedRecipes("Bearer " + token);

        call.enqueue(new Callback<ModelResponse.RecipeResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Response<ModelResponse.RecipeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    savedRecipes = response.body().getData().getRecipes();

                    if (savedRecipes == null || savedRecipes.isEmpty()) {
                        hideLoading();
                        Toast.makeText(requireContext(), "Bạn chưa lưu bất kỳ công thức nào.", Toast.LENGTH_SHORT).show();
                    } else {
                        // Fetch ratings for each recipe before displaying
                        fetchRatingsForRecipes(savedRecipes);
                    }
                } else {
                    hideLoading();
                    Toast.makeText(requireContext(), "Không thể tải danh sách công thức đã lưu.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Throwable t) {
                hideLoading();
                Toast.makeText(requireContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Fetch ratings for a list of recipes
     * @param recipes List of recipes to get ratings for
     */
    private void fetchRatingsForRecipes(List<ModelResponse.RecipeResponse.Recipe> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            hideLoading();
            return;
        }

        final AtomicInteger pendingRatings = new AtomicInteger(recipes.size());

        Log.d(TAG, "Fetching ratings for " + recipes.size() + " saved recipes");
        ApiService apiService = RetrofitClient.getApiService();

        for (ModelResponse.RecipeResponse.Recipe recipe : recipes) {
            String recipeId = recipe.getId();

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
                            Log.d(TAG, "Recipe: " + recipe.getTitle() +
                                    " - Average Rating: " + ratingData.getAverageRating() +
                                    " - Total Ratings: " + ratingData.getTotalRatings());

                            // Update recipe with rating information
                            recipe.setAverageRating(ratingData.getAverageRating());
                        } else {
                            Log.e(TAG, "Failed to get ratings for recipe: " + recipe.getTitle());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing rating response: " + e.getMessage());
                    } finally {
                        // If all ratings have been fetched (or failed), update the UI
                        if (pendingRatings.decrementAndGet() <= 0) {
                            updateUI();
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ModelResponse.getRatingResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "API Call Failed (Ratings) - Recipe: " + recipe.getTitle() + ", Error: " + t.getMessage());

                    // If all ratings have been fetched (or failed), update the UI
                    if (pendingRatings.decrementAndGet() <= 0) {
                        updateUI();
                    }
                }
            });
        }
    }

    /**
     * Update the UI with recipe data after ratings are fetched
     */
    private void updateUI() {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                hideLoading();
                adapter = new Profile_SavedAdapter(getContext(), savedRecipes, token);
                recyclerView.setAdapter(adapter);

                // Set up click listeners if needed
                adapter.setOnItemClickListener((int position) -> {
                    if (position < 0 || position >= savedRecipes.size()) return;

                    ModelResponse.RecipeResponse.Recipe recipe = savedRecipes.get(position);
                    Intent intent = new Intent(getActivity(), MainRecipe.class);
                    intent.putExtra("recipe_id", recipe.getId());
                    intent.putExtra("token", token);
                    startActivity(intent);
                });
            });
        }
    }

    private void hideLoading() {
        if (loadingOverlay != null && isAdded()) {
            requireActivity().runOnUiThread(() -> {
                loadingOverlay.setVisibility(View.GONE);
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (token != null && !token.isEmpty()) {
            // Show loading if available
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(View.VISIBLE);
            }
            fetchUserProfile();
        }
    }
}