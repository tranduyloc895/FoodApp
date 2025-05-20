package fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.Toast;
import android.widget.TextView;
import android.content.Intent;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import adapter.Common_RecipeAdapter;
import adapter.New_RecipeAdapter;

import com.example.appfood.MainRecipe;
import com.example.appfood.R;
import com.example.appfood.UserProfileActivity;
import com.example.appfood.AddRecipeActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * HomeFragment displays lists of common and new recipes.
 * Handles loading data from API and updating RecyclerViews.
 */
public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private static final int MAX_RECIPES_TO_DISPLAY = 10;
    private RecyclerView recyclerView_common, recyclerView_new;
    private Common_RecipeAdapter adapter_common;
    private New_RecipeAdapter adapter_new;
    private List<ModelResponse.RecipeResponse.Recipe> recipeList_common, recipeList_new;
    private String token;
    private TextView tvGreeting;
    private ImageView ivProfile;
    private FloatingActionButton fabAddRecipe;

    /**
     * Inflates the fragment layout and initializes UI elements.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Get token from activity
        token = getActivity().getIntent().getStringExtra("token");
        if (token == null || token.isEmpty()) {
            Toast.makeText(requireContext(), "Token không hợp lệ!", Toast.LENGTH_SHORT).show();
            return view;
        }

        initViews(view);
        initRecyclerViews(view);
        loadRecipeData();

        return view;
    }

    /**
     * Called when the fragment is visible to the user.
     * Reloads recipe data to ensure it is up-to-date.
     */
    @Override
    public void onResume() {
        super.onResume();
        loadRecipeData();
    }

    /**
     * Initializes view references.
     */
    private void initViews(View view) {
        tvGreeting = view.findViewById(R.id.tv_greeting);
        ivProfile = view.findViewById(R.id.iv_profile);
        fabAddRecipe = view.findViewById(R.id.fab_add);

        handleGreeting(token);
        setupProfileClick(token);
    }

    /**
     * Initializes RecyclerViews and their adapters for common and new recipes.
     */
    private void initRecyclerViews(View view) {
        recyclerView_common = view.findViewById(R.id.rv_common_recipe);
        recyclerView_common.setHasFixedSize(true);
        recipeList_common = new ArrayList<>();
        adapter_common = new Common_RecipeAdapter(requireContext(), recipeList_common, this::navigateToRecipeDetails);
        recyclerView_common.setAdapter(adapter_common);
        recyclerView_common.setLayoutManager(new GridLayoutManager(requireContext(), 1));

        recyclerView_new = view.findViewById(R.id.rv_new_recipe);
        recyclerView_new.setHasFixedSize(true);
        recipeList_new = new ArrayList<>();
        adapter_new = new New_RecipeAdapter(requireContext(), recipeList_new, this::navigateToRecipeDetails);
        recyclerView_new.setAdapter(adapter_new);
        recyclerView_new.setLayoutManager(new GridLayoutManager(requireContext(), 2));
    }

    /**
     * Handles the greeting logic: fetches user info and displays greeting.
     */
    private void handleGreeting(String token) {
        if (token != null) {
            getUserInfo(token, new OnUserInfoCallback() {
                @Override
                public void onUserInfoReceived(String name, String email, String dateOfBirth, String country) {
                    tvGreeting.setText("Hello, " + name + "!");
                }

                @Override
                public void onError(String errorMessage) {
                    tvGreeting.setText("Failed to load user info");
                }
            });
        } else {
            tvGreeting.setText("Token is missing.");
        }
    }

    /**
     * Sets up the profile image click to open UserProfileActivity.
     */
    private void setupProfileClick(String token) {
        ivProfile.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), UserProfileActivity.class);
            intent.putExtra("token", token);
            startActivity(intent);
        });
    }

    /**
     * Navigate to recipe details screen with recipe ID and token.
     */
    private void navigateToRecipeDetails(String recipeId) {
        Intent intent = new Intent(requireContext(), MainRecipe.class);
        intent.putExtra("recipe_id", recipeId);
        intent.putExtra("token", token);
        startActivity(intent);
    }

    /**
     * Loads recipe data from the API for both common and new recipes.
     */
    private void loadRecipeData() {
        loadCommonRecipes();
        loadNewRecipes();
    }

    /**
     * Fetches user information from the API.
     */
    public void getUserInfo(String token, OnUserInfoCallback callback) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.UserResponse> call = apiService.getUserInfo("Bearer " + token);

        call.enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.UserResponse> call, Response<ModelResponse.UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String name = response.body().getData().getUser().getName();
                    callback.onUserInfoReceived(name, "", "", "");
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
     * Loads common recipes from the API and updates the corresponding RecyclerView.
     */
    private void loadCommonRecipes() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.RecipeResponse> call = apiService.getRecipeLatest("Bearer " + token);

        call.enqueue(new Callback<ModelResponse.RecipeResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Response<ModelResponse.RecipeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    recipeList_common.clear();
                    List<ModelResponse.RecipeResponse.Recipe> allRecipes = response.body().getData().getRecipes();
                    List<ModelResponse.RecipeResponse.Recipe> displayRecipes =
                            allRecipes.subList(0, Math.min(allRecipes.size(), MAX_RECIPES_TO_DISPLAY));
                    recipeList_common.addAll(displayRecipes);

                    // Fetch ratings for each recipe
                    fetchRatingsForRecipes(apiService, recipeList_common);

                    // Notify adapter of data changes
                    adapter_common.notifyDataSetChanged();

                    recyclerView_common.setLayoutManager(new GridLayoutManager(requireContext(),
                            Math.max(1, recipeList_common.size()), GridLayoutManager.VERTICAL, false));
                } else {
                    Log.e(TAG, "Failed to load common recipes: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "API Call Failed (Common): " + t.getMessage());
            }
        });
    }

    /**
     * Loads new recipes from the API and updates the corresponding RecyclerView.
     */
    private void loadNewRecipes() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.RecipeResponse> call = apiService.getRecipeLatest("Bearer " + token);

        call.enqueue(new Callback<ModelResponse.RecipeResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Response<ModelResponse.RecipeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    recipeList_new.clear();
                    List<ModelResponse.RecipeResponse.Recipe> allRecipes = response.body().getData().getRecipes();
                    List<ModelResponse.RecipeResponse.Recipe> displayRecipes =
                            allRecipes.subList(0, Math.min(allRecipes.size(), MAX_RECIPES_TO_DISPLAY));
                    recipeList_new.addAll(displayRecipes);

                    // Fetch ratings for each recipe
                    fetchRatingsForRecipes(apiService, recipeList_new);

                    // Notify adapter of data changes
                    adapter_new.notifyDataSetChanged();

                    int spanCount = Math.max(1, recipeList_new.size());
                    recyclerView_new.setLayoutManager(new GridLayoutManager(requireContext(),
                            spanCount, GridLayoutManager.VERTICAL, false));
                } else {
                    Log.e(TAG, "Failed to load new recipes: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "API Call Failed (New): " + t.getMessage());
            }
        });
    }

    /**
     * Fetches ratings for a list of recipes and updates their rating values.
     * @param apiService The API service to use for requests
     * @param recipes The list of recipes to fetch ratings for
     */
    private void fetchRatingsForRecipes(ApiService apiService, List<ModelResponse.RecipeResponse.Recipe> recipes) {
        for (ModelResponse.RecipeResponse.Recipe recipe : recipes) {
            String recipeId = recipe.getId();
            Call<ModelResponse.RecipeDetailResponse> ratingCall = apiService.getRecipeDetail("Bearer " + token, recipeId);

            ratingCall.enqueue(new Callback<ModelResponse.RecipeDetailResponse>() {
                @Override
                public void onResponse(@NonNull Call<ModelResponse.RecipeDetailResponse> call, @NonNull Response<ModelResponse.RecipeDetailResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ModelResponse.RecipeDetailResponse.Recipe detailedRecipe = response.body().getData().getRecipe();
                        double averageRating = detailedRecipe.getAverageRating();
                        recipe.setAverageRating(averageRating);

                        // Notify adapter of data change
                        if (recipeList_common.contains(recipe)) {
                            adapter_common.notifyDataSetChanged();
                        } else if (recipeList_new.contains(recipe)) {
                            adapter_new.notifyDataSetChanged();
                        }
                    } else {
                        Log.e(TAG, "Failed to get details for Recipe ID: " + recipeId);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ModelResponse.RecipeDetailResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "API Call Failed (Details) - Recipe ID: " + recipeId + ", Error: " + t.getMessage());
                }
            });
        }
    }

    /**
     * Callback interface for receiving user information or error from getUserInfo.
     */
    public interface OnUserInfoCallback {
        /**
         * Called when user information is successfully received.
         *
         * @param name         The user's name.
         * @param email        The user's email.
         * @param dateOfBirth  The user's date of birth.
         * @param country      The user's country.
         */
        void onUserInfoReceived(String name, String email, String dateOfBirth, String country);

        /**
         * Called when there is an error fetching user information.
         *
         * @param errorMessage The error message.
         */
        void onError(String errorMessage);
    }
}