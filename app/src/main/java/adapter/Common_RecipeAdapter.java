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

import api.ModelResponse;

public class Common_RecipeAdapter extends RecyclerView.Adapter<Common_RecipeAdapter.ViewHolder> {
    private Context context;
    private List<ModelResponse.RecipeResponse.Recipe> recipeList;

    public Common_RecipeAdapter(Context context, List<ModelResponse.RecipeResponse.Recipe> recipeList) {
        this.context = context;
        this.recipeList = recipeList;
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
        holder.tvRecipeAuthor.setText(recipe.getAuthor());
        Glide.with(context).load(recipe.getImageUrl()).into(holder.ivRecipeImage);
    }

    @Override
    public int getItemCount() {
        return recipeList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivRecipeImage;
        TextView tvRecipeName, tvRecipeAuthor;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRecipeImage = itemView.findViewById(R.id.iv_recipe_image_common);
            tvRecipeName = itemView.findViewById(R.id.tv_recipe_name_common);
            tvRecipeAuthor = itemView.findViewById(R.id.tv_recipe_author_common);
        }
    }
}