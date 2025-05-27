package com.example.appfood;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import api.ModelResponse;

public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_BOT = 1;

    private int type;
    private String message;
    private long timestamp;
    private List<ModelResponse.RecipeResponse.Recipe> recipes;

    public ChatMessage(int type, String message) {
        this.type = type;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public ChatMessage(int type, String message, List<ModelResponse.RecipeResponse.Recipe> recipes) {
        this.type = type;
        this.message = message;
        this.recipes = recipes;
        this.timestamp = System.currentTimeMillis();
    }

    public int getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public List<ModelResponse.RecipeResponse.Recipe> getRecipes() {
        return recipes;
    }

    public void setRecipes(List<ModelResponse.RecipeResponse.Recipe> recipes) {
        this.recipes = recipes;
    }

    public boolean hasRecipes() {
        return recipes != null && !recipes.isEmpty();
    }
}