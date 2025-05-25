package com.example.appfood;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import adapter.CommentAdapter;
import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity for displaying and managing comments on a recipe
 */
public class CommentsRecipe extends AppCompatActivity implements CommentAdapter.OnCommentActionListener {
    private static final String TAG = "CommentsRecipe";
    private static final String BEARER_PREFIX = "Bearer ";

    // Auto refresh settings
    private boolean isAutoRefreshEnabled = true;
    private long refreshInterval = 10000; // 10 seconds
    private Handler refreshHandler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;

    // Comments data
    private final List<ModelResponse.CommentResponse.Comment> commentList = new ArrayList<>();
    private String token;
    private String recipeId;

    // Loading counter to track API calls
    private final AtomicInteger loadingCounter = new AtomicInteger(0);

    // UI elements
    private EditText etComment;
    private RecyclerView rvComments;
    private CommentAdapter commentAdapter;
    private FrameLayout loadingOverlay;
    private TextView tvCommentsCount;

    // User info
    private String userId;
    private String url_avatar;
    private String recipeName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments_recipe);

        // Get token and recipeId from intent
        extractIntentData();

        // Initialize UI elements
        initializeViews();

        // Set up click listeners
        setupClickListeners();

        // Get user info and load comments
        loadInitialData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startAutoRefresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAutoRefresh();
    }

    /**
     * Start automatic refresh of comments
     */
    private void startAutoRefresh() {
        if (isAutoRefreshEnabled) {
            refreshRunnable = new Runnable() {
                @Override
                public void run() {
                    refreshCommentsInBackground();
                    // Re-run this after delay
                    refreshHandler.postDelayed(this, refreshInterval);
                }
            };
            refreshHandler.postDelayed(refreshRunnable, refreshInterval);
        }
    }

    /**
     * Stop automatic refresh of comments
     */
    private void stopAutoRefresh() {
        if (refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    /**
     * Refresh comments in background without showing loading indicator
     */
    private void refreshCommentsInBackground() {
        if (token == null || recipeId == null) {
            return;
        }

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.CommentResponse> call = apiService.getRecipeComments(BEARER_PREFIX + token, recipeId);

        call.enqueue(new Callback<ModelResponse.CommentResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.CommentResponse> call,
                                   @NonNull Response<ModelResponse.CommentResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ModelResponse.CommentResponse commentResponse = response.body();

                    if ("success".equals(commentResponse.getStatus()) &&
                            commentResponse.getData() != null &&
                            commentResponse.getData().getComments() != null) {

                        List<ModelResponse.CommentResponse.Comment> newComments =
                                commentResponse.getData().getComments();

                        // Check if comments have changed
                        if (commentsHaveChanged(commentList, newComments)) {
                            Log.d(TAG, "Comments updated in background");

                            // Update our list
                            commentList.clear();
                            commentList.addAll(newComments);

                            // Update UI on the main thread
                            runOnUiThread(() -> updateUI());
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.CommentResponse> call, @NonNull Throwable t) {
                // Silent failure in background
                Log.e(TAG, "Background comments refresh failed", t);
            }
        });
    }

    /**
     * Check if comments have changed
     */
    private boolean commentsHaveChanged(
            List<ModelResponse.CommentResponse.Comment> oldList,
            List<ModelResponse.CommentResponse.Comment> newList) {

        if (oldList.size() != newList.size()) {
            return true;
        }

        // Compare comments for changes
        for (int i = 0; i < oldList.size(); i++) {
            if (!oldList.get(i).getId().equals(newList.get(i).getId()) ||
                    !oldList.get(i).getContent().equals(newList.get(i).getContent()) ||
                    oldList.get(i).getLikes() != newList.get(i).getLikes() ||
                    oldList.get(i).getDislikes() != newList.get(i).getDislikes()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extract data from intent
     */
    private void extractIntentData() {
        token = getIntent().getStringExtra("token");
        recipeId = getIntent().getStringExtra("recipeId");
        recipeName = getIntent().getStringExtra("recipeName");

        if (token == null || recipeId == null) {
            Toast.makeText(this, "Missing token or recipe ID", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Initialize all UI components from layout
     */
    private void initializeViews() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView tvTitle = findViewById(R.id.tvTitle);
        tvCommentsCount = findViewById(R.id.tvCommentsCount);
        etComment = findViewById(R.id.etComment);
        View btnSend = findViewById(R.id.btnSend);
        rvComments = findViewById(R.id.rvComments);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        // Set up RecyclerView
        commentAdapter = new CommentAdapter(this, commentList, this);
        commentAdapter.setToken(token);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);

        // Set title
        tvTitle.setText(recipeName != null ? "Reviews for " + recipeName : "Reviews");

        // Back button click listener
        btnBack.setOnClickListener(v -> finish());
    }

    /**
     * Set up click listeners for interactive elements
     */
    private void setupClickListeners() {
        // Send button click listener
        findViewById(R.id.btnSend).setOnClickListener(v -> {
            String commentText = etComment.getText().toString().trim();
            if (!commentText.isEmpty()) {
                postComment(commentText);
            } else {
                Toast.makeText(CommentsRecipe.this, "Please enter a comment", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Load initial data (user info and comments)
     */
    private void loadInitialData() {
        showLoading(true);
        getUserInfo();
        getComments();
    }

    /**
     * Get comments for a recipe from the API
     */
    private void getComments() {
        // Increment loading counter
        loadingCounter.incrementAndGet();

        Log.d(TAG, "Fetching comments for recipe ID: " + recipeId);

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.CommentResponse> call = apiService.getRecipeComments(BEARER_PREFIX + token, recipeId);

        call.enqueue(new Callback<ModelResponse.CommentResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.CommentResponse> call,
                                   @NonNull Response<ModelResponse.CommentResponse> response) {
                // Decrement loading counter
                checkAndUpdateLoadingState();

                if (response.isSuccessful() && response.body() != null) {
                    handleSuccessfulCommentsResponse(response.body());
                } else if (response.body() == null) {
                    Toast.makeText(CommentsRecipe.this, "No comments for this recipe!", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Response body is null");
                } else {
                    handleErrorResponse(response, "Failed to load comments");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.CommentResponse> call, @NonNull Throwable t) {
                checkAndUpdateLoadingState();
                showError("Network error: " + t.getMessage());
                Log.e(TAG, "API call failed", t);
            }
        });
    }

    /**
     * Handle successful comments response
     */
    private void handleSuccessfulCommentsResponse(ModelResponse.CommentResponse commentResponse) {
        Log.d(TAG, "API Response status: " + commentResponse.getStatus());

        if ("success".equals(commentResponse.getStatus()) &&
                commentResponse.getData() != null &&
                commentResponse.getData().getComments() != null) {

            List<ModelResponse.CommentResponse.Comment> newComments = commentResponse.getData().getComments();

            // Store comments data
            commentList.clear();
            commentList.addAll(newComments);

            // Update UI with comments data
            updateUI();
        } else {
            showError("Failed to load comments");
            Log.e(TAG, "Comment data structure is invalid: " + commentResponse);
        }
    }

    /**
     * Post a new comment to the recipe
     * @param commentText Comment content
     */
    private void postComment(String commentText) {
        showLoading(true);
        Log.d(TAG, "Attempting to post comment for recipe ID: " + recipeId);

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.RecipeDetailResponse> call = apiService.postComment(
                BEARER_PREFIX + token,
                recipeId,
                commentText
        );

        call.enqueue(new Callback<ModelResponse.RecipeDetailResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.RecipeDetailResponse> call,
                                   @NonNull Response<ModelResponse.RecipeDetailResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    // Comment posted successfully
                    Log.d(TAG, "Comment posted successfully: " + response.body().getStatus());
                    etComment.setText("");
                    Toast.makeText(CommentsRecipe.this, "Comment added successfully", Toast.LENGTH_SHORT).show();
                    getComments(); // Refresh comments list
                } else {
                    handleErrorResponse(response, "Failed to post comment");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.RecipeDetailResponse> call, @NonNull Throwable t) {
                showLoading(false);
                showError("Network error while posting comment: " + t.getMessage());
                Log.e(TAG, "Network error while posting comment", t);
            }
        });
    }

    /**
     * Delete a comment
     * @param commentId ID of the comment to delete
     * @param position Position of the comment in the list
     */
    private void deleteComment(String commentId, int position) {
        if (token == null || commentId == null) {
            showError("Missing required data for deleting comment");
            return;
        }

        // Show confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    proceedWithCommentDeletion(commentId, position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Proceed with the actual comment deletion after confirmation
     */
    private void proceedWithCommentDeletion(String commentId, int position) {
        showLoading(true);
        Log.d(TAG, "Deleting comment ID: " + commentId);

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.readNotificationResponse> call = apiService.deleteComment(
                BEARER_PREFIX + token,
                commentId
        );

        call.enqueue(new Callback<ModelResponse.readNotificationResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.readNotificationResponse> call,
                                   @NonNull Response<ModelResponse.readNotificationResponse> response) {
                showLoading(false);

                if (response.isSuccessful()) {
                    // Comment deleted successfully
                    Log.d(TAG, "Comment deleted successfully");
                    Toast.makeText(CommentsRecipe.this, "Comment deleted successfully", Toast.LENGTH_SHORT).show();

                    // Remove the comment from list and update UI
                    if (position >= 0 && position < commentList.size()) {
                        commentList.remove(position);
                        commentAdapter.notifyItemRemoved(position);
                        tvCommentsCount.setText(String.format("%d Comments", commentList.size()));
                    } else {
                        getComments(); // Refresh all comments if position is invalid
                    }
                } else {
                    handleErrorResponse(response, "Failed to delete comment");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.readNotificationResponse> call, @NonNull Throwable t) {
                showLoading(false);
                showError("Network error while deleting comment: " + t.getMessage());
                Log.e(TAG, "Network error while deleting comment", t);
            }
        });
    }

    /**
     * Get user info with token
     */
    private void getUserInfo() {
        loadingCounter.incrementAndGet();

        ApiService apiService = RetrofitClient.getApiService();
        apiService.getUserInfo(BEARER_PREFIX + token).enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.UserResponse> call,
                                   @NonNull Response<ModelResponse.UserResponse> response) {
                checkAndUpdateLoadingState();

                if (response.isSuccessful() && response.body() != null &&
                        response.body().getData() != null &&
                        response.body().getData().getUser() != null) {

                    userId = response.body().getData().getUser().getId();
                    url_avatar = response.body().getData().getUser().getUrlAvatar();

                    // Set the user ID and avatar URL in the adapter
                    if (commentAdapter != null) {
                        commentAdapter.setCurrentUserId(userId);
                        if (url_avatar != null) {
                            commentAdapter.setUserAvatarUrl(url_avatar);
                        }
                    }
                } else {
                    handleErrorResponse(response, "Error loading user info");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Throwable t) {
                checkAndUpdateLoadingState();
                Log.e(TAG, "Network error while loading user info", t);
            }
        });
    }

    /**
     * Handle reaction to a comment (like or dislike)
     *
     * @param commentId The ID of the comment to react to
     * @param isLike True for like, false for dislike
     */
    private void reactToComment(String commentId, boolean isLike) {
        if (token == null || recipeId == null || commentId == null) {
            showError("Missing required data for comment reaction");
            return;
        }

        showLoading(true);
        Log.d(TAG, (isLike ? "Liking" : "Disliking") + " comment ID: " + commentId);

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.RecipeDetailResponse> call;

        if (isLike) {
            call = apiService.likeComment(BEARER_PREFIX + token, recipeId, commentId);
        } else {
            call = apiService.dislikeComment(BEARER_PREFIX + token, recipeId, commentId);
        }

        call.enqueue(new Callback<ModelResponse.RecipeDetailResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.RecipeDetailResponse> call,
                                   @NonNull Response<ModelResponse.RecipeDetailResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    String action = isLike ? "liked" : "disliked";
                    Log.d(TAG, "Comment " + action + " successfully: " + response.body().getStatus());
                    getComments(); // Refresh comments
                } else {
                    String errorMsg = "Failed to " + (isLike ? "like" : "dislike") + " comment";
                    handleErrorResponse(response, errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.RecipeDetailResponse> call, @NonNull Throwable t) {
                showLoading(false);
                String action = isLike ? "liking" : "disliking";
                showError("Network error while " + action + " comment: " + t.getMessage());
                Log.e(TAG, "Network error while " + action + " comment", t);
            }
        });
    }

    /**
     * Handle error response from API
     */
    private void handleErrorResponse(Response<?> response, String defaultErrorMsg) {
        String errorMsg = defaultErrorMsg;
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
        tvCommentsCount.setText(String.format("%d Comments", commentList.size()));

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
     * Implementation of the OnCommentActionListener interface
     */
    @Override
    public void onLikeClicked(String commentId, int position) {
        reactToComment(commentId, true);
    }

    @Override
    public void onDislikeClicked(String commentId, int position) {
        reactToComment(commentId, false);
    }

    @Override
    public void onDeleteClicked(String commentId, int position) {
        deleteComment(commentId, position);
    }

    /**
     * Enable or disable auto-refresh
     */
    public void setAutoRefreshEnabled(boolean enabled) {
        this.isAutoRefreshEnabled = enabled;
        if (enabled) {
            startAutoRefresh();
        } else {
            stopAutoRefresh();
        }
    }

    /**
     * Set refresh interval
     */
    public void setRefreshInterval(long milliseconds) {
        this.refreshInterval = milliseconds;
        if (isAutoRefreshEnabled) {
            stopAutoRefresh();
            startAutoRefresh();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAutoRefresh();
    }
}