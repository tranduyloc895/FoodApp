package fragment;

import android.content.Context;
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
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import adapter.Common_RecipeAdapter;
import adapter.New_RecipeAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.appfood.MainActivity;
import com.example.appfood.MainRecipe;
import com.example.appfood.R;
import com.example.appfood.UserProfileActivity;
import com.example.appfood.AddRecipeActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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

    // Loading overlay
    private View loadingOverlay;

    // Counters for tracking loading state
    private AtomicInteger pendingLoads = new AtomicInteger(0);
    private boolean initialLoadComplete = false;

    // Map to store saved recipes
    private Map<String, Boolean> savedRecipesMap = new HashMap<>();


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

        // Initialize loading overlay
        loadingOverlay = view.findViewById(R.id.loading_overlay);

        initViews(view);

        // Load saved recipes before initializing RecyclerViews
        loadSavedRecipes(() -> {
            // Initialize RecyclerViews after we've loaded saved recipes
            initRecyclerViews(view);

            // Show loading before initial data fetch
            showLoading();
            loadRecipeData();
        });

        return view;
    }

    /**
     * Loads saved recipes from user info
     */
    private void loadSavedRecipes(Runnable onComplete) {
        registerPendingLoad(); // Register saved recipes load

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.UserResponse> call = apiService.getUserInfo("Bearer " + token);

        call.enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.UserResponse> call, Response<ModelResponse.UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ModelResponse.UserResponse.User user = response.body().getData().getUser();
                    List<String> savedRecipes = user.getSavedRecipes();

                    // Clear and update saved recipes map
                    savedRecipesMap.clear();
                    if (savedRecipes != null) {
                        for (String recipeId : savedRecipes) {
                            savedRecipesMap.put(recipeId, true);
                        }
                    }

                    Log.d(TAG, "Loaded " + (savedRecipes != null ? savedRecipes.size() : 0) + " saved recipes");
                } else {
                    Log.e(TAG, "Error loading saved recipes: " + response.code());
                }

                completeLoad(); // Complete saved recipes load
                onComplete.run(); // Continue initialization
            }

            @Override
            public void onFailure(Call<ModelResponse.UserResponse> call, Throwable t) {
                Log.e(TAG, "Failed to load saved recipes: " + t.getMessage());
                completeLoad(); // Complete saved recipes load despite error
                onComplete.run(); // Continue initialization
            }
        });
    }

    /**
     * Called when the fragment is visible to the user.
     * Reloads recipe data to ensure it is up-to-date.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (initialLoadComplete) {
            // Show loading when refreshing data
            showLoading();

            // Reset counter before starting new loads
            pendingLoads.set(0);

            // Load saved recipes first, then refresh other data
            loadSavedRecipes(() -> {
                // Load recipe data after refreshing saved recipes list
                loadRecipeData();

                // Update user profile without changing the loading message
                refreshUserProfile(token);
            });
        }
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

    /**
     * Register a pending load operation
     */
    private void registerPendingLoad() {
        pendingLoads.incrementAndGet();
        showLoading(); // Show loading when registering a new load
    }

    /**
     * Complete a loading operation and hide overlay if all are done
     */
    private void completeLoad() {
        if (pendingLoads.decrementAndGet() <= 0) {
            initialLoadComplete = true;
            hideLoading(); // Hide loading when all loads are complete
        }
    }

    /**
     * Refreshes user profile without changing the loading message
     */
    private void refreshUserProfile(String token) {
        if (token != null) {
            registerPendingLoad(); // Register user info load

            getUserInfo(token, new OnUserInfoCallback() {
                @Override
                public void onUserInfoReceived(String name, String email, String dateOfBirth, String country, String url_avatar) {
                    tvGreeting.setText("Hello, " + name + "!");
                    loadProfileImage(url_avatar);
                    completeLoad(); // Complete user info load
                }

                @Override
                public void onError(String errorMessage) {
                    // Don't change greeting if it fails on refresh
                    Log.e(TAG, "Error refreshing user profile: " + errorMessage);
                    completeLoad(); // Complete user info load despite error
                }
            });
        }
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

        // Set up scroll listener for navigation bar hide/show
        setupScrollListener(view);

        // Set up Add Recipe FAB if it exists
        if (fabAddRecipe != null) {
            fabAddRecipe.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), AddRecipeActivity.class);
                intent.putExtra("token", token);
                startActivity(intent);
            });
        }
    }

    /**
     * Initializes RecyclerViews and their adapters for common and new recipes.
     */
    private void initRecyclerViews(View view) {
        recyclerView_common = view.findViewById(R.id.rv_common_recipe);
        recyclerView_common.setHasFixedSize(true);
        recipeList_common = new ArrayList<>();
        adapter_common = new Common_RecipeAdapter(requireContext(), recipeList_common, this::navigateToRecipeDetails, this);
        recyclerView_common.setAdapter(adapter_common);
        recyclerView_common.setLayoutManager(new GridLayoutManager(requireContext(), 1));

        recyclerView_new = view.findViewById(R.id.rv_new_recipe);
        recyclerView_new.setHasFixedSize(true);
        recipeList_new = new ArrayList<>();

        // Pass the token to the New_RecipeAdapter constructor
        adapter_new = new New_RecipeAdapter(requireContext(), recipeList_new, this::navigateToRecipeDetails, token);

        recyclerView_new.setAdapter(adapter_new);
        recyclerView_new.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        // Save token to shared preferences for backup access
        requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("token", token)
                .apply();
    }

    /**
     * Handles the greeting logic: fetches user info and displays greeting.
     */
    private void handleGreeting(String token) {
        if (token != null) {
            registerPendingLoad(); // Register user info load

            getUserInfo(token, new OnUserInfoCallback() {
                @Override
                public void onUserInfoReceived(String name, String email, String dateOfBirth, String country, String url_avatar) {
                    if (isAdded()) {
                        tvGreeting.setText("Hello, " + name + "!");

                        // Load avatar image using Glide
                        loadProfileImage(url_avatar);
                    }
                    completeLoad(); // Complete user info load
                }

                @Override
                public void onError(String errorMessage) {
                    if (isAdded()) {
                        tvGreeting.setText("Hello there!");
                        // Set default avatar on error
                        ivProfile.setImageResource(R.drawable.ic_profile);
                        Log.e(TAG, "Error loading user info: " + errorMessage);
                    }
                    completeLoad(); // Complete user info load despite error
                }
            });
        } else {
            tvGreeting.setText("Hello there!");
            // Set default avatar if token is missing
            ivProfile.setImageResource(R.drawable.ic_profile);
        }
    }

    /**
     * Loads the user's profile image into the ivProfile ImageView.
     * Uses the special case for "helenrecipes" user.
     */
    private void loadProfileImage(String avatarUrl) {
        if (!isAdded() || getContext() == null) return;

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            // Check if this is helenrecipes (special case)
            if (avatarUrl.contains("helenrecipes")) {
                ivProfile.setImageResource(R.drawable.ic_helen);
            } else {
                // Load avatar using Glide
                Glide.with(this)
                        .load(avatarUrl)
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.ic_profile)
                                .error(R.drawable.ic_profile)
                                .circleCrop())
                        .into(ivProfile);
            }
        } else {
            // Set default avatar if URL is empty
            ivProfile.setImageResource(R.drawable.ic_profile);
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
                    ModelResponse.UserResponse.User user = response.body().getData().getUser();
                    String name = user.getName();
                    String email = user.getEmail();
                    String dateOfBirth = user.getDateOfBirth();
                    String country = user.getCountry();
                    String urlAvatar = user.getUrlAvatar();

                    callback.onUserInfoReceived(name, email, dateOfBirth, country, urlAvatar);
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
        registerPendingLoad(); // Register common recipes load

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.RecipeResponse> call = apiService.getRecipeLatest("Bearer " + token);

        call.enqueue(new Callback<ModelResponse.RecipeResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Response<ModelResponse.RecipeResponse> response) {
                if (response.isSuccessful() && response.body() != null && isAdded()) {
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
                    Log.e(TAG, "Failed to load common recipes: " + (response.message() != null ? response.message() : "Unknown error"));
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Failed to load common recipes", Toast.LENGTH_SHORT).show();
                    }
                }
                completeLoad(); // Complete common recipes load
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "API Call Failed (Common): " + t.getMessage());
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to load common recipes", Toast.LENGTH_SHORT).show();
                }
                completeLoad(); // Complete common recipes load despite error
            }
        });
    }

    /**
     * Loads new recipes from the API and updates the corresponding RecyclerView.
     */
    private void loadNewRecipes() {
        registerPendingLoad(); // Register new recipes load

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.RecipeResponse> call = apiService.getRecipeLatest("Bearer " + token);

        call.enqueue(new Callback<ModelResponse.RecipeResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Response<ModelResponse.RecipeResponse> response) {
                if (response.isSuccessful() && response.body() != null && isAdded()) {
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
                    Log.e(TAG, "Failed to load new recipes: " + (response.message() != null ? response.message() : "Unknown error"));
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Failed to load new recipes", Toast.LENGTH_SHORT).show();
                    }
                }
                completeLoad(); // Complete new recipes load
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "API Call Failed (New): " + t.getMessage());
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to load new recipes", Toast.LENGTH_SHORT).show();
                }
                completeLoad(); // Complete new recipes load despite error
            }
        });
    }

    /**
     * Fetches ratings for a list of recipes and updates their rating values.
     * @param apiService The API service to use for requests
     * @param recipes The list of recipes to fetch ratings for
     */
    private void fetchRatingsForRecipes(ApiService apiService, List<ModelResponse.RecipeResponse.Recipe> recipes) {
        // Register a batch load for all rating requests
        int recipeCount = recipes.size();
        if (recipeCount > 0) {
            registerPendingLoad();
        }

        final AtomicInteger completedRatings = new AtomicInteger(0);

        for (ModelResponse.RecipeResponse.Recipe recipe : recipes) {
            String recipeId = recipe.getId();
            Call<ModelResponse.RecipeDetailResponse> ratingCall = apiService.getRecipeDetail("Bearer " + token, recipeId);

            ratingCall.enqueue(new Callback<ModelResponse.RecipeDetailResponse>() {
                @Override
                public void onResponse(@NonNull Call<ModelResponse.RecipeDetailResponse> call, @NonNull Response<ModelResponse.RecipeDetailResponse> response) {
                    if (response.isSuccessful() && response.body() != null && isAdded()) {
                        ModelResponse.RecipeDetailResponse.Recipe detailedRecipe = response.body().getData().getRecipe();
                        double averageRating = detailedRecipe.getAverageRating();
                        recipe.setRating(detailedRecipe.getRatings());

                        // Notify adapter of data change
                        if (recipeList_common.contains(recipe)) {
                            adapter_common.notifyDataSetChanged();
                        } else if (recipeList_new.contains(recipe)) {
                            adapter_new.notifyDataSetChanged();
                        }
                    } else {
                        Log.e(TAG, "Failed to get details for Recipe ID: " + recipeId);
                    }

                    // Check if all ratings are completed
                    if (completedRatings.incrementAndGet() >= recipeCount) {
                        completeLoad(); // Complete ratings batch load
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ModelResponse.RecipeDetailResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "API Call Failed (Details) - Recipe ID: " + recipeId + ", Error: " + t.getMessage());

                    // Check if all ratings are completed
                    if (completedRatings.incrementAndGet() >= recipeCount) {
                        completeLoad(); // Complete ratings batch load despite errors
                    }
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
         * @param url_avatar   The user's avatar URL.
         */
        void onUserInfoReceived(String name, String email, String dateOfBirth, String country, String url_avatar);

        /**
         * Called when there is an error fetching user information.
         *
         * @param errorMessage The error message.
         */
        void onError(String errorMessage);
    }

    /**
     * Method to check if a recipe is saved
     */
    public boolean isRecipeSaved(String recipeId) {
        return savedRecipesMap.containsKey(recipeId) && savedRecipesMap.get(recipeId);
    }

    /**
     * Method to update saved recipe status
     */
    public void updateSavedRecipeStatus(String recipeId, boolean isSaved) {
        savedRecipesMap.put(recipeId, isSaved);

        // Notify adapters of the change
        adapter_common.notifyDataSetChanged();
        adapter_new.notifyDataSetChanged();
    }

    private void setupScrollListener(View view) {
        NestedScrollView nestedScrollView = view.findViewById(R.id.nested_scroll_view);
        if (nestedScrollView != null) {
            nestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
                private int lastScrollY = 0;

                @Override
                public void onScrollChange(NestedScrollView v, int scrollX, int scrollY,
                                           int oldScrollX, int oldScrollY) {
                    // Scroll down (user swipes up)
                    if (scrollY > lastScrollY && scrollY - lastScrollY > 15) {
                        // Hide navigation bar
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).hideNavigationBar();
                        }
                    }
                    // Scroll up (user swipes down)
                    else if (scrollY < lastScrollY && lastScrollY - scrollY > 15) {
                        // Show navigation bar
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).showNavigationBar();
                        }
                    }
                    lastScrollY = scrollY;
                }
            });
        }
    }
}