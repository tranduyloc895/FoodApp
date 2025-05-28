package fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.appfood.R;
import com.example.appfood.UserProfileActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import adapter.ProfilePagerAdapter;
import api.ApiService;
import api.RetrofitClient;
import api.ModelResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ProfileFragment displays the current user's profile information and recipes.
 * It shows user details, recipe statistics, and tabbed content for uploaded and saved recipes.
 * The fragment manages loading states to provide visual feedback during API calls.
 */
public class ProfileFragment extends Fragment {
    // Constants for logging and configuration
    private static final String TAG = "ProfileFragment";
    private static final int TOTAL_API_CALLS = 3;
    private static final int LOADING_TIMEOUT_MS = 8000;
    private static final int TAB_SWITCH_LOADING_MS = 300;

    // UI elements
    private TextView tvProfileName, tvNumberUploadedRecipes, tvNumberSavedRecipes, tvCountry, tvLevel, tvUploaded;
    private String token, currentUserId;
    private ImageView ivProfileImage;
    private View loadingOverlay;
    private ViewPager2 viewPager;

    // State management
    private final AtomicInteger pendingLoads = new AtomicInteger(0);
    private boolean initialLoadComplete = false;
    private int currentTabPosition = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * Inflates the fragment layout.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    /**
     * Sets up the UI components and loads initial data after views are created.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);      // Initialize view references
        setupTabs();          // Set up tab layout and ViewPager

        // Load user data if token is available
        if (extractToken()) {
            loadData();
        }

        // Set click listener for profile image to open detailed profile
        ivProfileImage.setOnClickListener(v -> openUserProfile());
    }

    /**
     * Initializes view references from the layout and sets initial properties.
     * @param view The fragment's root view
     */
    private void initViews(View view) {
        // Find views by ID
        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvNumberUploadedRecipes = view.findViewById(R.id.tv_number_uploaded_recipes);
        tvNumberSavedRecipes = view.findViewById(R.id.tv_number_saved_recipes);
        tvCountry = view.findViewById(R.id.tv_country_code);
        tvLevel = view.findViewById(R.id.tv_profile_level);
        ivProfileImage = view.findViewById(R.id.iv_profile_picture);
        loadingOverlay = view.findViewById(R.id.loading_overlay);
        viewPager = view.findViewById(R.id.vp_category);
        tvUploaded = view.findViewById(R.id.tv_uploaded_recipes);

        tvUploaded.setSelected(true);

        // Enable text marquee for scrolling texts
        tvProfileName.setSelected(true);
        tvUploaded.setSelected(true);
    }

    /**
     * Sets up the tab layout with ViewPager2 for uploaded and saved recipes.
     * Configures tab titles, selection listeners, and page change callbacks.
     */
    private void setupTabs() {
        TabLayout tabLayout = requireView().findViewById(R.id.tl_category);

        // Initialize ViewPager with adapter
        ProfilePagerAdapter adapter = new ProfilePagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Disable swiping until data is fully loaded
        viewPager.setUserInputEnabled(initialLoadComplete);

        // Connect TabLayout with ViewPager and set custom tab titles
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            String title = position == 0 ? "Uploaded" : "Saved";
            SpannableString spannableString = new SpannableString(title);
            spannableString.setSpan(
                    new AbsoluteSizeSpan(16, true),
                    0,
                    spannableString.length(),
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
            );
            tab.setText(spannableString);
        }).attach();

        // Handle tab selection events
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTabPosition = tab.getPosition();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Not needed
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Refresh data when tab is selected again
                refreshData();
            }
        });

        // Show loading animation during page transitions
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentTabPosition = position;
                showBriefLoading();
            }
        });
    }

    /**
     * Extracts authentication token from parent activity.
     * @return true if token was successfully extracted, false otherwise
     */
    private boolean extractToken() {
        if (getActivity() == null) return false;

        token = getActivity().getIntent().getStringExtra("token");
        if (token == null || token.isEmpty()) {
            Toast.makeText(requireContext(), "Invalid token!", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /**
     * Opens the detailed user profile activity.
     */
    private void openUserProfile() {
        Intent intent = new Intent(getActivity(), UserProfileActivity.class);
        intent.putExtra("token", token);
        startActivity(intent);
    }

    /**
     * Loads all required data by initiating parallel API calls.
     * Shows loading indicator during the process.
     */
    private void loadData() {
        // Set the expected number of API calls to complete
        pendingLoads.set(TOTAL_API_CALLS);
        showLoading(true);  // Show loading with safety timeout

        // Start all API calls in parallel for better performance
        fetchUserProfile();
        fetchSavedRecipesCount();
        fetchUploadedRecipesCount();
    }

    /**
     * Refreshes all data when needed (e.g., on resume or tab reselection).
     */
    private void refreshData() {
        pendingLoads.set(TOTAL_API_CALLS);
        showLoading(false);  // Show loading without safety timeout for refreshes

        // Refresh all data sources
        fetchUserProfile();
        fetchSavedRecipesCount();
        fetchUploadedRecipesCount();
    }

    /**
     * Shows a brief loading animation for visual feedback during tab changes.
     */
    private void showBriefLoading() {
        if (!initialLoadComplete) return;  // Skip if initial load isn't complete

        showLoading(false);  // Show loading without safety timeout

        // Hide loading after a brief delay
        handler.postDelayed(() -> {
            if (isAdded()) {  // Check if fragment is still attached
                hideLoading();
            }
        }, TAB_SWITCH_LOADING_MS);
    }

    /**
     * Fetches user profile information from the API.
     * Updates UI with name, country, and stores the user ID.
     */
    private void fetchUserProfile() {
        ApiService apiService = RetrofitClient.getApiService();
        apiService.getUserInfo("Bearer " + token).enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Response<ModelResponse.UserResponse> response) {
                if (!isAdded()) {
                    completeLoad();
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    ModelResponse.UserResponse.User user = response.body().getData().getUser();

                    // Update UI with user information
                    tvProfileName.setText(user.getName());

                    // Format country code (take first 3 chars or use default)
                    String country = user.getCountry();
                    String countryCode = (country != null && !country.isEmpty())
                            ? country.substring(0, Math.min(3, country.length())).toUpperCase()
                            : "NT118";
                    tvCountry.setText(countryCode);

                    // Load profile image if available
                    String avatarUrl = user.getUrlAvatar();
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        // Load image using Glide
                        Glide.with(requireContext())
                                .load(avatarUrl)
                                .apply(new RequestOptions()
                                        .placeholder(R.drawable.ic_profile)
                                        .error(R.drawable.ic_profile)
                                        .circleCrop())
                                .into(ivProfileImage);
                    }

                    // Store user ID for later use
                    currentUserId = user.getId();
                } else {
                    handleApiError("user profile");
                }

                completeLoad();  // Mark this API call as complete
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Throwable t) {
                handleNetworkError(t);
                completeLoad();  // Mark this API call as complete even on failure
            }
        });
    }

    /**
     * Fetches all recipes and counts how many belong to the current user.
     * Updates UI with upload count and sets user level based on recipe count.
     */
    private void fetchUploadedRecipesCount() {
        ApiService apiService = RetrofitClient.getApiService();
        apiService.getAllRecipes("Bearer " + token).enqueue(new Callback<ModelResponse.RecipeResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Response<ModelResponse.RecipeResponse> response) {
                if (!isAdded()) {
                    completeLoad();
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<ModelResponse.RecipeResponse.Recipe> allRecipes = response.body().getData().getRecipes();

                    // Count recipes belonging to current user
                    int uploadedCount = countUserRecipes(allRecipes);

                    // Update UI with count and level
                    tvNumberUploadedRecipes.setText(String.valueOf(uploadedCount));
                    setUserLevel(uploadedCount);
                } else {
                    handleApiError("uploaded recipes");
                }

                completeLoad();  // Mark this API call as complete
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Throwable t) {
                handleNetworkError(t);
                completeLoad();  // Mark this API call as complete even on failure
            }
        });
    }

    /**
     * Counts the number of recipes that belong to the current user.
     * @param recipes List of all recipes
     * @return Count of user's recipes
     */
    private int countUserRecipes(List<ModelResponse.RecipeResponse.Recipe> recipes) {
        if (currentUserId == null) return 0;

        int count = 0;
        for (ModelResponse.RecipeResponse.Recipe recipe : recipes) {
            if (recipe.getAuthor().equals(currentUserId)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Sets the user's level based on their recipe count.
     * @param recipeCount Number of recipes uploaded by user
     */
    private void setUserLevel(int recipeCount) {
        String level;
        if (recipeCount == 0) {
            level = "Guest";
        } else if (recipeCount < 5) {
            level = "Beginner";
        } else if (recipeCount < 15) {
            level = "Intermediate";
        } else if (recipeCount < 30) {
            level = "Chef";
        } else {
            level = "Master Chef";
        }
        tvLevel.setText(level);
    }

    /**
     * Fetches the count of recipes saved by the current user.
     * Updates UI with the saved recipes count.
     */
    private void fetchSavedRecipesCount() {
        ApiService apiService = RetrofitClient.getApiService();
        apiService.getUserInfo("Bearer " + token).enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Response<ModelResponse.UserResponse> response) {
                if (!isAdded()) {
                    completeLoad();
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    // Get saved recipes array and count items
                    List<String> savedRecipes = response.body().getData().getUser().getSavedRecipes();
                    int savedCount = (savedRecipes != null) ? savedRecipes.size() : 0;

                    // Update UI with count
                    tvNumberSavedRecipes.setText(String.valueOf(savedCount));
                } else {
                    handleApiError("saved recipes");
                }

                completeLoad();  // Mark this API call as complete
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Throwable t) {
                handleNetworkError(t);
                completeLoad();  // Mark this API call as complete even on failure
            }
        });
    }

    /**
     * Handles API errors by showing a toast and logging the error.
     * @param resourceName Name of the resource that failed to load
     */
    private void handleApiError(String resourceName) {
        if (isAdded()) {
            String message = "Failed to load " + resourceName;
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            Log.e(TAG, message);
        }
    }

    /**
     * Handles network errors by showing a toast and logging the error.
     * @param t The throwable representing the network error
     */
    private void handleNetworkError(Throwable t) {
        if (isAdded()) {
            String message = "Network error: " + t.getMessage();
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            Log.e(TAG, message);
        }
    }

    /**
     * Shows the loading overlay with optional safety timeout.
     * @param withSafetyTimeout Whether to apply safety timeout to prevent infinite loading
     */
    private void showLoading(boolean withSafetyTimeout) {
        if (loadingOverlay != null) {
            // Ensure overlay is visible and in front of other elements
            loadingOverlay.bringToFront();
            loadingOverlay.setVisibility(View.VISIBLE);

            // Apply safety timeout for initial loading
            if (withSafetyTimeout) {
                setLoadingTimeout();
            }
        }
    }

    /**
     * Sets a safety timeout to ensure loading doesn't get stuck.
     * Will force-hide loading after timeout period.
     */
    private void setLoadingTimeout() {
        handler.postDelayed(() -> {
            if (isAdded() && loadingOverlay != null &&
                    loadingOverlay.getVisibility() == View.VISIBLE) {
                Log.w(TAG, "Loading safety timeout triggered - forcing hide");
                pendingLoads.set(0);
                hideLoadingAndEnableInteraction();
            }
        }, LOADING_TIMEOUT_MS);
    }

    /**
     * Hides the loading overlay.
     */
    private void hideLoading() {
        if (loadingOverlay != null && isAdded()) {
            loadingOverlay.setVisibility(View.GONE);
        }
    }

    /**
     * Marks a loading operation as complete and updates loading state.
     * Hides loading overlay when all operations are complete.
     */
    private void completeLoad() {
        int remaining = pendingLoads.decrementAndGet();

        if (remaining <= 0) {
            // Reset counter and mark initial load as complete
            pendingLoads.set(0);
            initialLoadComplete = true;

            // Hide loading and enable user interaction
            hideLoadingAndEnableInteraction();
        }
    }

    /**
     * Hides loading overlay and enables ViewPager interaction.
     * Called when all data is loaded and UI is ready for interaction.
     */
    private void hideLoadingAndEnableInteraction() {
        hideLoading();

        // Enable ViewPager interaction now that data is loaded
        if (viewPager != null) {
            viewPager.setUserInputEnabled(true);
        }
    }

    /**
     * Refreshes data when fragment resumes, if initial data was already loaded.
     * This ensures data is updated when returning from other screens.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (initialLoadComplete) {
            refreshData();
        }
    }
}