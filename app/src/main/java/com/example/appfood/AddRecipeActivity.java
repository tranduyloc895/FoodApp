package com.example.appfood;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import adapter.IngredientsAdapter;
import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddRecipeActivity extends AppCompatActivity {

    private static final String TAG = "AddRecipeActivity";
    private EditText etTitle, etInstructions, etTime;
    private RecyclerView rvIngredients;
    private IngredientsAdapter ingredientsAdapter;
    private List<String> ingredientsList = new ArrayList<>();
    private String token;
    private ImageView ivRecipeImage;
    private Uri imageUri;
    private ImageButton ibBack;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        initViews();

        // Receive token from HomeActivity
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

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Submitting recipe...");
        progressDialog.setCancelable(false);
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
            String ingredient = etQuantityName.getText().toString().trim();
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
        String instructionsText = etInstructions.getText().toString().trim();

        // Validate input
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ingredientsList.isEmpty()) {
            Toast.makeText(this, "Please add at least one ingredient", Toast.LENGTH_SHORT).show();
            return;
        }

        if (instructionsText.isEmpty()) {
            Toast.makeText(this, "Please enter instructions", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress dialog
        progressDialog.show();

        try {
            // Split instructions into a list by new lines
            List<String> instructionsList = new ArrayList<>();
            for (String instruction : instructionsText.split("\\n")) {
                if (!instruction.trim().isEmpty()) {
                    instructionsList.add(instruction.trim());
                }
            }

            // Convert to JSON strings
            String ingredientsJson = new Gson().toJson(ingredientsList);
            String instructionsJson = new Gson().toJson(instructionsList);

            Log.d(TAG, "Ingredients JSON: " + ingredientsJson);
            Log.d(TAG, "Instructions JSON: " + instructionsJson);

            // Create a map of parts for the multipart request
            Map<String, RequestBody> partMap = new HashMap<>();

            // Add text parts properly with Multipart encoding
            partMap.put("title", createPartFromString(title));
            partMap.put("time", createPartFromString(time));
            partMap.put("ingredients", createPartFromString(ingredientsJson));
            partMap.put("instructions", createPartFromString(instructionsJson));

            // Create MultipartBody.Part for image
            MultipartBody.Part imagePart = prepareImagePart("imageRecipe", imageUri);

            // Call API service
            ApiService apiService = RetrofitClient.getApiService();
            Call<ModelResponse.RecipeDetailResponse> call = apiService.addRecipeWithParts(
                    "Bearer " + token,
                    partMap,
                    imagePart);

            call.enqueue(new Callback<ModelResponse.RecipeDetailResponse>() {
                @Override
                public void onResponse(Call<ModelResponse.RecipeDetailResponse> call, Response<ModelResponse.RecipeDetailResponse> response) {
                    progressDialog.dismiss();
                    if (response.isSuccessful() && response.body() != null) {
                        Toast.makeText(AddRecipeActivity.this, "Recipe added successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        String errorMessage = "Failed to add recipe. Status code: " + response.code();
                        try {
                            if (response.errorBody() != null) {
                                errorMessage += "\nError: " + response.errorBody().string();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Log.e(TAG, errorMessage);
                        Toast.makeText(AddRecipeActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ModelResponse.RecipeDetailResponse> call, Throwable t) {
                    progressDialog.dismiss();
                    Log.e(TAG, "API call failed", t);
                    Toast.makeText(AddRecipeActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            progressDialog.dismiss();
            Log.e(TAG, "Error submitting recipe", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Create a part from a string value with proper content type
     */
    private RequestBody createPartFromString(String value) {
        return RequestBody.create(MediaType.parse("text/plain"), value);
    }

    /**
     * Prepare image file for upload
     */
    private MultipartBody.Part prepareImagePart(String partName, Uri imageUri) {
        try {
            // Create a file from the URI
            File imageFile = createFileFromUri(imageUri);

            // Get MIME type
            String mimeType = getContentResolver().getType(imageUri);
            if (mimeType == null) {
                mimeType = "image/jpeg"; // Default if can't determine type
            }

            // Create RequestBody instance from file
            RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), imageFile);

            // Create MultipartBody.Part using file name
            return MultipartBody.Part.createFormData(partName, imageFile.getName(), requestFile);
        } catch (Exception e) {
            Log.e(TAG, "Error preparing image", e);
            Toast.makeText(this, "Error preparing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            throw new RuntimeException("Failed to prepare image");
        }
    }

    /**
     * Create a File from a content URI
     */
    private File createFileFromUri(Uri uri) throws IOException {
        String fileName = getFileNameFromUri(uri);
        if (fileName == null) {
            fileName = "image_" + System.currentTimeMillis() + ".jpg";
        }

        File file = new File(getCacheDir(), fileName);

        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new IOException("Cannot open input stream for URI: " + uri);
        }

        FileOutputStream outputStream = new FileOutputStream(file);

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        outputStream.close();
        inputStream.close();

        Log.d(TAG, "Created file from URI: " + file.getAbsolutePath() + ", size: " + file.length());
        return file;
    }

    /**
     * Get file name from URI
     */
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
                    if (columnIndex >= 0) {
                        result = cursor.getString(columnIndex);
                    }
                }
            }
        }

        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            } else {
                result = "image_" + System.currentTimeMillis() + ".jpg";
            }
        }

        return result;
    }
}