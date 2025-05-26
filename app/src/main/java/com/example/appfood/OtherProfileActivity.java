package com.example.appfood;

import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.example.appfood.UserProfileActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

import fragment.OtherProfile_SavedFragment;
import adapter.OtherProfilePagerAdapter;
import api.ApiService;
import api.RetrofitClient;
import api.ModelResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OtherProfileActivity extends AppCompatActivity {
    private static final String TAG = "OtherProfileActivity";
    private TextView tvProfileName, tvNumberUploadedRecipes, tvNumberSavedRecipes, tvCountry, tvLevel;
    private String token, currentUserId;
    private ImageView ivProfileImage;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ImageButton ibBack;
    private List<String> uploadedRecipeIds = new ArrayList<>();
    private List<String> savedRecipeIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other_profile);

        tvProfileName = findViewById(R.id.tv_profile_name);
        tvNumberUploadedRecipes = findViewById(R.id.tv_number_uploaded_recipes);
        tvNumberSavedRecipes = findViewById(R.id.tv_number_saved_recipes);
        tvCountry = findViewById(R.id.tv_country_code);
        tvLevel = findViewById(R.id.tv_profile_level);
        ivProfileImage = findViewById(R.id.iv_profile_picture);
        tabLayout = findViewById(R.id.tl_category);
        viewPager = findViewById(R.id.vp_category);
        ibBack = findViewById(R.id.ib_other_profile_back);

        ibBack.setOnClickListener(v -> {
            Intent intent = new Intent(OtherProfileActivity.this, HomeActivity.class);
            intent.putExtra("token", token);
            intent.putExtra("author_id", currentUserId);
            startActivity(intent);
            finish();
        });

        if (extractToken()) {
            Log.d(TAG, "Final extracted author_id: " + currentUserId);
            Log.d(TAG, "Extracted token: " + token);
            fetchUserProfile();
            fetchUploadedRecipesCount();
            fetchSavedRecipesCount();
        }
    }

    private boolean extractToken() {
        Intent intent = getIntent();
        token = intent.getStringExtra("token");
        currentUserId = intent.getStringExtra("author_id");

        Log.d(TAG, "Extracted token: " + token);
        Log.d(TAG, "Extracted author_id: " + currentUserId);

        if (token == null || token.isEmpty() || currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "Invalid token or author ID!");
            Toast.makeText(this, "Invalid token or author ID!", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void fetchUserProfile() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.UserResponse> call = apiService.getUserById("Bearer " + token, currentUserId);

        call.enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Response<ModelResponse.UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String userName = response.body().getData().getUser().getName();
                    tvProfileName.setText(userName);

                    String country = response.body().getData().getUser().getCountry();
                    String countryCode = (country != null && !country.isEmpty()) ? country.substring(0, Math.min(3, country.length())).toUpperCase() : "NT118";
                    tvCountry.setText(countryCode);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Throwable t) {
                Toast.makeText(OtherProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isUploadedRecipesFetched = false;
    private void fetchUploadedRecipesCount() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.RecipeResponse> call = apiService.getAllRecipes("Bearer " + token);

        call.enqueue(new Callback<ModelResponse.RecipeResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Response<ModelResponse.RecipeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ModelResponse.RecipeResponse.Recipe> allRecipes = response.body().getData().getRecipes();

                    uploadedRecipeIds.clear();
                    int uploadedCount = 0;

                    for (ModelResponse.RecipeResponse.Recipe recipe : allRecipes) {
                        Log.d(TAG, "Recipe Author ID: " + recipe.getAuthor());
                        Log.d(TAG, "Current User ID: " + currentUserId);

                        if (recipe.getAuthor().equals(currentUserId)) {
                            uploadedCount++;
                            uploadedRecipeIds.add(recipe.getId());
                        }
                    }

                    Log.d(TAG, "Uploaded recipes count: " + uploadedCount);
                    tvNumberUploadedRecipes.setText(String.valueOf(uploadedCount));

                    tvLevel.setText(uploadedCount == 0 ? "Guest" :
                            uploadedCount < 5 ? "Beginner" :
                                    uploadedCount < 15 ? "Intermediate" :
                                            uploadedCount < 30 ? "Chef" : "Master Chef");

                    Log.d(TAG, "Uploaded Recipe IDs: " + uploadedRecipeIds);

                    isUploadedRecipesFetched = true;
                    checkAndSetupViewPager();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Throwable t) {
                Toast.makeText(OtherProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isSavedRecipesFetched = false;
    private void fetchSavedRecipesCount() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.UserResponse> call = apiService.getUserById("Bearer " + token, currentUserId);

        call.enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Response<ModelResponse.UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    savedRecipeIds = response.body().getData().getUser().getSavedRecipes();
                    if (savedRecipeIds == null) {
                        savedRecipeIds = new ArrayList<>();
                    }

                    Log.d(TAG, "Saved recipes count: " + savedRecipeIds.size());
                    tvNumberSavedRecipes.setText(String.valueOf(savedRecipeIds.size()));

                    isSavedRecipesFetched = true;
                    checkAndSetupViewPager();
                } else {
                    Toast.makeText(OtherProfileActivity.this, "Không thể tải số lượng công thức đã lưu.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Throwable t) {
                Toast.makeText(OtherProfileActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAndSetupViewPager() {
        Log.d("checkAndSetupViewPager", "Checking if recipe lists are populated...");

        Log.d("checkAndSetupViewPager", "Uploaded Recipes Count: " + uploadedRecipeIds.size());
        for (String id : uploadedRecipeIds) {
            Log.d("checkAndSetupViewPager", "Uploaded Recipe ID: " + id);
        }

        Log.d("checkAndSetupViewPager", "Saved Recipes Count: " + savedRecipeIds.size());
        for (String id : savedRecipeIds) {
            Log.d("checkAndSetupViewPager", "Saved Recipe ID: " + id);
        }

        if (!uploadedRecipeIds.isEmpty() && !savedRecipeIds.isEmpty()) {
            Log.d("checkAndSetupViewPager", "Both lists populated, initializing ViewPager.");

            OtherProfilePagerAdapter adapter = new OtherProfilePagerAdapter(this, currentUserId, uploadedRecipeIds, savedRecipeIds);
            viewPager.setAdapter(adapter);
            Log.d("checkAndSetupViewPager", "ViewPager Adapter set successfully.");

            new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                SpannableString spannableString;
                switch (position) {
                    case 0:
                        spannableString = new SpannableString("Uploaded");
                        break;
                    case 1:
                        spannableString = new SpannableString("Saved");
                        break;
                    default:
                        spannableString = new SpannableString("");
                }

                spannableString.setSpan(new AbsoluteSizeSpan(16, true), 0, spannableString.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                tab.setText(spannableString);
            }).attach();

            Log.d("checkAndSetupViewPager", "ViewPager initialized successfully.");
        } else {
            Log.d("checkAndSetupViewPager", "Waiting for both lists to be populated...");
        }
    }
}