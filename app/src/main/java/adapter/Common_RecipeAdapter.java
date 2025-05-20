package adapter;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.appfood.R;

import java.util.List;

import api.ModelResponse;

public class Common_RecipeAdapter extends RecyclerView.Adapter<Common_RecipeAdapter.ViewHolder> {
    final private Context context;
    final private List<ModelResponse.RecipeResponse.Recipe> recipeList;
    final private OnRecipeClickListener listener;

    public interface OnRecipeClickListener {
        void onRecipeClick(String recipeId);
    }

    public Common_RecipeAdapter(Context context, List<ModelResponse.RecipeResponse.Recipe> recipeList, OnRecipeClickListener listener) {
        this.context = context;
        this.recipeList = recipeList;
        this.listener = listener;
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
    }

    @Override
    public int getItemCount() {
        return recipeList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivRecipeImage;
        TextView tvRecipeName, tvAverageRating;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            ivRecipeImage = itemView.findViewById(R.id.iv_recipe_image_common);
            tvRecipeName = itemView.findViewById(R.id.tv_recipe_name_common);
            tvAverageRating = itemView.findViewById(R.id.tv_average_rating_common);
        }
    }
}

