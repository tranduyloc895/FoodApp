package adapter;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.appfood.R;

import java.util.List;

import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import fragment.HomeFragment;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Adapter for displaying common recipes in a RecyclerView
 */
public class CommonRecipeAdapter extends RecyclerView.Adapter<CommonRecipeAdapter.ViewHolder> {
    private static final String TAG = "CommonRecipeAdapter";
    private static final String BEARER_PREFIX = "Bearer ";

    private final Context context;
    private final List<ModelResponse.RecipeResponse.Recipe> recipeList;
    private final OnRecipeClickListener listener;
    private final HomeFragment homeFragment; // Reference to HomeFragment
    private final String token;

    /**
     * Interface for recipe click actions
     */
    public interface OnRecipeClickListener {
        void onRecipeClick(String recipeId);
    }

    /**
     * Constructor with HomeFragment reference
     */
    public CommonRecipeAdapter(Context context, List<ModelResponse.RecipeResponse.Recipe> recipeList,
                               OnRecipeClickListener listener, HomeFragment homeFragment) {
        this.context = context;
        this.recipeList = recipeList;
        this.listener = listener;
        this.homeFragment = homeFragment;

        // Get token from shared preferences
        this.token = getTokenFromPreferences(context);
    }

    /**
     * Constructor without HomeFragment reference
     */
    public CommonRecipeAdapter(Context context, List<ModelResponse.RecipeResponse.Recipe> recipeList,
                               OnRecipeClickListener listener) {
        this(context, recipeList, listener, null);
    }

    /**
     * Retrieves token from shared preferences
     */
    private String getTokenFromPreferences(Context context) {
        try {
            return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .getString("token", "");
        } catch (Exception e) {
            Log.e(TAG, "Error getting token: " + e.getMessage());
            return "";
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_common_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ModelResponse.RecipeResponse.Recipe recipe = recipeList.get(position);

        bindRecipeName(holder, recipe);
        bindRecipeImage(holder, recipe);
        bindRatingAndTime(holder, recipe);
        setupClickListeners(holder, recipe);
    }

    /**
     * Binds recipe name and sets up text animation if needed
     */
    private void bindRecipeName(ViewHolder holder, ModelResponse.RecipeResponse.Recipe recipe) {
        holder.tvRecipeName.setText(recipe.getTitle());

        holder.tvRecipeName.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                holder.tvRecipeName.getViewTreeObserver().removeOnPreDrawListener(this);

                int maxHeightPx = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        100,
                        context.getResources().getDisplayMetrics()
                );

                if (holder.tvRecipeName.getHeight() > maxHeightPx) {
                    Animation marqueeAnimation = AnimationUtils.loadAnimation(context, R.anim.vertical_marquee);
                    holder.tvRecipeName.startAnimation(marqueeAnimation);
                    holder.tvRecipeName.setSelected(true);
                }
                return true;
            }
        });
    }

    /**
     * Binds recipe image using Glide
     */
    private void bindRecipeImage(ViewHolder holder, ModelResponse.RecipeResponse.Recipe recipe) {
        Glide.with(context)
                .load(recipe.getImageUrl())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(holder.ivRecipeImage);
    }

    /**
     * Binds rating and time information
     */
    private void bindRatingAndTime(ViewHolder holder, ModelResponse.RecipeResponse.Recipe recipe) {
        // Log the value for debugging
        Log.d(TAG, "Recipe: " + recipe.getTitle() + " - Average Rating: " + recipe.getAverageRating());

        // Format the average rating
        if (recipe.getAverageRating() > 0) {
            // Format it with one decimal place
            holder.tvAverageRating.setText(String.format("%.1f", (float)recipe.getAverageRating()));
        } else {
            // Show "0.0" or "N/A" for recipes with no ratings
            holder.tvAverageRating.setText("0.0");
        }

        holder.tvTime.setText(recipe.getTime());
    }

    /**
     * Sets up click listeners for recipe item and save button
     */
    private void setupClickListeners(ViewHolder holder, ModelResponse.RecipeResponse.Recipe recipe) {
        // Recipe item click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecipeClick(recipe.getId());
            }
        });

        // Update save button appearance
        updateSaveButtonState(holder, recipe.getId());

        // Save button click
        holder.btnSaveRecipe.setOnClickListener(v -> toggleSavedState(holder, recipe.getId()));
    }

    /**
     * Updates save button appearance based on saved state
     */
    private void updateSaveButtonState(ViewHolder holder, String recipeId) {
        boolean isSaved = homeFragment != null && homeFragment.isRecipeSaved(recipeId);
        holder.btnSaveRecipe.setImageResource(
                isSaved ? R.drawable.ic_bookmark_fill : R.drawable.ic_bookmark_outline
        );
    }

    /**
     * Toggles saved/unsaved state for a recipe
     */
    private void toggleSavedState(ViewHolder holder, String recipeId) {
        if (token.isEmpty()) {
            Toast.makeText(context, "Please log in to save recipes", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button during API call
        holder.btnSaveRecipe.setEnabled(false);

        // Check if recipe is currently saved
        boolean isCurrentlySaved = homeFragment != null && homeFragment.isRecipeSaved(recipeId);

        if (isCurrentlySaved) {
            unsaveRecipe(holder, recipeId);
        } else {
            saveRecipe(holder, recipeId);
        }
    }

    /**
     * Calls API to save recipe
     */
    private void saveRecipe(ViewHolder holder, String recipeId) {
        Log.d(TAG, "Saving recipe: " + recipeId);

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.SavedRecipeResponse> call = apiService.saveRecipe(BEARER_PREFIX + token, recipeId);

        call.enqueue(new Callback<ModelResponse.SavedRecipeResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.SavedRecipeResponse> call,
                                   Response<ModelResponse.SavedRecipeResponse> response) {
                holder.btnSaveRecipe.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    // Update state in HomeFragment
                    if (homeFragment != null) {
                        homeFragment.updateSavedRecipeStatus(recipeId, true);
                    }

                    // Update UI
                    updateSaveButtonState(holder, recipeId);

                    // Show success message
                    Toast.makeText(context, "Recipe saved successfully", Toast.LENGTH_SHORT).show();

                    // Log success
                    if (response.body().getData() != null) {
                        Log.d(TAG, "Total saved recipes: " + response.body().getData().getTotalSavedRecipes());
                    }
                } else {
                    // Show error message
                    Toast.makeText(context, "Failed to save recipe", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error saving recipe: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.SavedRecipeResponse> call, Throwable t) {
                holder.btnSaveRecipe.setEnabled(true);
                Toast.makeText(context, "Network error while saving recipe", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Network error saving: " + t.getMessage());
            }
        });
    }

    /**
     * Calls API to unsave recipe
     */
    private void unsaveRecipe(ViewHolder holder, String recipeId) {
        Log.d(TAG, "Unsaving recipe: " + recipeId);

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.DeleteSavedRecipeResponse> call =
                apiService.deleteSavedRecipe(BEARER_PREFIX + token, recipeId);

        call.enqueue(new Callback<ModelResponse.DeleteSavedRecipeResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.DeleteSavedRecipeResponse> call,
                                   Response<ModelResponse.DeleteSavedRecipeResponse> response) {
                holder.btnSaveRecipe.setEnabled(true);

                if (response.isSuccessful()) {
                    // Update state in HomeFragment
                    if (homeFragment != null) {
                        homeFragment.updateSavedRecipeStatus(recipeId, false);
                    }

                    // Update UI
                    updateSaveButtonState(holder, recipeId);

                    // Show success message
                    Toast.makeText(context, "Recipe removed from saved collection", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Successfully unsaved recipe: " + recipeId);
                } else {
                    // Show error message
                    Toast.makeText(context, "Failed to remove recipe from collection", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error unsaving recipe: " + response.code() + " " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.DeleteSavedRecipeResponse> call, Throwable t) {
                holder.btnSaveRecipe.setEnabled(true);
                Toast.makeText(context, "Network error while updating recipe", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Network error unsaving: " + t.getMessage());
            }
        });
    }

    @Override
    public int getItemCount() {
        return recipeList.size();
    }

    /**
     * ViewHolder class for recipe items
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivRecipeImage;
        final TextView tvRecipeName, tvAverageRating, tvTime;
        final ImageButton btnSaveRecipe;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            ivRecipeImage = itemView.findViewById(R.id.iv_recipe_image_common);
            tvRecipeName = itemView.findViewById(R.id.tv_recipe_name_common);
            tvAverageRating = itemView.findViewById(R.id.tv_average_rating_common);
            tvTime = itemView.findViewById(R.id.tv_recipe_time_value_common);
            btnSaveRecipe = itemView.findViewById(R.id.btn_save_recipe_common);
            tvTime.setSelected(true);
        }
    }
}