package adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appfood.MainRecipe;
import com.example.appfood.R;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import api.ApiService;
import api.RetrofitClient;
import api.ModelResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OtherProfile_UploadedAdapter extends RecyclerView.Adapter<OtherProfile_UploadedAdapter.ViewHolder> {

    private static final String TAG = "OtherProfile_UploadedAdapter";
    private final List<ModelResponse.RecipeDetailResponse.Recipe> recipeList;
    private final Context context;
    private final String token;
    private OnItemClickListener onItemClickListener;
    private AtomicInteger loadingCounter = new AtomicInteger(0);
    private double averageRating;
    private FrameLayout loadingOverlay;

    public OtherProfile_UploadedAdapter(Context context, List<ModelResponse.RecipeDetailResponse.Recipe> recipeList, String token) {
        this.context = context;
        this.recipeList = recipeList;
        this.token = token;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_profile_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ModelResponse.RecipeDetailResponse.Recipe recipe = recipeList.get(position);

        Log.d(TAG, "Binding recipe to UI: " + recipe.getTitle());
        holder.tvNameRecipe.setText(recipe.getTitle());
        holder.tvTime.setText(recipe.getTime());
        loadingOverlay = holder.itemView.findViewById(R.id.loadingOverlay);

        fetchRecipeRating(token, recipe.getId(), holder);
        fetchUsername(recipe.getAuthor(), holder.tvAuthor);

        Glide.with(holder.itemView.getContext())
                .load(recipe.getImageUrl())
                .into(holder.imgRecipe);

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(holder.getAdapterPosition());
            } else {
                Intent intent = new Intent(context, MainRecipe.class);
                intent.putExtra("recipe_id", recipe.getId());
                intent.putExtra("token", token);
                context.startActivity(intent);
            }
        });
    }

    private void fetchUsername(String userId, TextView tvAuthor) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.UserResponse> call = apiService.getUserById("Bearer " + token, userId);

        call.enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Response<ModelResponse.UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String username = response.body().getData().getUser().getName();
                    tvAuthor.setText("by " + username);
                } else {
                    tvAuthor.setText("by Unknown");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Throwable t) {
                tvAuthor.setText("by Unknown");
            }
        });
    }

    private void fetchRecipeRating(String token, String id, ViewHolder holder) {
        // Increment loading counter
        loadingCounter.incrementAndGet();

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.getRatingResponse> call = apiService.getRecipeRating("Bearer " + token, id);

        call.enqueue(new Callback<ModelResponse.getRatingResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.getRatingResponse> call,
                                   @NonNull Response<ModelResponse.getRatingResponse> response) {
                // Decrement loading counter
                checkAndUpdateLoadingState();

                if (response.isSuccessful() && response.body() != null &&
                        response.body().getData() != null) {

                    ModelResponse.getRatingResponse.Data ratingData = response.body().getData();

                    // Cập nhật rating
                    averageRating = ratingData.getAverageRating();
                    Log.d(TAG, "Received rating data: average=" + averageRating);

                    // Truyền holder vào UI update
                    updateRatingUI(holder);
                } else {
                    Log.e(TAG, "Failed to get rating info: " +
                            (response.code() + " " + (response.errorBody() != null ?
                                    "Error body available" : "No error body")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.getRatingResponse> call, @NonNull Throwable t) {
                // Decrement loading counter
                checkAndUpdateLoadingState();
                Log.e(TAG, "Network error fetching rating: " + t.getMessage());
            }
        });
    }

    private void updateRatingUI(ViewHolder holder) {
        if (holder != null) {
            ((Activity) context).runOnUiThread(() -> {
                holder.tvRating.setText(String.format("%.1f", averageRating));
            });
        }
    }

    private void checkAndUpdateLoadingState() {
        int count = loadingCounter.decrementAndGet();
        Log.d(TAG, "Remaining API calls: " + count);

        if (count <= 0) {
            showLoading(false);
        }
    }

    private void showLoading(boolean isLoading) {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> {
                if (loadingOverlay != null) {
                    loadingOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            Log.e(TAG, "Context is not an Activity. UI update failed.");
        }
    }

    @Override
    public int getItemCount() {
        return recipeList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNameRecipe, tvAuthor, tvTime, tvRating;
        ImageView imgRecipe;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNameRecipe = itemView.findViewById(R.id.tvNameRecipe);
            tvNameRecipe.setSelected(true);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvRating = itemView.findViewById(R.id.tvRating);
            imgRecipe = itemView.findViewById(R.id.savedRecipesRecyclerView);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
}