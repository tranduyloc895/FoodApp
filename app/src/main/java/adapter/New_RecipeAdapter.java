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
import com.bumptech.glide.request.RequestOptions;
import com.example.appfood.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import de.hdodenhof.circleimageview.CircleImageView;
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
    private final Map<String, String> authorAvatarCache = new HashMap<>();
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

                // Set helen's avatar for special author
                if (holder.ivAuthorAvatar != null) {
                    holder.ivAuthorAvatar.setImageResource(R.drawable.ic_helen);
                }
            } else {
                // Check name cache first
                if (authorNameCache.containsKey(author)) {
                    holder.tvRecipeAuthor.setText(authorNameCache.get(author));

                    // Check avatar cache and set avatar if available
                    if (holder.ivAuthorAvatar != null && authorAvatarCache.containsKey(author)) {
                        loadAuthorAvatar(holder.ivAuthorAvatar, authorAvatarCache.get(author));
                    }
                } else {
                    // Set temporary text
                    holder.tvRecipeAuthor.setText("Loading author...");

                    // Set default avatar while loading
                    if (holder.ivAuthorAvatar != null) {
                        holder.ivAuthorAvatar.setImageResource(R.drawable.ic_profile);
                    }

                    // Fetch author info (name and avatar)
                    fetchAuthorInfo(author, holder);
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

        holder.tvTime.setText(recipe.getTime());
    }

    /**
     * Fetch the author information (name and avatar) based on author ID
     */
    private void fetchAuthorInfo(String authorId, ViewHolder holder) {
        if (token == null || token.isEmpty() || authorId == null || authorId.isEmpty()) {
            holder.tvRecipeAuthor.setText("Unknown Author");
            if (holder.ivAuthorAvatar != null) {
                holder.ivAuthorAvatar.setImageResource(R.drawable.ic_profile);
            }
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

                    ModelResponse.UserResponse.User user = response.body().getData().getUser();
                    String name = user.getName();
                    String avatarUrl = user.getUrlAvatar();

                    // Cache the results
                    authorNameCache.put(authorId, name);
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        authorAvatarCache.put(authorId, avatarUrl);
                    }

                    // Update name
                    holder.tvRecipeAuthor.setText(name);

                    // Update avatar if view exists
                    if (holder.ivAuthorAvatar != null) {
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            loadAuthorAvatar(holder.ivAuthorAvatar, avatarUrl);
                        } else {
                            holder.ivAuthorAvatar.setImageResource(R.drawable.ic_profile);
                        }
                    }
                } else {
                    holder.tvRecipeAuthor.setText("Unknown Author");
                    if (holder.ivAuthorAvatar != null) {
                        holder.ivAuthorAvatar.setImageResource(R.drawable.ic_profile);
                    }
                    Log.e(TAG, "Error getting author info: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.UserResponse> call, Throwable t) {
                holder.tvRecipeAuthor.setText("Unknown Author");
                if (holder.ivAuthorAvatar != null) {
                    holder.ivAuthorAvatar.setImageResource(R.drawable.ic_profile);
                }
                Log.e(TAG, "Failed to get author info: " + t.getMessage());
            }
        });
    }

    /**
     * Load author avatar with Glide
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivRecipeImage;
        ImageView ivAuthorAvatar; // Add author avatar view reference
        TextView tvRecipeName, tvRecipeAuthor, tvTime;
        RatingBar ratingBar_new;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            ivRecipeImage = itemView.findViewById(R.id.iv_recipe_image_latest);
            tvRecipeName = itemView.findViewById(R.id.tv_recipe_name_latest);
            tvRecipeAuthor = itemView.findViewById(R.id.tv_recipe_author_name_latest);
            ratingBar_new = itemView.findViewById(R.id.ratingBar_new);
            tvTime = itemView.findViewById(R.id.tv_recipe_time_latest);

            // Get reference to author avatar image view
            // (if this ID doesn't exist in your layout, you'll need to add it)
            ivAuthorAvatar = itemView.findViewById(R.id.iv_recipe_author_image_latest);
        }
    }
}