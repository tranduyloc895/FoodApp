package adapter;

import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.appfood.R;

import java.util.List;

import api.ModelResponse;

public class RecipeChatCardAdapter extends RecyclerView.Adapter<RecipeChatCardAdapter.RecipeViewHolder> {
    private static final String TAG = "RecipeChatCardAdapter";

    private final List<ModelResponse.RecipeResponse.Recipe> recipes;
    private final Context context;
    private final OnRecipeClickListener recipeClickListener;

    public interface OnRecipeClickListener {
        void onRecipeClick(String recipeId);
    }

    public RecipeChatCardAdapter(Context context, List<ModelResponse.RecipeResponse.Recipe> recipes,
                                 OnRecipeClickListener listener) {
        this.context = context;
        this.recipes = recipes;
        this.recipeClickListener = listener;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recipe_chat_card, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        ModelResponse.RecipeResponse.Recipe recipe = recipes.get(position);

        // Set recipe title
        holder.titleText.setText(recipe.getTitle());

        // Set author name
        holder.authorText.setText(recipe.getAuthor());

        // Load image
        if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(recipe.getImageUrl())
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.placeholder_image)
                            .centerCrop())
                    .into(holder.recipeImage);
        } else {
            holder.recipeImage.setImageResource(R.drawable.placeholder_image);
        }

        // Set click listener
        holder.cardView.setOnClickListener(v -> {
            if (recipeClickListener != null) {
                recipeClickListener.onRecipeClick(recipe.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView recipeImage;
        TextView titleText;
        TextView authorText;

        RecipeViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            recipeImage = itemView.findViewById(R.id.recipeImage);
            titleText = itemView.findViewById(R.id.titleText);
            authorText = itemView.findViewById(R.id.authorText);
        }
    }
}