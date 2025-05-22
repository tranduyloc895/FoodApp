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
                // Handle unsave functionality if needed
                if (recipe != null && recipe.getId() != null) {
                    // Implement unsave functionality if desired
                    // For now, just show a toast
                    Toast.makeText(getContext(), "Recipe unsaved", Toast.LENGTH_SHORT).show();
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
                // Hide loading when response is received
                hideLoading();

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "API response successful");
                    ModelResponse.RecipeResponse recipeResponse = response.body();
                    if (recipeResponse.getData() != null && recipeResponse.getData().getRecipes() != null) {
                        savedRecipes.clear();

                        // Process recipes to calculate average ratings if needed
                        List<ModelResponse.RecipeResponse.Recipe> recipes = recipeResponse.getData().getRecipes();
                        Log.d(TAG, "Received " + recipes.size() + " saved recipes");

                        savedRecipes.addAll(recipes);
                        adapter.updateData(savedRecipes);

                        // Process recipes to get author names
                        processRecipesForAuthorNames(savedRecipes);

                        if (savedRecipes.isEmpty()) {
                            showEmptyState();
                        }
                    } else {
                        Log.e(TAG, "Response data or recipes is null");
                        showError("Không có dữ liệu công thức");
                    }
                } else {
                    int errorCode = response.code();
                    Log.e(TAG, "API error: " + errorCode);
                    showError("Không thể tải danh sách công thức đã lưu (Mã: " + errorCode + ")");
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.RecipeResponse> call, Throwable t) {
                // Hide loading on failure
                hideLoading();

                Log.e(TAG, "API call failed", t);
                showError("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    private void showEmptyState() {
        if (titleTextView != null) {
            titleTextView.setText("Bạn chưa lưu công thức nào");
        }
    }

    private void showError(String message) {
        if (isAdded()) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
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

}