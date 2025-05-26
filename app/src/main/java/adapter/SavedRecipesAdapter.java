package adapter;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.appfood.R;
import api.ModelResponse;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class SavedRecipesAdapter extends RecyclerView.Adapter<SavedRecipesAdapter.SavedRecipeViewHolder> {
    private static final String TAG = "SavedRecipesAdapter";

    private Context context;
    private List<ModelResponse.RecipeResponse.Recipe> savedRecipeList;
    private String token;
    private OnRecipeClickListener listener;

    public interface OnRecipeClickListener {
        void onRecipeClick(ModelResponse.RecipeResponse.Recipe recipe, int position);
        void onSaveButtonClick(ModelResponse.RecipeResponse.Recipe recipe, int position);
    }

    public void setOnRecipeClickListener(OnRecipeClickListener listener) {
        this.listener = listener;
    }

    public SavedRecipesAdapter(Context context, List<ModelResponse.RecipeResponse.Recipe> savedRecipeList) {
        this.context = context;
        this.savedRecipeList = savedRecipeList;
    }

    @NonNull
    @Override
    public SavedRecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_saved_recipe, parent, false);
        return new SavedRecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SavedRecipeViewHolder holder, int position) {
        ModelResponse.RecipeResponse.Recipe recipe = savedRecipeList.get(position);

        // Set recipe name
        if (recipe.getTitle() != null) {
            holder.recipeName.setText(recipe.getTitle());
        }

        // Set author
        if (recipe.getAuthor() != null ) {
            holder.recipeAuthor.setText("by " + recipe.getAuthor());
        }

        // Set cooking time
        if (recipe.getTime() != null) {
            holder.recipeTime.setText(recipe.getTime());
        }

        // Log rating for debugging purposes
        Log.d(TAG, "Recipe: " + recipe.getTitle() + " - Rating: " + recipe.getAverageRating());

        // Calculate and set rating with one decimal place
        double avgRating = recipe.getAverageRating();
        holder.recipeRating.setText(String.format("%.1f", avgRating));

        // Load image using Glide
        if (recipe.getImageUrl() != null && !TextUtils.isEmpty(recipe.getImageUrl())) {
            Glide.with(context)
                    .load(recipe.getImageUrl())
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.error_image))
                    .into(holder.recipeImage);
        }

        // Set the bookmark icon to filled since these are saved recipes
        holder.saveButton.setImageResource(R.drawable.ic_bookmark_fill);

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecipeClick(recipe, holder.getAdapterPosition());
            }
        });

        holder.saveButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSaveButtonClick(recipe, holder.getAdapterPosition());
            }
        });
    }

    /**
     * Set the authentication token for API calls
     * @param token Authentication token
     */
    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public int getItemCount() {
        return savedRecipeList.size();
    }

    public void removeRecipe(int position) {
        if (position >= 0 && position < savedRecipeList.size()) {
            savedRecipeList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, savedRecipeList.size());
        }
    }

    public void updateData(List<ModelResponse.RecipeResponse.Recipe> newRecipes) {
        this.savedRecipeList = newRecipes;
        notifyDataSetChanged();
    }

    public static class SavedRecipeViewHolder extends RecyclerView.ViewHolder {
        ImageView recipeImage;
        TextView recipeName, recipeAuthor, recipeTime, recipeRating;
        public ImageButton saveButton;

        public SavedRecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            recipeImage = itemView.findViewById(R.id.savedRecipesRecyclerView);
            recipeName = itemView.findViewById(R.id.tvNameRecipe);
            recipeAuthor = itemView.findViewById(R.id.tvAuthor);
            recipeTime = itemView.findViewById(R.id.tvTime);
            recipeRating = itemView.findViewById(R.id.tvRating);
            saveButton = itemView.findViewById(R.id.saveButton);
        }
    }
}