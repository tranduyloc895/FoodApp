package adapter;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
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
import com.bumptech.glide.request.RequestOptions;
import com.example.appfood.R;
import com.example.appfood.OtherProfileActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Adapter for displaying new recipes in a RecyclerView with additional author information
 */
public class NewRecipeAdapter extends RecyclerView.Adapter<NewRecipeAdapter.ViewHolder> {
    private static final String TAG = "NewRecipeAdapter";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String SPECIAL_AUTHOR = "helenrecipes";

    private final Context context;
    private final List<ModelResponse.RecipeResponse.Recipe> recipeList;
    private final OnRecipeClickListener listener;
    private final Map<String, String> authorNameCache = new HashMap<>();
    private final Map<String, String> authorAvatarCache = new HashMap<>();
    private String token;

    /**
     * Interface for recipe click actions
     */
    public interface OnRecipeClickListener {
        void onRecipeClick(String recipeId);
    }

    /**
     * Constructor without explicit token
     */
    public NewRecipeAdapter(Context context, List<ModelResponse.RecipeResponse.Recipe> recipeList,
                            OnRecipeClickListener listener) {
        this.context = context;
        this.recipeList = recipeList;
        this.listener = listener;
        this.token = getTokenFromPreferences(context);
    }

    /**
     * Constructor with explicit token
     */
    public NewRecipeAdapter(Context context, List<ModelResponse.RecipeResponse.Recipe> recipeList,
                            OnRecipeClickListener listener, String token) {
        this.context = context;
        this.recipeList = recipeList;
        this.listener = listener;
        this.token = token != null ? token : "";
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

    /**
     * Updates the token
     */
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

        bindRecipeBasics(holder, recipe);
        bindAuthorInfo(holder, recipe);
        setupClickListeners(holder, recipe);
    }

    /**
     * Binds basic recipe information (title, image, rating, time)
     */
    private void bindRecipeBasics(ViewHolder holder, ModelResponse.RecipeResponse.Recipe recipe) {
        // Set recipe name with animation
        holder.tvRecipeName.setText(recipe.getTitle());
        setupTextAnimation(holder);

        // Load recipe image
        Glide.with(context)
                .load(recipe.getImageUrl())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(holder.ivRecipeImage);

        // Set rating and time
        holder.ratingBar.setRating((float) recipe.getAverageRating());
        holder.tvTime.setText(recipe.getTime());
    }

    /**
     * Sets up text animation for recipe title
     */
    private void setupTextAnimation(ViewHolder holder) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(
                holder.tvRecipeName, "translationX", 0f, 0f);
        animator.setDuration(5000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.start();
        holder.tvRecipeName.setSelected(true);
    }

    /**
     * Binds author information (name and avatar)
     */
    private void bindAuthorInfo(ViewHolder holder, ModelResponse.RecipeResponse.Recipe recipe) {
        if (holder.tvRecipeAuthor == null) return;

        String authorId = recipe.getAuthor();

        if (SPECIAL_AUTHOR.equals(authorId)) {
            // Special case for helenrecipes
            holder.tvRecipeAuthor.setText(authorId);
            if (holder.ivAuthorAvatar != null) {
                holder.ivAuthorAvatar.setImageResource(R.drawable.ic_helen);
            }
        } else if (authorNameCache.containsKey(authorId)) {
            // Use cached author info
            holder.tvRecipeAuthor.setText(authorNameCache.get(authorId));
            if (holder.ivAuthorAvatar != null && authorAvatarCache.containsKey(authorId)) {
                loadAuthorAvatar(holder.ivAuthorAvatar, authorAvatarCache.get(authorId));
            }
        } else {
            // Set temporary values and fetch author info
            holder.tvRecipeAuthor.setText("Loading author...");
            if (holder.ivAuthorAvatar != null) {
                holder.ivAuthorAvatar.setImageResource(R.drawable.ic_profile);
            }
            fetchAuthorInfo(authorId, holder);
        }
    }

    /**
     * Sets up click listeners for recipe item
     */
    private void setupClickListeners(ViewHolder holder, ModelResponse.RecipeResponse.Recipe recipe) {
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecipeClick(recipe.getId());
            }
        });

        holder.ivAuthorAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(context, OtherProfileActivity.class);
            intent.putExtra("author_id", recipe.getAuthor());
            intent.putExtra("token", token);
            context.startActivity(intent);
        });
    }

    /**
     * Fetches author information from API
     */
    private void fetchAuthorInfo(String authorId, ViewHolder holder) {
        // Skip API call if missing data
        if (token == null || token.isEmpty() || authorId == null || authorId.isEmpty()) {
            setDefaultAuthorInfo(holder);
            return;
        }

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.UserResponse> call = apiService.getUserById(BEARER_PREFIX + token, authorId);

        call.enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.UserResponse> call,
                                   Response<ModelResponse.UserResponse> response) {
                if (isSuccessfulUserResponse(response)) {
                    // Extract user info
                    ModelResponse.UserResponse.User user = response.body().getData().getUser();
                    String name = user.getName();
                    String avatarUrl = user.getUrlAvatar();

                    // Cache the data
                    authorNameCache.put(authorId, name);
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        authorAvatarCache.put(authorId, avatarUrl);
                    }

                    // Update UI
                    updateAuthorUI(holder, name, avatarUrl);
                } else {
                    // Handle error
                    setDefaultAuthorInfo(holder);
                    Log.e(TAG, "Error getting author info: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.UserResponse> call, Throwable t) {
                setDefaultAuthorInfo(holder);
                Log.e(TAG, "Failed to get author info: " + t.getMessage());
            }
        });
    }

    /**
     * Checks if user response is valid and contains required data
     */
    private boolean isSuccessfulUserResponse(Response<ModelResponse.UserResponse> response) {
        return response.isSuccessful() &&
                response.body() != null &&
                response.body().getData() != null &&
                response.body().getData().getUser() != null;
    }

    /**
     * Sets default author information when API call fails
     */
    private void setDefaultAuthorInfo(ViewHolder holder) {
        holder.tvRecipeAuthor.setText("Unknown Author");
        if (holder.ivAuthorAvatar != null) {
            holder.ivAuthorAvatar.setImageResource(R.drawable.ic_profile);
        }
    }

    /**
     * Updates author UI with fetched information
     */
    private void updateAuthorUI(ViewHolder holder, String name, String avatarUrl) {
        holder.tvRecipeAuthor.setText(name);
        if (holder.ivAuthorAvatar != null) {
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                loadAuthorAvatar(holder.ivAuthorAvatar, avatarUrl);
            } else {
                holder.ivAuthorAvatar.setImageResource(R.drawable.ic_profile);
            }
        }
    }

    /**
     * Loads author avatar using Glide
     */
    private void loadAuthorAvatar(ImageView imageView, String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_profile);
            return;
        }

        Glide.with(context)
                .load(avatarUrl)
                .apply(new RequestOptions()
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop())
                .into(imageView);
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
        final ImageView ivAuthorAvatar;
        final TextView tvRecipeName, tvRecipeAuthor, tvTime;
        final RatingBar ratingBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            ivRecipeImage = itemView.findViewById(R.id.iv_recipe_image_latest);
            tvRecipeName = itemView.findViewById(R.id.tv_recipe_name_latest);
            tvRecipeName.setSelected(true);
            tvRecipeAuthor = itemView.findViewById(R.id.tv_recipe_author_name_latest);
            ratingBar = itemView.findViewById(R.id.ratingBar_new);
            tvTime = itemView.findViewById(R.id.tv_recipe_time_latest);
            ivAuthorAvatar = itemView.findViewById(R.id.iv_recipe_author_image_latest);
        }
    }
}