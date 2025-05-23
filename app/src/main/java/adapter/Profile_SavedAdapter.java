package adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appfood.MainRecipe;
import com.example.appfood.R;
import java.util.List;
import api.ApiService;
import api.RetrofitClient;
import api.ModelResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Profile_SavedAdapter extends RecyclerView.Adapter<Profile_SavedAdapter.ViewHolder> {

    private final List<ModelResponse.RecipeResponse.Recipe> recipeList;
    private final Context context;
    private final String token;
    public Profile_SavedAdapter(Context context, List<ModelResponse.RecipeResponse.Recipe> recipeList, String token) {
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
        ModelResponse.RecipeResponse.Recipe recipe = recipeList.get(position);

        holder.tvNameRecipe.setText(recipe.getTitle());
        holder.tvTime.setText(recipe.getTime());
        holder.tvRating.setText(String.valueOf(recipe.getAverageRating()));

        fetchUsername(recipe.getAuthor(), holder.tvAuthor);

        Glide.with(holder.itemView.getContext())
                .load(recipe.getImageUrl())
                .into(holder.imgRecipe);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MainRecipe.class);
            intent.putExtra("recipe_id", recipe.getId());
            intent.putExtra("token", token);
            context.startActivity(intent);
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
}