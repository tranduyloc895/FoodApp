package adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appfood.R;

import java.util.List;

public class IngredientsAdapter extends RecyclerView.Adapter<IngredientsAdapter.ViewHolder> {
    private List<String> ingredientsList;
    private OnIngredientDeleteListener deleteListener;

    public IngredientsAdapter(List<String> ingredientsList, OnIngredientDeleteListener deleteListener) {
        this.ingredientsList = ingredientsList;
        this.deleteListener = deleteListener;
    }

    public interface OnIngredientDeleteListener {
        void onIngredientDelete(int position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_add_recipe_ingredients, parent, false);

        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.width = parent.getMeasuredWidth() / 2;
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        view.setLayoutParams(layoutParams);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String ingredient = ingredientsList.get(position);
        holder.tvIngredient.setText(ingredient);

        holder.ib_ingredient_delete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onIngredientDelete(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return ingredientsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIngredient;
        ImageButton ib_ingredient_delete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIngredient = itemView.findViewById(R.id.tv_ingredient_quantity_name);
            ib_ingredient_delete = itemView.findViewById(R.id.ib_ingredient_delete);
        }
    }
}