package adapter;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
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

import java.util.List;

import api.ModelResponse;

public class New_RecipeAdapter extends RecyclerView.Adapter<New_RecipeAdapter.ViewHolder> {
    final private Context context;
    final private List<ModelResponse.RecipeResponse.Recipe> recipeList;
    final private OnRecipeClickListener listener;

    public interface OnRecipeClickListener {
        void onRecipeClick(String recipeId);
    }

    public New_RecipeAdapter(Context context, List<ModelResponse.RecipeResponse.Recipe> recipeList, OnRecipeClickListener listener) {
        this.context = context;
        this.recipeList = recipeList;
        this.listener = listener;
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
        holder.tvRecipeAuthor.setText(recipe.getAuthor());

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
