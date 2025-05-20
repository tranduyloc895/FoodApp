package com.example.appfood;

import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import adapter.IngredientsAdapter;
import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddRecipeActivity extends AppCompatActivity {

    private EditText etTitle, etInstructions, etTime;
    private RecyclerView rvIngredients;
    private IngredientsAdapter ingredientsAdapter;
    private List<String> ingredientsList = new ArrayList<>();
    private String token;
    private ImageView ivRecipeImage;
    private Uri imageUri;
    private ImageButton ibBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        initViews();

        // Nhận token từ HomeActivity
        token = getIntent().getStringExtra("token");

        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Token is missing!", Toast.LENGTH_SHORT).show();
        }

        initRecyclerView();
        setupAddIngredientButton();
        setupSubmitRecipeButton();
        setupImageSelection();
        setupBackButton();
    }

    private void initViews() {
        etTitle = findViewById(R.id.et_add_recipe_name);
        etInstructions = findViewById(R.id.et_add_recipe_instruction);
        rvIngredients = findViewById(R.id.rv_add_recipe_ingredients);
        etTime = findViewById(R.id.et_add_recipe_time);
        ivRecipeImage = findViewById(R.id.iv_add_recipe_image);
        ibBack = findViewById(R.id.ib_add_recip_back);
    }

    private void initRecyclerView() {
        ingredientsAdapter = new IngredientsAdapter(ingredientsList, position -> {
            ingredientsList.remove(position);
            ingredientsAdapter.notifyItemRemoved(position);
        });

        rvIngredients.setLayoutManager(new GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false));
        rvIngredients.setAdapter(ingredientsAdapter);
    }

    private void setupAddIngredientButton() {
        ImageButton ibAddIngredients = findViewById(R.id.ib_add_ingredients);
        EditText etQuantityName = findViewById(R.id.et_quantity_name);

        ibAddIngredients.setOnClickListener(view -> {
            String ingredient = etQuantityName.getText().toString();
            if (!ingredient.isEmpty()) {
                ingredientsList.add(ingredient);
                ingredientsAdapter.notifyDataSetChanged();
                etQuantityName.setText("");
            }
        });
    }

    private void setupSubmitRecipeButton() {
        ImageButton btnAddRecipe = findViewById(R.id.ib_finish);
        btnAddRecipe.setOnClickListener(view -> {
            if (token != null && !token.isEmpty()) {
                submitRecipe(token);
            } else {
                Toast.makeText(this, "Token is missing!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBackButton() {
        ibBack.setOnClickListener(v -> finish());
    }

    private void setupImageSelection() {
        findViewById(R.id.fl_add_recipe_image).setOnClickListener(v -> openGallery());
    }

    private void openGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent intent = new Intent(android.provider.MediaStore.ACTION_PICK_IMAGES);
            imagePicker.launch(intent);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePicker.launch(intent);
        }
    }

    private final androidx.activity.result.ActivityResultLauncher<Intent> imagePicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    ivRecipeImage.setImageURI(imageUri);
                }
            });

    private void submitRecipe(String token) {
        String title = etTitle.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String imageUrl = (imageUri != null) ? imageUri.toString() : "";

        if (title.isEmpty() || ingredientsList.isEmpty() || etInstructions.getText().toString().trim().isEmpty() || imageUrl.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        String ingredientsJson = new Gson().toJson(ingredientsList);
        String instructionsJson = new Gson().toJson(etInstructions.getText().toString().trim().split("\\n"));

        Map<String, Object> recipeData = new HashMap<>();
        recipeData.put("title", title);
        recipeData.put("ingredients", new Gson().fromJson(ingredientsJson, List.class));
        recipeData.put("instructions", new Gson().fromJson(instructionsJson, List.class));
        recipeData.put("time", time);
        recipeData.put("image_url", imageUrl);

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.RecipeResponse> call = apiService.addRecipe("Bearer " + token, recipeData);

        call.enqueue(new Callback<ModelResponse.RecipeResponse>() {
            @Override
            public void onResponse(Call<ModelResponse.RecipeResponse> call, Response<ModelResponse.RecipeResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddRecipeActivity.this, "Thêm công thức thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AddRecipeActivity.this, "Lỗi khi thêm công thức: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ModelResponse.RecipeResponse> call, Throwable t) {
                Toast.makeText(AddRecipeActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}