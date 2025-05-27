    package fragment;

    import static android.app.Activity.RESULT_OK;

    import android.content.Context;
    import android.content.Intent;
    import android.os.Bundle;
    import android.provider.OpenableColumns;
    import android.text.Editable;
    import android.text.TextWatcher;
    import android.util.Log;
    import android.view.KeyEvent;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.view.inputmethod.EditorInfo;
    import android.view.inputmethod.InputMethodManager;
    import android.widget.EditText;
    import android.widget.ImageView;
    import android.widget.TextView;
    import android.widget.Toast;

    import androidx.annotation.NonNull;
    import androidx.annotation.Nullable;
    import androidx.core.widget.NestedScrollView;
    import androidx.fragment.app.Fragment;
    import androidx.recyclerview.widget.GridLayoutManager;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;

    import com.bumptech.glide.Glide;
    import com.bumptech.glide.request.RequestOptions;
    import com.example.appfood.AddRecipeActivity;
    import com.example.appfood.ChatBotActivity;
    import com.example.appfood.ImageSearchResultsActivity;
    import com.example.appfood.MainActivity;
    import com.example.appfood.MainRecipe;
    import com.example.appfood.R;
    import com.example.appfood.UserProfileActivity;
    import com.example.appfood.RecipeCache;
    import com.google.android.material.floatingactionbutton.FloatingActionButton;

    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;
    import java.util.concurrent.atomic.AtomicInteger;

    import adapter.CommonRecipeAdapter;
    import adapter.NewRecipeAdapter;
    import adapter.SearchResultAdapter;
    import api.ApiService;
    import api.ModelResponse;
    import api.RetrofitClient;
    import retrofit2.Call;
    import retrofit2.Callback;
    import retrofit2.Response;

    import androidx.activity.result.ActivityResultLauncher;
    import androidx.activity.result.contract.ActivityResultContracts;
    import android.net.Uri;
    import android.provider.MediaStore;
    import okhttp3.MediaType;
    import okhttp3.MultipartBody;
    import okhttp3.RequestBody;
    import java.io.File;
    import java.io.InputStream;
    import android.database.Cursor;
    import android.content.ContentResolver;
    import java.io.FileOutputStream;
    import android.os.Environment;
    import java.util.ArrayList;

    /**
     * HomeFragment displays lists of common and new recipes.
     */
    public class HomeFragment extends Fragment {

        private ActivityResultLauncher<Intent> imagePickerLauncher;
        // Add this field at the top of your HomeFragment class
        private static final int MAX_IMAGE_SIZE = 1024 * 1024; // 1MB

        private static final String TAG = "HomeFragment";
        private static final String BEARER_PREFIX = "Bearer ";
        private static final int MAX_RECIPES_TO_DISPLAY = 10;
        private static final int CHATBOT_REQUEST_CODE = 1001;

        // UI Components
        private RecyclerView rvCommonRecipes, rvNewRecipes, rvSearchResults;
        private TextView tvGreeting;
        private ImageView ivProfile, ivCamera;
        private FloatingActionButton fabAddRecipe;
        private View loadingOverlay;
        private EditText etSearch;
        private View nestedScrollView, searchResultsContainer;
        private TextView tvNoResults;
        private View btnAI;

        // Data
        private List<ModelResponse.RecipeResponse.Recipe> commonRecipeList, newRecipeList, searchResultsList;
        private CommonRecipeAdapter commonRecipeAdapter;
        private NewRecipeAdapter newRecipeAdapter;
        private SearchResultAdapter searchResultAdapter;
        private String token;
        private Map<String, Boolean> savedRecipesMap = new HashMap<>();

        // Loading state tracking
        private AtomicInteger pendingLoads = new AtomicInteger(0);
        private boolean initialLoadComplete = false;
        private boolean isSearchActive = false;

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

            // Register image picker launcher
            registerImagePicker();

            // Set up search functionality
            setupSearch();

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
            ivCamera = view.findViewById(R.id.iv_camera);
            fabAddRecipe = view.findViewById(R.id.fab_add);
            etSearch = view.findViewById(R.id.et_search);
            nestedScrollView = view.findViewById(R.id.nested_scroll_view);

            // Add search results RecyclerView and container
            searchResultsContainer = view.findViewById(R.id.search_results_container);
            rvSearchResults = view.findViewById(R.id.rv_search_results);
            tvNoResults = view.findViewById(R.id.tv_no_results);

            if (searchResultsContainer != null) {
                searchResultsContainer.setVisibility(View.GONE); // Hide initially
            }

            // Setup AI button
            btnAI = view.findViewById(R.id.btn_ai);
            if (btnAI != null) {
                btnAI.setOnClickListener(v -> navigateToChatBot());
            }

            setupProfileUI();
            setupScrollListener(view);
            setupAddRecipeButton();
            setupCameraButton();
        }

        /**
         * Navigates to the ChatBot activity
         */
        private void navigateToChatBot() {
            Intent intent = new Intent(requireContext(), ChatBotActivity.class);
            intent.putExtra("token", token);
            startActivity(intent);
        }

        /**
         * Registers the image picker launcher
         */
        private void registerImagePicker() {
            imagePickerLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                            Uri selectedImageUri = result.getData().getData();
                            if (selectedImageUri != null) {
                                processSelectedImage(selectedImageUri);
                            }
                        }
                    }
            );
        }

        /**
         * Sets up camera button click listener
         */
        private void setupCameraButton() {
            if (ivCamera != null) {
                ivCamera.setOnClickListener(v -> openImagePicker());
            }
        }

        /**
         * Opens the image picker
         */
        private void openImagePicker() {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        }

        /**
         * Processes the selected image and calls the API
         */
        private void processSelectedImage(Uri imageUri) {
            showLoading();

            try {
                // Get the file from the URI
                File imageFile = getFileFromUri(imageUri);
                if (imageFile == null) {
                    Toast.makeText(requireContext(), "Failed to process image", Toast.LENGTH_SHORT).show();
                    hideLoading();
                    return;
                }

                // Create multipart request
                RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
                MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", imageFile.getName(), requestFile);

                // Call API
                callSearchImageApi(imagePart);

            } catch (Exception e) {
                Log.e(TAG, "Error processing image: " + e.getMessage());
                Toast.makeText(requireContext(), "Error processing image", Toast.LENGTH_SHORT).show();
                hideLoading();
            }
        }

        /**
         * Converts Uri to File
         */
        private File getFileFromUri(Uri uri) {
            try {
                ContentResolver contentResolver = requireContext().getContentResolver();
                String fileName = getFileNameFromUri(contentResolver, uri);

                // Create temporary file
                File outputDir = requireContext().getCacheDir();
                File outputFile = File.createTempFile("image_", ".jpg", outputDir);

                // Copy data to file
                InputStream inputStream = contentResolver.openInputStream(uri);
                FileOutputStream outputStream = new FileOutputStream(outputFile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.close();
                inputStream.close();

                return outputFile;
            } catch (Exception e) {
                Log.e(TAG, "Error getting file from uri: " + e.getMessage());
                return null;
            }
        }

        /**
         * Gets filename from Uri
         */
        private String getFileNameFromUri(ContentResolver contentResolver, Uri uri) {
            String result = null;
            if (uri.getScheme().equals("content")) {
                Cursor cursor = contentResolver.query(uri, null, null, null, null);
                try {
                    if (cursor != null && cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (nameIndex >= 0) {
                            result = cursor.getString(nameIndex);
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
            if (result == null) {
                result = uri.getLastPathSegment();
            }
            return result;
        }

        /**
         * Calls the search image API
         */
        private void callSearchImageApi(MultipartBody.Part imagePart) {
            ApiService apiService = RetrofitClient.getApiService();
            Call<ModelResponse.searchImageResponse> call = apiService.searchImage(BEARER_PREFIX + token, imagePart);

            call.enqueue(new Callback<ModelResponse.searchImageResponse>() {
                @Override
                public void onResponse(@NonNull Call<ModelResponse.searchImageResponse> call,
                                       @NonNull Response<ModelResponse.searchImageResponse> response) {
                    hideLoading();

                    if (response.isSuccessful() && response.body() != null) {
                        processSearchImageResponse(response.body());
                    } else {
                        Toast.makeText(requireContext(), "Failed to search with image", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "API error: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ModelResponse.searchImageResponse> call, @NonNull Throwable t) {
                    hideLoading();
                    Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "API call failed: " + t.getMessage());
                }
            });
        }

        /**
         * Processes the search image response
         */
        private void processSearchImageResponse(ModelResponse.searchImageResponse response) {
            if (response.isSuccess() && response.getData() != null) {
                // Extract recipe IDs from response
                ArrayList<String> recipeIds = new ArrayList<>();
                ArrayList<String> recipeTitles = new ArrayList<>();
                ArrayList<Double> similarities = new ArrayList<>();

                // Get suggested dishes and ingredients
                ArrayList<String> suggestedDishes = new ArrayList<>();
                ArrayList<String> extractedIngredients = new ArrayList<>();

                if (response.getData().getSuggestedDishes() != null) {
                    suggestedDishes.addAll(response.getData().getSuggestedDishes());
                }

                if (response.getData().getExtractedIngredients() != null) {
                    extractedIngredients.addAll(response.getData().getExtractedIngredients());
                }

                // Get recipe IDs from API response
                if (response.getData().getRecipes() != null &&
                        response.getData().getRecipes().getRecipes() != null) {

                    List<ModelResponse.searchImageResponse.Recipe> recipes =
                            response.getData().getRecipes().getRecipes();

                    for (ModelResponse.searchImageResponse.Recipe recipe : recipes) {
                        recipeIds.add(recipe.getId());
                        recipeTitles.add(recipe.getTitle());
                        similarities.add(recipe.getSimilarity());
                    }

                    // Navigate to results activity
                    navigateToImageSearchResults(
                            recipeIds,
                            recipeTitles,
                            similarities,
                            suggestedDishes,
                            extractedIngredients);
                } else {
                    Toast.makeText(requireContext(), "No recipes found", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "Search failed", Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * Navigate to image search results activity
         */
        private void navigateToImageSearchResults(
                ArrayList<String> recipeIds,
                ArrayList<String> recipeTitles,
                ArrayList<Double> similarities,
                ArrayList<String> suggestedDishes,
                ArrayList<String> extractedIngredients) {

            Intent intent = new Intent(requireContext(), ImageSearchResultsActivity.class);
            intent.putExtra("token", token);
            intent.putStringArrayListExtra("recipe_ids", recipeIds);
            intent.putStringArrayListExtra("recipe_titles", recipeTitles);
            intent.putExtra("similarities", similarities);
            intent.putStringArrayListExtra("suggested_dishes", suggestedDishes);
            intent.putStringArrayListExtra("extracted_ingredients", extractedIngredients);
            startActivity(intent);
        }

        /**
         * Sets up search functionality
         */
        private void setupSearch() {
            if (etSearch == null) return;

            // Initialize search results list and adapter
            searchResultsList = new ArrayList<>();
            searchResultAdapter = new SearchResultAdapter(
                    requireContext(),
                    searchResultsList,
                    this::navigateToRecipeDetails);

            // Set up RecyclerView for search results
            if (rvSearchResults != null) {
                rvSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
                rvSearchResults.setAdapter(searchResultAdapter);
            }

            // Set up search action listener (when user presses search button on keyboard)
            etSearch.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    performSearch(etSearch.getText().toString());
                    hideKeyboard();
                    return true;
                }
                return false;
            });

            // Set up text change listener for real-time search
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Not needed
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Handle search as user types
                    if (s.length() >= 2) { // Only search if 2 or more characters
                        performSearch(s.toString());
                    } else if (s.length() == 0) {
                        // Hide search results when search box is cleared
                        hideSearchResults();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // Not needed
                }
            });
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
         * Performs search on cached recipes
         */
        private void performSearch(String query) {
            if (query == null || query.trim().isEmpty()) {
                hideSearchResults();
                return;
            }

            List<ModelResponse.RecipeResponse.Recipe> results =
                    RecipeCache.searchRecipesByTitle(requireContext(), query);

            if (results.isEmpty()) {
                // Show "no results" message
                if (tvNoResults != null) {
                    tvNoResults.setVisibility(View.VISIBLE);
                    rvSearchResults.setVisibility(View.GONE);
                }
                showSearchResults(); // Still show the container
            } else {
                // Update and display search results
                if (tvNoResults != null) {
                    tvNoResults.setVisibility(View.GONE);
                    rvSearchResults.setVisibility(View.VISIBLE);
                }
                searchResultsList.clear();
                searchResultsList.addAll(results);
                searchResultAdapter.notifyDataSetChanged();
                showSearchResults();

                Log.d(TAG, "Found " + results.size() + " recipes matching '" + query + "'");
            }
        }

        /**
         * Shows search results container and hides main content
         */
        private void showSearchResults() {
            if (searchResultsContainer != null && nestedScrollView != null) {
                searchResultsContainer.setVisibility(View.VISIBLE);
                nestedScrollView.setVisibility(View.GONE);
                isSearchActive = true;
            }
        }

        /**
         * Hides search results container and shows main content
         */
        private void hideSearchResults() {
            if (searchResultsContainer != null && nestedScrollView != null) {
                searchResultsContainer.setVisibility(View.GONE);
                nestedScrollView.setVisibility(View.VISIBLE);
                isSearchActive = false;

                if (tvNoResults != null) {
                    tvNoResults.setVisibility(View.GONE);
                }
            }
        }

        /**
         * Hides the keyboard
         */
        private void hideKeyboard() {
            if (getActivity() != null) {
                InputMethodManager imm = (InputMethodManager) getActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null && etSearch != null) {
                    imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                }
            }
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

            // Cache all recipes for search functionality
            loadAllRecipes();
        }

        /**
         * Loads common recipes from API
         */
        private void loadCommonRecipes() {
            registerPendingLoad();

            ApiService apiService = RetrofitClient.getApiService();
            Call<ModelResponse.RecipeResponse> call = apiService.getRandomRecipe(BEARER_PREFIX + token);

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
         * Loads all recipes for caching and search functionality
         */
        private void loadAllRecipes() {
            registerPendingLoad();

            ApiService apiService = RetrofitClient.getApiService();
            Call<ModelResponse.RecipeResponse> call = apiService.getAllRecipes(BEARER_PREFIX + token);

            call.enqueue(new Callback<ModelResponse.RecipeResponse>() {
                @Override
                public void onResponse(@NonNull Call<ModelResponse.RecipeResponse> call,
                                       @NonNull Response<ModelResponse.RecipeResponse> response) {
                    if (isSuccessfulRecipeResponse(response)) {
                        // Save recipes to cache for search functionality
                        List<ModelResponse.RecipeResponse.Recipe> allRecipes = response.body().getData().getRecipes();
                        RecipeCache.saveRecipesToCache(requireContext(), allRecipes);
                        Log.d(TAG, "Cached " + allRecipes.size() + " recipes for search functionality");
                    } else {
                        Log.e(TAG, "Failed to load all recipes for caching");
                    }
                    completeLoad();
                }

                @Override
                public void onFailure(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "API Call Failed (All Recipes): " + t.getMessage());
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

            // Clear search if active
            if (isSearchActive && etSearch != null) {
                etSearch.setText("");
                hideSearchResults();
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