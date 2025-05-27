package com.example.appfood;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import adapter.MessageAdapter;
import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import androidx.appcompat.app.AlertDialog;

public class ChatBotActivity extends AppCompatActivity implements MessageAdapter.OnRecipeClickListener {
    private static final String TAG = "ChatBotActivity";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String PREF_CHAT_HISTORY = "chat_history";
    private static final int MAX_CONCURRENT_REQUESTS = 5;
    private final ModelResponse modelResponse = new ModelResponse();
    // UI components
    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private EditText messageEditText;
    private ImageButton sendButton;
    private Toolbar toolbar;
    private FrameLayout loadingOverlay;

    // API and data
    private ApiService apiService;
    private SessionManager sessionManager;
    private SharedPreferences sharedPreferences;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_bot);

        // Get token from intent
        if (getIntent().hasExtra("token")) {
            token = getIntent().getStringExtra("token");
        } else {
            // Fallback to session manager if needed
            sessionManager = new SessionManager(this);
            token = sessionManager.getToken();

            if (token == null || token.isEmpty()) {
                Toast.makeText(this, "Authentication error: No token available", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        Log.d(TAG, "Received token: " + (token != null && !token.isEmpty() ? "Valid token" : "Invalid token"));

        // Initialize services
        apiService = RetrofitClient.getApiService();
        sharedPreferences = getSharedPreferences("ChatBotPrefs", MODE_PRIVATE);

        // Initialize UI components
        initializeViews();

        // Set up RecyclerView
        setupRecyclerView();

        // Load chat history
        loadChatHistory();

        // Add welcome message if it's a fresh chat
        if (messageAdapter.getItemCount() == 0) {
            addBotMessage("Hello! I'm your Food Advisor. Ask me about recipes and I'll help you find something delicious!");
        }
    }

    /**
     * Initialize UI components
     */
    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        toolbar = findViewById(R.id.toolbar);

        // Set up the clear history button
        ImageButton clearHistoryButton = findViewById(R.id.clearHistoryButton);
        clearHistoryButton.setOnClickListener(v -> showClearHistoryConfirmation());

        // Set up toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Set click listener for send button
        sendButton.setOnClickListener(v -> sendMessage());
    }

    /**
     * Shows confirmation dialog before clearing chat history
     */
    private void showClearHistoryConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Chat History")
                .setMessage("Are you sure you want to clear all chat history? This action cannot be undone.")
                .setPositiveButton("Clear", (dialog, which) -> {
                    clearChatHistory();
                    Toast.makeText(ChatBotActivity.this, "Chat history cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Set up RecyclerView with adapter
     */
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new MessageAdapter(this, this);
        recyclerView.setAdapter(messageAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            handleBackNavigation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Handle back navigation to ensure token is passed back
     */
    private void handleBackNavigation() {
        // Make sure chat history is saved before leaving
        saveChatHistory();

        // Return the token to the calling activity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("token", token);
        setResult(RESULT_OK, resultIntent);

        finish();
    }

    /**
     * Send message and search for recipes
     */
    private void sendMessage() {
        String message = messageEditText.getText().toString().trim();
        if (message.isEmpty()) {
            return;
        }

        // Add user message to chat
        addUserMessage(message);
        messageEditText.setText("");

        // Hide keyboard
        hideKeyboard();

        // Show typing indicator
        showTypingIndicator();

        // Call API
        searchRecipes(message);
    }

    /**
     * Hide the keyboard
     */
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(messageEditText.getWindowToken(), 0);
        }
    }

    /**
     * Add a user message to the chat
     * @param message The message to add
     */
    private void addUserMessage(String message) {
        Log.d(TAG, "Adding user message: " + message);
        ChatMessage chatMessage = new ChatMessage(ChatMessage.TYPE_USER, message);
        messageAdapter.addMessage(chatMessage);
        scrollToBottom();
        saveChatHistory();
    }

    /**
     * Add a bot message to the chat
     * @param message The message to add
     */
    private void addBotMessage(String message) {
        Log.d(TAG, "Adding bot message: " + message);
        ChatMessage chatMessage = new ChatMessage(ChatMessage.TYPE_BOT, message);
        messageAdapter.addMessage(chatMessage);
        scrollToBottom();
        saveChatHistory();
    }

    /**
     * Add a bot message with recipes to the chat
     * @param message The message to add
     * @param recipes The list of recipes to display
     */
    private void addBotMessageWithRecipes(String message, List<ModelResponse.RecipeResponse.Recipe> recipes) {
        Log.d(TAG, "Adding bot message with " + recipes.size() + " recipes: " + message);
        ChatMessage chatMessage = new ChatMessage(ChatMessage.TYPE_BOT, message, recipes);
        messageAdapter.addMessage(chatMessage);
        scrollToBottom();
        saveChatHistory();
    }

    /**
     * Scroll to the bottom of the chat
     */
    private void scrollToBottom() {
        recyclerView.post(() -> {
            int messageCount = messageAdapter.getItemCount();
            if (messageCount > 0) {
                recyclerView.smoothScrollToPosition(messageCount - 1);
            }
        });
    }

    /**
     * Show the typing indicator
     */
    private void showTypingIndicator() {
        Log.d(TAG, "Showing typing indicator");
        ChatMessage typingMessage = new ChatMessage(ChatMessage.TYPE_BOT, "Searching for recipes...");
        messageAdapter.addMessage(typingMessage);
        scrollToBottom();
    }

    /**
     * Remove the typing indicator
     */
    private void removeTypingIndicator() {
        Log.d(TAG, "Removing typing indicator");
        List<ChatMessage> messages = messageAdapter.getMessages();
        if (!messages.isEmpty()) {
            messages.remove(messages.size() - 1);
            messageAdapter.setMessages(messages);
        }
    }

    /**
     * Search for recipes using the API
     * @param prompt The search prompt
     */
    private void searchRecipes(String prompt) {
        Log.d(TAG, "Searching for recipes with prompt: " + prompt);
        showLoading(true);

        String authToken = BEARER_PREFIX + token;
        Call<ModelResponse.SearchRecipeResponse> call = apiService.searchRecipeByPrompt(authToken, prompt);

        call.enqueue(new Callback<ModelResponse.SearchRecipeResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.SearchRecipeResponse> call,
                                   @NonNull Response<ModelResponse.SearchRecipeResponse> response) {
                removeTypingIndicator();

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Recipe search successful");
                    ModelResponse.SearchRecipeResponse searchResponse = response.body();

                    if (searchResponse.isSuccess() && searchResponse.getData() != null) {
                        processSearchResults(searchResponse);
                    } else {
                        Log.e(TAG, "API returned success=false or null data");
                        addBotMessage("Sorry, I couldn't find any recipes matching your request. Please try a different search.");
                        showLoading(false);
                    }
                } else {
                    handleErrorResponse(response, "Failed to search for recipes");
                    showLoading(false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.SearchRecipeResponse> call, @NonNull Throwable t) {
                removeTypingIndicator();
                showLoading(false);
                Log.e(TAG, "API call failed", t);
                addBotMessage("Sorry, there was an error connecting to the server. Please try again later.");
            }
        });
    }

    /**
     * Process search results and fetch detailed recipe information
     * @param response The search response
     */
    private void processSearchResults(ModelResponse.SearchRecipeResponse response) {
        List<ModelResponse.SearchRecipeResponse.Recipe> searchRecipes = response.getData().getRecipes();

        if (searchRecipes == null || searchRecipes.isEmpty()) {
            Log.d(TAG, "No recipes found in response");
            addBotMessage("I didn't find any recipes matching your request. Try a different search term.");
            showLoading(false);
            return;
        }

        int recipeCount = Math.min(searchRecipes.size(), 5);
        List<String> recipeIds = new ArrayList<>(recipeCount);

        // Extract recipe IDs from search results
        for (int i = 0; i < recipeCount; i++) {
            recipeIds.add(searchRecipes.get(i).getId());
        }

        // Fetch detailed recipe information for each ID
        fetchRecipeDetails(recipeIds, response.getSearchQuery().getTitle());
    }

    /**
     * Fetch detailed recipe information for each ID
     * @param recipeIds List of recipe IDs
     * @param searchQuery The original search query
     */
    private void fetchRecipeDetails(List<String> recipeIds, String searchQuery) {
        final List<ModelResponse.RecipeResponse.Recipe> recipeDetails = new ArrayList<>();
        final AtomicInteger pendingRequests = new AtomicInteger(recipeIds.size());

        for (String recipeId : recipeIds) {
            String authToken = BEARER_PREFIX + token;
            Call<ModelResponse.RecipeDetailResponse> call = apiService.getRecipeDetail(authToken, recipeId);

            call.enqueue(new Callback<ModelResponse.RecipeDetailResponse>() {
                @Override
                public void onResponse(@NonNull Call<ModelResponse.RecipeDetailResponse> call,
                                       @NonNull Response<ModelResponse.RecipeDetailResponse> response) {
                    if (response.isSuccessful() && response.body() != null &&
                            response.body().getData() != null &&
                            response.body().getData().getRecipe() != null) {

                        // Convert RecipeDetail to Recipe for adapter
                        ModelResponse.RecipeDetailResponse.Recipe detailedRecipe =
                                response.body().getData().getRecipe();

                        // Create a RecipeResponse instance from ModelResponse to build the Recipe object
                        ModelResponse.RecipeResponse recipeResponse = modelResponse.new RecipeResponse();

                        // Create the Recipe instance from the RecipeResponse instance
                        ModelResponse.RecipeResponse.Recipe recipe = recipeResponse.new Recipe();

                        recipe.setId(detailedRecipe.getId());
                        recipe.setTitle(detailedRecipe.getTitle());
                        recipe.setAuthor(detailedRecipe.getAuthor());
                        recipe.setImageUrl(detailedRecipe.getImageUrl());
                        recipe.setIngredients(detailedRecipe.getIngredients());
                        recipe.setInstructions(detailedRecipe.getInstructions());

                        // Add to our list
                        synchronized (recipeDetails) {
                            recipeDetails.add(recipe);
                        }
                    }

                    if (pendingRequests.decrementAndGet() == 0) {
                        // All requests completed
                        showLoading(false);
                        displayRecipeResults(recipeDetails, searchQuery);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ModelResponse.RecipeDetailResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "Failed to fetch recipe details for ID: " + recipeId, t);

                    if (pendingRequests.decrementAndGet() == 0) {
                        // All requests completed
                        showLoading(false);
                        displayRecipeResults(recipeDetails, searchQuery);
                    }
                }
            });
        }
    }

    /**
     * Display recipe results in the chat
     * @param recipes List of detailed recipes
     * @param searchQuery The original search query
     */
    private void displayRecipeResults(List<ModelResponse.RecipeResponse.Recipe> recipes, String searchQuery) {
        if (recipes.isEmpty()) {
            addBotMessage("I couldn't find any recipes matching your request. Please try a different search.");
            return;
        }

        String message = String.format("Here are some %s recipes I found for you:", searchQuery);
        addBotMessageWithRecipes(message, recipes);
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
     * Show error message
     */
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        addBotMessage("Error: " + message);
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
     * Save chat history to SharedPreferences
     */
    private void saveChatHistory() {
        Log.d(TAG, "Saving chat history");
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(messageAdapter.getMessages());
        editor.putString(PREF_CHAT_HISTORY, json);
        editor.apply();
    }

    /**
     * Load chat history from SharedPreferences
     */
    private void loadChatHistory() {
        Log.d(TAG, "Loading chat history");
        String json = sharedPreferences.getString(PREF_CHAT_HISTORY, null);
        if (json != null) {
            try {
                Gson gson = new Gson();
                Type type = new TypeToken<ArrayList<ChatMessage>>() {}.getType();
                List<ChatMessage> savedMessages = gson.fromJson(json, type);
                messageAdapter.setMessages(savedMessages);
                Log.d(TAG, "Loaded " + savedMessages.size() + " messages from history");
            } catch (Exception e) {
                Log.e(TAG, "Error loading chat history", e);
                // Reset chat history if there's an error
                sharedPreferences.edit().remove(PREF_CHAT_HISTORY).apply();
            }
        }
    }

    /**
     * Clear chat history
     */
    public void clearChatHistory() {
        Log.d(TAG, "Clearing chat history");
        sharedPreferences.edit().remove(PREF_CHAT_HISTORY).apply();
        messageAdapter.setMessages(new ArrayList<>());
        addBotMessage("Hello! I'm your Food Advisor. Ask me about recipes and I'll help you find something delicious!");
    }

    /**
     * Handle recipe click
     */
    @Override
    public void onRecipeClick(String recipeId) {
        Log.d(TAG, "Recipe clicked: " + recipeId);
        Intent intent = new Intent(this, MainRecipe.class);
        intent.putExtra("recipe_id", recipeId);
        intent.putExtra("token", token);
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveChatHistory();
    }
}