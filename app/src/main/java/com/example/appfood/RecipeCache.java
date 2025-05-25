package com.example.appfood;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import api.ModelResponse.RecipeResponse.Recipe;

public class RecipeCache {
    private static final String TAG = "RecipeCache";
    private static final String PREF_NAME = "recipe_cache_pref";
    private static final String CACHE_TIMESTAMP_KEY = "recipe_cache_timestamp";
    private static final String RECIPE_CACHE_FILENAME = "recipe_cache.json";
    private static final int CACHE_EXPIRY_HOURS = 24; // Cache expires after 24 hours

    public static void saveRecipesToCache(Context context, List<Recipe> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            Log.d(TAG, "No recipes to cache");
            return;
        }

        try {
            // Save recipes to a file
            Gson gson = new Gson();
            String jsonRecipes = gson.toJson(recipes);

            File cacheFile = new File(context.getFilesDir(), RECIPE_CACHE_FILENAME);
            FileOutputStream outputStream = new FileOutputStream(cacheFile);
            outputStream.write(jsonRecipes.getBytes());
            outputStream.close();

            // Save timestamp to preferences
            SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            preferences.edit()
                    .putLong(CACHE_TIMESTAMP_KEY, System.currentTimeMillis())
                    .apply();

            Log.d(TAG, "Cached " + recipes.size() + " recipes successfully");
        } catch (IOException e) {
            Log.e(TAG, "Error saving recipes to cache: " + e.getMessage());
        }
    }

    public static List<Recipe> getRecipesFromCache(Context context) {
        try {
            // Check if cache is valid
            SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            long timestamp = preferences.getLong(CACHE_TIMESTAMP_KEY, 0);

            // Check if cache is expired
            if (isCacheExpired(timestamp)) {
                Log.d(TAG, "Cache expired");
                return null;
            }

            // Read recipes from file
            File cacheFile = new File(context.getFilesDir(), RECIPE_CACHE_FILENAME);
            if (!cacheFile.exists()) {
                return null;
            }

            FileReader reader = new FileReader(cacheFile);
            Gson gson = new Gson();
            Type recipeListType = new TypeToken<ArrayList<Recipe>>(){}.getType();
            List<Recipe> recipes = gson.fromJson(reader, recipeListType);
            reader.close();

            Log.d(TAG, "Retrieved " + (recipes != null ? recipes.size() : 0) + " recipes from cache");
            return recipes;
        } catch (IOException e) {
            Log.e(TAG, "Error reading recipes from cache: " + e.getMessage());
            return null;
        }
    }

    public static List<Recipe> searchRecipesByTitle(Context context, String query) {
        List<Recipe> allRecipes = getRecipesFromCache(context);
        if (allRecipes == null || allRecipes.isEmpty() || query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String normalizedQuery = query.toLowerCase(Locale.getDefault()).trim();

        // Filter recipes that contain the query in their title
        return allRecipes.stream()
                .filter(recipe -> recipe.getTitle() != null &&
                        recipe.getTitle().toLowerCase(Locale.getDefault()).contains(normalizedQuery))
                .collect(Collectors.toList());
    }

    public static boolean isCacheAvailable(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long timestamp = preferences.getLong(CACHE_TIMESTAMP_KEY, 0);

        if (isCacheExpired(timestamp)) {
            return false;
        }

        File cacheFile = new File(context.getFilesDir(), RECIPE_CACHE_FILENAME);
        return cacheFile.exists();
    }

    public static void clearCache(Context context) {
        File cacheFile = new File(context.getFilesDir(), RECIPE_CACHE_FILENAME);
        if (cacheFile.exists()) {
            cacheFile.delete();
        }

        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        preferences.edit().remove(CACHE_TIMESTAMP_KEY).apply();

        Log.d(TAG, "Recipe cache cleared");
    }

    private static boolean isCacheExpired(long timestamp) {
        if (timestamp == 0) {
            return true;
        }

        long currentTime = System.currentTimeMillis();
        long expiryTimeMs = CACHE_EXPIRY_HOURS * 60 * 60 * 1000; // Convert hours to milliseconds

        return currentTime - timestamp > expiryTimeMs;
    }
}