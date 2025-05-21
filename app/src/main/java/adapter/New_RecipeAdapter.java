package adapter;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.appfood.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class New_RecipeAdapter extends RecyclerView.Adapter<New_RecipeAdapter.ViewHolder> {
    private static final String TAG = "New_RecipeAdapter";
    private static final String SPECIAL_AUTHOR = "helenrecipes";

    final private Context context;
    final private List<ModelResponse.RecipeResponse.Recipe> recipeList;
    final private OnRecipeClickListener listener;
    private final Map<String, String> authorNameCache = new HashMap<>();
    private String token;

    public interface OnRecipeClickListener {
        void onRecipeClick(String recipeId);
    }

    // Original constructor
    public New_RecipeAdapter(Context context, List<ModelResponse.RecipeResponse.Recipe> recipeList, OnRecipeClickListener listener) {
        this.context = context;
        this.recipeList = recipeList;
        this.listener = listener;

        // Safely get token from shared preferences as fallback
        try {
            this.token = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("token", "");
        } catch (Exception e) {
            Log.e(TAG, "Error getting token: " + e.getMessage());
            this.token = "";
        }
    }

    // New overloaded constructor that accepts token parameter
    public New_RecipeAdapter(Context context, List<ModelResponse.RecipeResponse.Recipe> recipeList,
                             OnRecipeClickListener listener, String token) {
        this.context = context;
        this.recipeList = recipeList;
        this.listener = listener;
        this.token = token != null ? token : "";
    }

    // Set token method for updating the token later if needed
    public void setToken(String token) {
        this.token = token;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_latest_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ModelResponse.RecipeResponse.Recipe recipe = recipeList.get(position);
        holder.tvRecipeName.setText(recipe.getTitle());

        // Handle author display - verify tvRecipeAuthor is not null
        if (holder.tvRecipeAuthor != null) {
            String author = recipe.getAuthor();
            if (SPECIAL_AUTHOR.equals(author)) {
                // Special case for helenrecipes
                holder.tvRecipeAuthor.setText(author);
            } else {
                // Check cache first
                if (authorNameCache.containsKey(author)) {
                    holder.tvRecipeAuthor.setText(authorNameCache.get(author));
                } else {
                    // Set temporary text
                    holder.tvRecipeAuthor.setText("Loading author...");
                    // Fetch author name by ID
                    fetchAuthorName(author, holder.tvRecipeAuthor);
                }
            }
        }

        ObjectAnimator animator = ObjectAnimator.ofFloat(holder.tvRecipeName, "translationX", 0f, 0f);
        animator.setDuration(5000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.start();

        holder.tvRecipeName.setSelected(true);

        Glide.with(context).load(recipe.getImageUrl()).into(holder.ivRecipeImage);
        holder.ratingBar_new.setRating((float) recipe.getAverageRating());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecipeClick(recipe.getId());
            }
        });
    }

    /**
     * Fetch the author name based on author ID
     */
    private void fetchAuthorName(String authorId, TextView authorTextView) {
        if (token == null || token.isEmpty() || authorId == null || authorId.isEmpty()) {
            authorTextView.setText("Unknown Author");
            return;
        }

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.UserResponse> call = apiService.getUserById("Bearer " + token, authorId);

        call.enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.UserResponse> call, Response<ModelResponse.UserResponse> response) {
                if (response.isSuccessful() && response.body() != null &&
                        response.body().getData() != null &&
                        response.body().getData().getUser() != null) {

                    String name = response.body().getData().getUser().getName();
                    // Cache the result
                    authorNameCache.put(authorId, name);
                    // Update UI
                    authorTextView.setText(name);
                } else {
                    authorTextView.setText("Unknown Author");
                    Log.e(TAG, "Error getting author name: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.UserResponse> call, Throwable t) {
                authorTextView.setText("Unknown Author");
                Log.e(TAG, "Failed to get author name: " + t.getMessage());
            }
        });
    }

    @Override
    public int getItemCount() {
        return recipeList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivRecipeImage;
        TextView tvRecipeName, tvRecipeAuthor;
        RatingBar ratingBar_new;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            ivRecipeImage = itemView.findViewById(R.id.iv_recipe_image_latest);
            tvRecipeName = itemView.findViewById(R.id.tv_recipe_name_latest);
            tvRecipeAuthor = itemView.findViewById(R.id.tv_recipe_author_name_latest);
            ratingBar_new = itemView.findViewById(R.id.ratingBar_new);
        }
    }
}