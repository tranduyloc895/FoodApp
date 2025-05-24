package fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.appfood.AddRecipeActivity;
import com.example.appfood.MainActivity;
import com.example.appfood.MainRecipe;
import com.example.appfood.R;
import com.example.appfood.UserProfileActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import adapter.CommonRecipeAdapter;
import adapter.NewRecipeAdapter;
import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * HomeFragment displays lists of common and new recipes.
 */
public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int MAX_RECIPES_TO_DISPLAY = 10;

    // UI Components
    private RecyclerView rvCommonRecipes, rvNewRecipes;
    private TextView tvGreeting;
    private ImageView ivProfile;
    private FloatingActionButton fabAddRecipe;
    private View loadingOverlay;

    // Data
    private List<ModelResponse.RecipeResponse.Recipe> commonRecipeList, newRecipeList;
    private CommonRecipeAdapter commonRecipeAdapter;
    private NewRecipeAdapter newRecipeAdapter;
    private String token;
    private Map<String, Boolean> savedRecipesMap = new HashMap<>();

    // Loading state tracking
    private AtomicInteger pendingLoads = new AtomicInteger(0);
    private boolean initialLoadComplete = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        if (!extractToken()) {
            // Return early if token is invalid
            return view;
        }

        // Initialize views and data
        initViews(view);

        // Start loading data
        startInitialDataLoad(view);

        return view;
    }

    /**
     * Extracts token from activity intent
     * @return true if token is valid, false otherwise
     */
    private boolean extractToken() {
        if (getActivity() == null) return false;

        token = getActivity().getIntent().getStringExtra("token");
        if (token == null || token.isEmpty()) {
            Toast.makeText(requireContext(), "Invalid token!", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Save token to shared preferences for backup access
        saveTokenToPreferences();
        return true;
    }

    /**
     * Saves token to shared preferences
     */
    private void saveTokenToPreferences() {
        requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("token", token)
                .apply();
    }

    /**
     * Starts the initial data loading sequence
     */
    private void startInitialDataLoad(View view) {
        // Initialize loading overlay
        loadingOverlay = view.findViewById(R.id.loading_overlay);
        showLoading();

        // Load saved recipes first, then initialize everything else
        loadSavedRecipes(() -> {
            initRecyclerViews(view);
            loadRecipeData();
        });
    }

    /**
     * Initializes UI elements
     */
    private void initViews(View view) {
        tvGreeting = view.findViewById(R.id.tv_greeting);
        ivProfile = view.findViewById(R.id.iv_profile);
        fabAddRecipe = view.findViewById(R.id.fab_add);

        setupProfileUI();
        setupScrollListener(view);
        setupAddRecipeButton();
    }

    /**
     * Sets up profile UI elements and interactions
     */
    private void setupProfileUI() {
        handleGreeting();

        // Set up profile image click listener
        if (ivProfile != null) {
            ivProfile.setOnClickListener(v -> navigateToUserProfile());
        }
    }

    /**
     * Navigates to user profile activity
     */
    private void navigateToUserProfile() {
        Intent intent = new Intent(requireContext(), UserProfileActivity.class);
        intent.putExtra("token", token);
        startActivity(intent);
    }

    /**
     * Sets up Add Recipe button
     */
    private void setupAddRecipeButton() {
        if (fabAddRecipe != null) {
            fabAddRecipe.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), AddRecipeActivity.class);
                intent.putExtra("token", token);
                startActivity(intent);
            });
        }
    }

    /**
     * Initializes RecyclerViews for both recipe types
     */
    private void initRecyclerViews(View view) {
        // Common recipes RecyclerView
        rvCommonRecipes = view.findViewById(R.id.rv_common_recipe);
        rvCommonRecipes.setHasFixedSize(true);
        commonRecipeList = new ArrayList<>();
        commonRecipeAdapter = new CommonRecipeAdapter(requireContext(), commonRecipeList,
                this::navigateToRecipeDetails, this);
        rvCommonRecipes.setAdapter(commonRecipeAdapter);

        // New recipes RecyclerView
        rvNewRecipes = view.findViewById(R.id.rv_new_recipe);
        rvNewRecipes.setHasFixedSize(true);
        newRecipeList = new ArrayList<>();
        newRecipeAdapter = new NewRecipeAdapter(requireContext(), newRecipeList,
                this::navigateToRecipeDetails, token);
        rvNewRecipes.setAdapter(newRecipeAdapter);
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
                } else {
                    Log.e(TAG, "Error loading saved recipes: " + response.code());
                }

                completeLoad();
                onComplete.run();
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to load saved recipes: " + t.getMessage());
                completeLoad();
                onComplete.run();
            }
        });
    }

    /**
     * Handles loading the user greeting with name and avatar
     */
    private void handleGreeting() {
        if (token != null) {
            registerPendingLoad();

            getUserInfo(new UserInfoCallback() {
                @Override
                public void onSuccess(String name, String email, String dateOfBirth,
                                      String country, String avatarUrl) {
                    if (isAdded()) {
                        tvGreeting.setText("Hello, " + name + "!");
                        loadProfileImage(avatarUrl);
                    }
                    completeLoad();
                }

                @Override
                public void onError(String errorMessage) {
                    if (isAdded()) {
                        tvGreeting.setText("Hello, Welcome!");
                        ivProfile.setImageResource(R.drawable.ic_profile);
                        Log.e(TAG, "Error loading user info: " + errorMessage);
                    }
                    completeLoad();
                }
            });
        } else {
            tvGreeting.setText("Hello, Welcome!");
            ivProfile.setImageResource(R.drawable.ic_profile);
        }
    }

    /**
     * Loads the user's profile image
     */
    private void loadProfileImage(String avatarUrl) {
        if (!isAdded() || getContext() == null) return;

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            // Special case for helenrecipes
            if (avatarUrl.contains("helenrecipes")) {
                ivProfile.setImageResource(R.drawable.ic_helen);
            } else {
                Glide.with(this)
                        .load(avatarUrl)
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.ic_profile)
                                .error(R.drawable.ic_profile)
                                .circleCrop())
                        .into(ivProfile);
            }
        } else {
            ivProfile.setImageResource(R.drawable.ic_profile);
        }
    }

    /**
     * Sets up scroll listener for navigation bar visibility
     */
    private void setupScrollListener(View view) {
        NestedScrollView nestedScrollView = view.findViewById(R.id.nested_scroll_view);
        if (nestedScrollView != null) {
            nestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
                private int lastScrollY = 0;
                private static final int SCROLL_THRESHOLD = 15;

                @Override
                public void onScrollChange(NestedScrollView v, int scrollX, int scrollY,
                                           int oldScrollX, int oldScrollY) {
                    // Scroll down - hide navigation bar
                    if (scrollY > lastScrollY && scrollY - lastScrollY > SCROLL_THRESHOLD) {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).hideNavigationBar();
                        }
                    }
                    // Scroll up - show navigation bar
                    else if (scrollY < lastScrollY && lastScrollY - scrollY > SCROLL_THRESHOLD) {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).showNavigationBar();
                        }
                    }
                    lastScrollY = scrollY;
                }
            });
        }
    }

    /**
     * Navigate to recipe details screen
     */
    private void navigateToRecipeDetails(String recipeId) {
        Intent intent = new Intent(requireContext(), MainRecipe.class);
        intent.putExtra("recipe_id", recipeId);
        intent.putExtra("token", token);
        startActivity(intent);
    }

    /**
     * Loads all recipe data
     */
    private void loadRecipeData() {
        loadCommonRecipes();
        loadNewRecipes();
    }

    /**
     * Loads common recipes from API
     */
    private void loadCommonRecipes() {
        registerPendingLoad();

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.RecipeResponse> call = apiService.getRecipeLatest(BEARER_PREFIX + token);

        call.enqueue(new Callback<ModelResponse.RecipeResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.RecipeResponse> call,
                                   @NonNull Response<ModelResponse.RecipeResponse> response) {
                if (isSuccessfulRecipeResponse(response)) {
                    processCommonRecipes(apiService, response.body().getData().getRecipes());
                } else {
                    handleRecipeLoadError("common recipes");
                }
                completeLoad();
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "API Call Failed (Common): " + t.getMessage());
                handleRecipeLoadError("common recipes");
                completeLoad();
            }
        });
    }

    /**
     * Loads new recipes from API
     */
    private void loadNewRecipes() {
        registerPendingLoad();

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.RecipeResponse> call = apiService.getRecipeLatest(BEARER_PREFIX + token);

        call.enqueue(new Callback<ModelResponse.RecipeResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.RecipeResponse> call,
                                   @NonNull Response<ModelResponse.RecipeResponse> response) {
                if (isSuccessfulRecipeResponse(response)) {
                    processNewRecipes(apiService, response.body().getData().getRecipes());
                } else {
                    handleRecipeLoadError("new recipes");
                }
                completeLoad();
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "API Call Failed (New): " + t.getMessage());
                handleRecipeLoadError("new recipes");
                completeLoad();
            }
        });
    }

    /**
     * Checks if recipe response is successful and contains required data
     */
    private boolean isSuccessfulRecipeResponse(Response<ModelResponse.RecipeResponse> response) {
        return response.isSuccessful() &&
                response.body() != null &&
                response.body().getData() != null &&
                response.body().getData().getRecipes() != null &&
                isAdded();
    }

    /**
     * Handles error when loading recipes
     */
    private void handleRecipeLoadError(String recipeType) {
        Log.e(TAG, "Failed to load " + recipeType);
        if (isAdded()) {
            Toast.makeText(requireContext(), "Failed to load " + recipeType, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Processes retrieved common recipes
     */
    private void processCommonRecipes(ApiService apiService, List<ModelResponse.RecipeResponse.Recipe> recipes) {
        commonRecipeList.clear();
        List<ModelResponse.RecipeResponse.Recipe> displayRecipes =
                recipes.subList(0, Math.min(recipes.size(), MAX_RECIPES_TO_DISPLAY));
        commonRecipeList.addAll(displayRecipes);

        // Fetch ratings for each recipe
        fetchRatingsForRecipes(apiService, commonRecipeList);

        // Update RecyclerView
        commonRecipeAdapter.notifyDataSetChanged();
        updateCommonRecipesLayout();
    }

    /**
     * Updates layout for common recipes recycler view
     */
    private void updateCommonRecipesLayout() {
        int spanCount = Math.max(1, commonRecipeList.size());
        rvCommonRecipes.setLayoutManager(new GridLayoutManager(requireContext(),
                spanCount, GridLayoutManager.VERTICAL, false));
    }

    /**
     * Processes retrieved new recipes
     */
    private void processNewRecipes(ApiService apiService, List<ModelResponse.RecipeResponse.Recipe> recipes) {
        newRecipeList.clear();
        List<ModelResponse.RecipeResponse.Recipe> displayRecipes =
                recipes.subList(0, Math.min(recipes.size(), MAX_RECIPES_TO_DISPLAY));
        newRecipeList.addAll(displayRecipes);

        // Fetch ratings for each recipe
        fetchRatingsForRecipes(apiService, newRecipeList);

        // Update RecyclerView
        newRecipeAdapter.notifyDataSetChanged();
        updateNewRecipesLayout();
    }

    /**
     * Updates layout for new recipes recycler view
     */
    private void updateNewRecipesLayout() {
        int spanCount = Math.max(1, newRecipeList.size());
        rvNewRecipes.setLayoutManager(new GridLayoutManager(requireContext(),
                1, GridLayoutManager.VERTICAL, false));
    }

    /**
     * Fetches detailed ratings for recipes
     */
//    private void fetchRatingsForRecipes(ApiService apiService, List<ModelResponse.RecipeResponse.Recipe> recipes) {
//        int recipeCount = recipes.size();
//        if (recipeCount > 0) {
//            registerPendingLoad();
//        }
//
//        final AtomicInteger completedRatings = new AtomicInteger(0);
//
//        for (ModelResponse.RecipeResponse.Recipe recipe : recipes) {
//            String recipeId = recipe.getId();
//            Call<ModelResponse.RecipeDetailResponse> ratingCall =
//                    apiService.getRecipeDetail(BEARER_PREFIX + token, recipeId);
//
//            ratingCall.enqueue(new Callback<ModelResponse.RecipeDetailResponse>() {
//                @Override
//                public void onResponse(@NonNull Call<ModelResponse.RecipeDetailResponse> call,
//                                       @NonNull Response<ModelResponse.RecipeDetailResponse> response) {
//                    if (response.isSuccessful() && response.body() != null &&
//                            response.body().getData() != null &&
//                            response.body().getData().getRecipe() != null &&
//                            isAdded()) {
//
//                        ModelResponse.RecipeDetailResponse.Recipe detailedRecipe =
//                                response.body().getData().getRecipe();
//                        recipe.setRating(detailedRecipe.getRatings());
//
//                        // Notify appropriate adapter
//                        notifyAdapterForRecipe(recipe);
//                    } else {
//                        Log.e(TAG, "Failed to get details for Recipe ID: " + recipeId);
//                    }
//
//                    // Check if all ratings are completed
//                    if (completedRatings.incrementAndGet() >= recipeCount) {
//                        completeLoad(); // Complete ratings batch load
//                    }
//                }
//
//                @Override
//                public void onFailure(@NonNull Call<ModelResponse.RecipeDetailResponse> call, @NonNull Throwable t) {
//                    Log.e(TAG, "API Call Failed (Details) - Recipe ID: " + recipeId + ", Error: " + t.getMessage());
//
//                    // Check if all ratings are completed
//                    if (completedRatings.incrementAndGet() >= recipeCount) {
//                        completeLoad(); // Complete ratings batch load despite errors
//                    }
//                }
//            });
//        }
//    }

    /**
     * Fetches detailed ratings for recipes using the new Rating API
     */
    private void fetchRatingsForRecipes(ApiService apiService, List<ModelResponse.RecipeResponse.Recipe> recipes) {
        int recipeCount = recipes.size();
        if (recipeCount > 0) {
            registerPendingLoad();
        }

        final AtomicInteger completedRatings = new AtomicInteger(0);

        for (ModelResponse.RecipeResponse.Recipe recipe : recipes) {
            String recipeId = recipe.getId();
            // Change the type to match the API service return type
            Call<ModelResponse.getRatingResponse> ratingCall =
                    apiService.getRecipeRating(BEARER_PREFIX + token, recipeId);

            // Update the callback to use getRatingResponse instead of RatingResponse
            ratingCall.enqueue(new Callback<ModelResponse.getRatingResponse>() {
                @Override
                public void onResponse(@NonNull Call<ModelResponse.getRatingResponse> call,
                                       @NonNull Response<ModelResponse.getRatingResponse> response) {
                    if (response.isSuccessful() && response.body() != null &&
                            response.body().getData() != null &&
                            isAdded()) {

                        ModelResponse.getRatingResponse.Data ratingData = response.body().getData();

                        // Log the rating values for debugging
                        Log.d(TAG, "Recipe ID: " + recipeId +
                                " - Average Rating: " + ratingData.getAverageRating() +
                                " - Total Ratings: " + ratingData.getTotalRatings());

                        // Update recipe with rating information
                        recipe.setAverageRating(ratingData.getAverageRating());

                        // If you need to handle user rating info as well, you can do it here
                        if (ratingData.hasUserRated()) {
                            // Store user's rating if needed
                            // recipe.setUserRating(ratingData.getUserRating().getRating());
                        }

                        // Notify appropriate adapter
                        notifyAdapterForRecipe(recipe);
                    } else {
                        Log.e(TAG, "Failed to get ratings for Recipe ID: " + recipeId);
                    }

                    // Check if all ratings are completed
                    if (completedRatings.incrementAndGet() >= recipeCount) {
                        completeLoad(); // Complete ratings batch load
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ModelResponse.getRatingResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "API Call Failed (Ratings) - Recipe ID: " + recipeId + ", Error: " + t.getMessage());

                    // Check if all ratings are completed
                    if (completedRatings.incrementAndGet() >= recipeCount) {
                        completeLoad(); // Complete ratings batch load despite errors
                    }
                }
            });
        }
    }
    /**
     * Notifies the appropriate adapter when a recipe is updated
     */
    private void notifyAdapterForRecipe(ModelResponse.RecipeResponse.Recipe recipe) {
        if (commonRecipeList.contains(recipe)) {
            commonRecipeAdapter.notifyDataSetChanged();
        } else if (newRecipeList.contains(recipe)) {
            newRecipeAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Fetches user info from API
     */
    private void getUserInfo(UserInfoCallback callback) {
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
            public void onFailure(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Throwable t) {
                callback.onError("Request failed: " + t.getMessage());
            }
        });
    }

    /**
     * Called when fragment resumes to refresh data
     */
    @Override
    public void onResume() {
        super.onResume();
        if (initialLoadComplete) {
            refreshData();
        }
    }

    /**
     * Refreshes all data
     */
    private void refreshData() {
        showLoading();
        pendingLoads.set(0); // Reset counter

        // Refresh data in sequence
        loadSavedRecipes(() -> {
            loadRecipeData();
            refreshUserProfile();
        });
    }

    /**
     * Refreshes user profile information
     */
    private void refreshUserProfile() {
        registerPendingLoad();

        getUserInfo(new UserInfoCallback() {
            @Override
            public void onSuccess(String name, String email, String dateOfBirth,
                                  String country, String avatarUrl) {
                if (isAdded()) {
                    tvGreeting.setText("Hello, " + name + "!");
                    loadProfileImage(avatarUrl);
                }
                completeLoad();
            }

            @Override
            public void onError(String errorMessage) {
                // Keep existing greeting on refresh error
                Log.e(TAG, "Error refreshing user profile: " + errorMessage);
                completeLoad();
            }
        });
    }

    /**
     * Shows loading overlay
     */
    private void showLoading() {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Hides loading overlay
     */
    private void hideLoading() {
        if (loadingOverlay != null && isAdded()) {
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
            initialLoadComplete = true;
            hideLoading();
        }
    }

    /**
     * Checks if a recipe is saved by current user
     */
    public boolean isRecipeSaved(String recipeId) {
        return savedRecipesMap.containsKey(recipeId) && savedRecipesMap.get(recipeId);
    }

    /**
     * Updates saved status for a recipe
     */
    public void updateSavedRecipeStatus(String recipeId, boolean isSaved) {
        savedRecipesMap.put(recipeId, isSaved);

        // Notify adapters about the change
        if (commonRecipeAdapter != null) {
            commonRecipeAdapter.notifyDataSetChanged();
        }

        if (newRecipeAdapter != null) {
            newRecipeAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Callback interface for user info
     */
    public interface UserInfoCallback {
        void onSuccess(String name, String email, String dateOfBirth, String country, String avatarUrl);
        void onError(String errorMessage);
    }
}