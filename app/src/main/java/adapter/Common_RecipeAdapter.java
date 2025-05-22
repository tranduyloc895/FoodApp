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

public class Common_RecipeAdapter extends RecyclerView.Adapter<Common_RecipeAdapter.ViewHolder> {
    private static final String TAG = "Common_RecipeAdapter";
    final private Context context;
    final private List<ModelResponse.RecipeResponse.Recipe> recipeList;
    final private OnRecipeClickListener listener;
    private String token;
    private HomeFragment homeFragment; // Reference to HomeFragment

    public interface OnRecipeClickListener {
        void onRecipeClick(String recipeId);
    }

    // Basic constructor without HomeFragment reference
    public Common_RecipeAdapter(Context context, List<ModelResponse.RecipeResponse.Recipe> recipeList,
                                OnRecipeClickListener listener) {
        this.context = context;
        this.recipeList = recipeList;
        this.listener = listener;
        this.homeFragment = null; // No HomeFragment reference

        // Safely get token from shared preferences if available
        try {
            this.token = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("token", "");
        } catch (Exception e) {
            Log.e(TAG, "Error getting token: " + e.getMessage());
            this.token = "";
        }
    }

    // Constructor that explicitly takes HomeFragment reference
    public Common_RecipeAdapter(Context context, List<ModelResponse.RecipeResponse.Recipe> recipeList,
                                OnRecipeClickListener listener, HomeFragment homeFragment) {
        this.context = context;
        this.recipeList = recipeList;
        this.listener = listener;
        this.homeFragment = homeFragment;

        // Safely get token from shared preferences if available
        try {
            this.token = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("token", "");
        } catch (Exception e) {
            Log.e(TAG, "Error getting token: " + e.getMessage());
            this.token = "";
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
        holder.tvRecipeName.setText(recipe.getTitle());

        holder.tvRecipeName.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                holder.tvRecipeName.getViewTreeObserver().removeOnPreDrawListener(this);

                int maxHeightPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, context.getResources().getDisplayMetrics());
                if (holder.tvRecipeName.getHeight() > maxHeightPx) {
                    Animation marqueeAnimation = AnimationUtils.loadAnimation(context, R.anim.vertical_marquee);
                    holder.tvRecipeName.startAnimation(marqueeAnimation);
                    holder.tvRecipeName.setSelected(true);
                }
                return true;
            }
        });

        Glide.with(context).load(recipe.getImageUrl()).into(holder.ivRecipeImage);
        holder.tvAverageRating.setText(String.format("%.1f", recipe.getAverageRating()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecipeClick(recipe.getId());
            }
        });

        holder.tvTime.setText(recipe.getTime());

        // Update save button appearance based on saved state
        updateSaveButtonState(holder, recipe.getId());

        // Set click listener for save button
        holder.btnSaveRecipe.setOnClickListener(v -> saveRecipe(holder, recipe.getId()));
    }

    @Override
    public int getItemCount() {
        return recipeList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivRecipeImage;
        TextView tvRecipeName, tvAverageRating, tvTime;
        ImageButton btnSaveRecipe;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            ivRecipeImage = itemView.findViewById(R.id.iv_recipe_image_common);
            tvRecipeName = itemView.findViewById(R.id.tv_recipe_name_common);
            tvAverageRating = itemView.findViewById(R.id.tv_average_rating_common);
            tvTime = itemView.findViewById(R.id.tv_recipe_time_value_common);
            btnSaveRecipe = itemView.findViewById(R.id.btn_save_recipe_common);
        }
    }

    // Update the save button appearance based on saved state from HomeFragment
    private void updateSaveButtonState(ViewHolder holder, String recipeId) {
        boolean isSaved = false;

        // Check if we have a reference to HomeFragment
        if (homeFragment != null) {
            isSaved = homeFragment.isRecipeSaved(recipeId);
        }

        // Set appropriate icon based on saved state
        holder.btnSaveRecipe.setImageResource(
                isSaved ? R.drawable.ic_bookmark_fill : R.drawable.ic_bookmark_outline
        );
    }

    // Call API to save recipe and update HomeFragment saved recipes map
//    private void saveRecipe(ViewHolder holder, String recipeId) {
//        if (token.isEmpty()) {
//            Toast.makeText(context, "Please log in to save recipes", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // Show loading state
//        holder.btnSaveRecipe.setEnabled(false);
//
//        ApiService apiService = RetrofitClient.getApiService();
//        Call<ModelResponse.SavedRecipeResponse> call = apiService.saveRecipe("Bearer " + token, recipeId);
//
//        call.enqueue(new Callback<ModelResponse.SavedRecipeResponse>() {
//            @Override
//            public void onResponse(Call<ModelResponse.SavedRecipeResponse> call, Response<ModelResponse.SavedRecipeResponse> response) {
//                holder.btnSaveRecipe.setEnabled(true);
//
//                if (response.isSuccessful() && response.body() != null) {
//                    // Get current saved state from HomeFragment
//                    boolean currentState = false;
//                    if (homeFragment != null) {
//                        currentState = homeFragment.isRecipeSaved(recipeId);
//                    }
//
//                    // Toggle saved state
//                    boolean newState = !currentState;
//
//                    // Update state in HomeFragment
//                    if (homeFragment != null) {
//                        homeFragment.updateSavedRecipeStatus(recipeId, newState);
//                    }
//
//                    // Update UI
//                    updateSaveButtonState(holder, recipeId);
//
//                    // Show success message
//                    String message = newState ?
//                            "Recipe saved successfully" :
//                            "Recipe removed from saved collection";
//                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
//
//                    // Log the total saved recipes count from response if available
//                    if (response.body().getData() != null) {
//                        Log.d(TAG, "Total saved recipes: " + response.body().getData().getTotalSavedRecipes());
//                    }
//                } else {
//                    // Show error message
//                    Toast.makeText(context, "Failed to save recipe", Toast.LENGTH_SHORT).show();
//                    Log.e(TAG, "Error saving recipe: " + response.code());
//                }
//            }
//
//            @Override
//            public void onFailure(Call<ModelResponse.SavedRecipeResponse> call, Throwable t) {
//                holder.btnSaveRecipe.setEnabled(true);
//                Toast.makeText(context, "Network error while saving recipe", Toast.LENGTH_SHORT).show();
//                Log.e(TAG, "Network error: " + t.getMessage());
//            }
//        });
//    }

    // Toggle save/unsave recipe
    private void saveRecipe(ViewHolder holder, String recipeId) {
        if (token.isEmpty()) {
            Toast.makeText(context, "Please log in to save recipes", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        holder.btnSaveRecipe.setEnabled(false);

        ApiService apiService = RetrofitClient.getApiService();

        // Check if recipe is currently saved or not
        boolean isCurrentlySaved = false;
        if (homeFragment != null) {
            isCurrentlySaved = homeFragment.isRecipeSaved(recipeId);
        }

        // Log the current state and intended action
        Log.d(TAG, "Recipe " + recipeId + " is currently " +
                (isCurrentlySaved ? "saved" : "unsaved") +
                ". Will " + (isCurrentlySaved ? "DELETE" : "SAVE"));

        if (isCurrentlySaved) {
            // Recipe is already saved, so unsave it
            Call<ModelResponse.DeleteSavedRecipeResponse> call = apiService.deleteSavedRecipe("Bearer " + token, recipeId);

            call.enqueue(new Callback<ModelResponse.DeleteSavedRecipeResponse>() {
                @Override
                public void onResponse(Call<ModelResponse.DeleteSavedRecipeResponse> call,
                                       Response<ModelResponse.DeleteSavedRecipeResponse> response) {
                    holder.btnSaveRecipe.setEnabled(true);

                    if (response.isSuccessful()) {
                        // Update state in HomeFragment to unsaved
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
        } else {
            // Recipe is not saved, so save it
            Call<ModelResponse.SavedRecipeResponse> call = apiService.saveRecipe("Bearer " + token, recipeId);

            call.enqueue(new Callback<ModelResponse.SavedRecipeResponse>() {
                @Override
                public void onResponse(Call<ModelResponse.SavedRecipeResponse> call,
                                       Response<ModelResponse.SavedRecipeResponse> response) {
                    holder.btnSaveRecipe.setEnabled(true);

                    if (response.isSuccessful() && response.body() != null) {
                        // Update state in HomeFragment to saved
                        if (homeFragment != null) {
                            homeFragment.updateSavedRecipeStatus(recipeId, true);
                        }

                        // Update UI
                        updateSaveButtonState(holder, recipeId);

                        // Show success message
                        Toast.makeText(context, "Recipe saved successfully", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Successfully saved recipe: " + recipeId);

                        // Log the total saved recipes count from response if available
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
    }
}