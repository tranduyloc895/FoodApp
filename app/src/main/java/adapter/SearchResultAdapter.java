package adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appfood.R;

import java.util.List;

import api.ModelResponse.RecipeResponse.Recipe;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.SearchViewHolder> {
    private final Context context;
    private List<Recipe> searchResults;
    private final OnRecipeClickListener clickListener;

    public interface OnRecipeClickListener {
        void onRecipeClick(String recipeId);
    }

    public SearchResultAdapter(Context context, List<Recipe> searchResults, OnRecipeClickListener clickListener) {
        this.context = context;
        this.searchResults = searchResults;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new SearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
        Recipe recipe = searchResults.get(position);

        holder.tvTitle.setText(recipe.getTitle());
        holder.tvAuthor.setText(recipe.getAuthor());

        // Load recipe image
        if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(recipe.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(holder.ivRecipe);
        } else {
            holder.ivRecipe.setImageResource(R.drawable.placeholder_image);
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onRecipeClick(recipe.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return searchResults != null ? searchResults.size() : 0;
    }

    public void updateSearchResults(List<Recipe> newResults) {
        this.searchResults = newResults;
        notifyDataSetChanged();
    }

    static class SearchViewHolder extends RecyclerView.ViewHolder {
        ImageView ivRecipe;
        TextView tvTitle;
        TextView tvAuthor;

        public SearchViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRecipe = itemView.findViewById(R.id.iv_search_recipe);
            tvTitle = itemView.findViewById(R.id.tv_search_title);
            tvAuthor = itemView.findViewById(R.id.tv_search_author);
        }
    }
}