package com.example.appfood;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import adapter.CommentAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommentsRecipe extends AppCompatActivity implements CommentAdapter.OnCommentActionListener{
    private static final String TAG = "CommentsRecipe";

    // Comments data
    private List<ModelResponse.CommentResponse.Comment> commentList = new ArrayList<>();
    private String token;
    private String recipeId;

    // Loading counter to track API calls
    private AtomicInteger loadingCounter = new AtomicInteger(0);

    // UI elements
    private ImageButton btnBack;
    private TextView tvTitle;
    private TextView tvCommentsCount;
    private TextView tvSavedCount;
    private TextView tvLeaveComment;
    private EditText etComment;
    private View btnSend;
    private RecyclerView rvComments;
    private CommentAdapter commentAdapter;
    private FrameLayout loadingOverlay;

    // User info
    private String userId;
    private String url_avatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments_recipe);

        // Get token and recipeId from intent
        token = getIntent().getStringExtra("token");
        recipeId = getIntent().getStringExtra("recipeId");

        if (token == null || recipeId == null) {
            Toast.makeText(this, "Missing token or recipe ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI elements
        initializeViews();

        // Set up click listeners
        setupClickListeners();

        // Get user info
        getUserInfo(token);

        // Show loading state
        showLoading(true);

        // Load comments data
        getComments(token, recipeId);
    }

    /**
     * Initialize all UI components from layout
     */
    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        tvCommentsCount = findViewById(R.id.tvCommentsCount);
        tvLeaveComment = findViewById(R.id.tvLeaveComment);
        etComment = findViewById(R.id.etComment);
        btnSend = findViewById(R.id.btnSend);
        rvComments = findViewById(R.id.rvComments);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        // Set up RecyclerView
        commentAdapter = new CommentAdapter(this, commentList, this);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);

        // Set title
        tvTitle.setText("Reviews");
    }

    /**
     * Set up click listeners for interactive elements
     */
    private void setupClickListeners() {
        // Back button click listener
        btnBack.setOnClickListener(v -> finish());

        // Send button click listener
        btnSend.setOnClickListener(v -> {
            String commentText = etComment.getText().toString().trim();
            if (!commentText.isEmpty()) {
                postComment(token, recipeId, commentText);
            } else {
                Toast.makeText(CommentsRecipe.this, "Please enter a comment", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Get comments for a recipe from the API
     * @param token Authentication token
     * @param recipeId Recipe ID
     */
    private void getComments(String token, String recipeId) {
        // Increment loading counter
        loadingCounter.incrementAndGet();

        Log.d(TAG, "Fetching comments for recipe ID: " + recipeId);

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.CommentResponse> call = apiService.getRecipeComments("Bearer " + token, recipeId);

        call.enqueue(new Callback<ModelResponse.CommentResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.CommentResponse> call,
                                   @NonNull Response<ModelResponse.CommentResponse> response) {
                // Decrement loading counter and check if we should hide loading overlay
                checkAndUpdateLoadingState();

                if (response.isSuccessful() && response.body() != null) {
                    ModelResponse.CommentResponse commentResponse = response.body();
                    Log.d(TAG, "API Response status: " + commentResponse.getStatus());

                    if ("success".equals(commentResponse.getStatus()) &&
                            commentResponse.getData() != null &&
                            commentResponse.getData().getComments() != null) {

                        // Store comments data
                        commentList.clear();
                        commentList.addAll(commentResponse.getData().getComments());

                        // Update UI with comments data
                        updateUI();
                    } else {
                        showError("Failed to load comments");
                        Log.e(TAG, "Comment data structure is invalid: " + response.body());
                    }
                } else {
                    String errorMsg = "Error loading comments";
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
            public void onFailure(@NonNull Call<ModelResponse.CommentResponse> call, @NonNull Throwable t) {
                // Decrement loading counter and check if we should hide loading overlay
                checkAndUpdateLoadingState();

                showError("Network error: " + t.getMessage());
                Log.e(TAG, "API call failed", t);
            }
        });
    }

    /**
     * Post a new comment to the recipe
     * @param token Authorization token
     * @param recipeId ID of the recipe
     * @param commentText Comment content
     */
    private void postComment(String token, String recipeId, String commentText) {
        // Show loading state
        showLoading(true);

        // Log the attempt
        Log.d(TAG, "Attempting to post comment for recipe ID: " + recipeId);

        // Get API service
        ApiService apiService = RetrofitClient.getApiService();

        // Make API call
        Call<ModelResponse.RecipeDetailResponse> call = apiService.postComment(
                "Bearer " + token,
                recipeId,
                commentText
        );

        call.enqueue(new Callback<ModelResponse.RecipeDetailResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.RecipeDetailResponse> call,
                                   @NonNull Response<ModelResponse.RecipeDetailResponse> response) {
                // Hide loading indicator
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    // Comment posted successfully
                    ModelResponse.RecipeDetailResponse recipeResponse = response.body();
                    Log.d(TAG, "Comment posted successfully: " + recipeResponse.getStatus());

                    // Clear the input field
                    if (etComment != null) {
                        etComment.setText("");
                    }

                    // Show success message
                    Toast.makeText(CommentsRecipe.this, "Comment added successfully", Toast.LENGTH_SHORT).show();

                    // Refresh the comments list to include the new comment
                    getComments(token, recipeId);

                } else {
                    // Handle error
                    String errorMsg = "Failed to post comment";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += ": " + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error response", e);
                    }

                    showError(errorMsg);
                    Log.e(TAG, errorMsg + " - HTTP Status: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.RecipeDetailResponse> call, @NonNull Throwable t) {
                // Hide loading indicator
                showLoading(false);

                // Handle network failure
                String errorMessage = "Network error while posting comment: " + t.getMessage();
                showError(errorMessage);
                Log.e(TAG, errorMessage, t);
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
     * Update UI with comments data
     */
    private void updateUI() {
        // Update comments count
        tvCommentsCount.setText(commentList.size() + " Comments");

        // Notify adapter that data has changed
        commentAdapter.notifyDataSetChanged();
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
     * Get user info with token
     */
    public void getUserInfo(String token) {
        ApiService apiService = RetrofitClient.getApiService();
        apiService.getUserInfo("Bearer " + token).enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.UserResponse> call, Response<ModelResponse.UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userId = response.body().getData().getUser().getId();
                    url_avatar = response.body().getData().getUser().getUrlAvatar();

                    // Set the avatar URL in the adapter when received
                    if (commentAdapter != null && url_avatar != null) {
                        commentAdapter.setUserAvatarUrl(url_avatar);
                    }
                } else {
                    String errorMsg = "Error loading user info";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += ": " + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error response", e);
                    }
                    Log.e(TAG, errorMsg);
                }
            }
            @Override
            public void onFailure(Call<ModelResponse.UserResponse> call, Throwable t) {
                String errorMessage = "Network error while loading user info: " + t.getMessage();
                Log.e(TAG, errorMessage, t);
            }
        });
    }

    /**
     * Handle like and dislike actions
     */

    public void onLikeClicked(String commentId, int position) {
        if (token == null || recipeId == null || commentId == null) {
            showError("Missing required data for liking comment");
            return;
        }

        // Show loading
        showLoading(true);

        // Log action
        Log.d(TAG, "Liking comment ID: " + commentId + " for recipe ID: " + recipeId);

        // Call API to like comment
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.RecipeDetailResponse> call = apiService.likeComment(
                "Bearer " + token,
                recipeId,
                commentId
        );

        call.enqueue(new Callback<ModelResponse.RecipeDetailResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.RecipeDetailResponse> call,
                                   @NonNull Response<ModelResponse.RecipeDetailResponse> response) {
                // Hide loading
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ModelResponse.RecipeDetailResponse detailResponse = response.body();
                    Log.d(TAG, "Comment liked successfully: " + detailResponse.getStatus());

                    // Reload all comments instead of updating just one
                    getComments(token, recipeId);


                } else {
                    String errorMsg = "Failed to like comment";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += ": " + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error response", e);
                    }

                    showError(errorMsg);
                    Log.e(TAG, errorMsg + " - HTTP Status: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.RecipeDetailResponse> call, @NonNull Throwable t) {
                // Hide loading
                showLoading(false);

                // Handle network failure
                String errorMessage = "Network error while liking comment: " + t.getMessage();
                showError(errorMessage);
                Log.e(TAG, errorMessage, t);
            }
        });
    }

    /**
     * Implementation of the OnCommentActionListener interface
     * Called when a user dislikes a comment
     */

    public void onDislikeClicked(String commentId, int position) {
        if (token == null || recipeId == null || commentId == null) {
            showError("Missing required data for disliking comment");
            return;
        }

        // Show loading
        showLoading(true);

        // Log action
        Log.d(TAG, "Disliking comment ID: " + commentId + " for recipe ID: " + recipeId);

        // Call API to dislike comment
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.RecipeDetailResponse> call = apiService.dislikeComment(
                "Bearer " + token,
                recipeId,
                commentId
        );

        call.enqueue(new Callback<ModelResponse.RecipeDetailResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.RecipeDetailResponse> call,
                                   @NonNull Response<ModelResponse.RecipeDetailResponse> response) {
                // Hide loading
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ModelResponse.RecipeDetailResponse detailResponse = response.body();
                    Log.d(TAG, "Comment disliked successfully: " + detailResponse.getStatus());

                    // Reload all comments instead of updating just one
                    getComments(token, recipeId);


                } else {
                    String errorMsg = "Failed to dislike comment";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += ": " + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error response", e);
                    }

                    showError(errorMsg);
                    Log.e(TAG, errorMsg + " - HTTP Status: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.RecipeDetailResponse> call, @NonNull Throwable t) {
                // Hide loading
                showLoading(false);

                // Handle network failure
                String errorMessage = "Network error while disliking comment: " + t.getMessage();
                showError(errorMessage);
                Log.e(TAG, errorMessage, t);
            }
        });
    }

    /**
     * Update a comment in the list after a like/dislike action
     */

}