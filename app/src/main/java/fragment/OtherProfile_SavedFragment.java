package fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appfood.MainRecipe;
import com.example.appfood.R;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import adapter.OtherProfile_SavedAdapter;
import api.ModelResponse;
import api.ApiService;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OtherProfile_SavedFragment extends Fragment {
    private static final String TAG = "Profile_SavedFragment";
    private TextView titleTextView;
    private RecyclerView recyclerView;
    private OtherProfile_SavedAdapter adapter;
    private String token;
    private List<ModelResponse.RecipeDetailResponse.Recipe> savedRecipes;
    private View loadingOverlay;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_other_saved, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.rv_saved_other_profile);
        titleTextView = view.findViewById(R.id.titleTextView);
        loadingOverlay = view.findViewById(R.id.loading_overlay);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        if (extractToken() && getArguments() != null) {
            List<String> savedRecipeIds = getArguments().getStringArrayList("saved_recipe_ids");

            if (savedRecipeIds != null) {
                Log.d(TAG, "Received saved recipe IDs count in Fragment: " + savedRecipeIds.size());
                for (String recipeId : savedRecipeIds) {
                    Log.d(TAG, "Received Recipe ID: " + recipeId);
                }
            } else {
                Log.d(TAG, "Bundle does not contain saved recipe IDs.");
            }

            if (savedRecipeIds != null && !savedRecipeIds.isEmpty()) {
                showLoading();
                fetchRecipeDetails(savedRecipeIds);
            } else {
                Toast.makeText(requireContext(), "Không có công thức nào được lưu.", Toast.LENGTH_SHORT).show();
                savedRecipes = new ArrayList<>();
                adapter = new OtherProfile_SavedAdapter(getContext(), savedRecipes, token);
                recyclerView.setAdapter(adapter);
            }
        }
    }

    private boolean extractToken() {
        if (getActivity() == null) return false;

        Intent intent = getActivity().getIntent();
        token = intent.getStringExtra("token");

        if (token == null || token.isEmpty()) {
            Toast.makeText(requireContext(), "Invalid token!", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void fetchRecipeDetails(List<String> recipeIds) {
        if (recipeIds == null || recipeIds.isEmpty()) {
            hideLoading();
            return;
        }

        savedRecipes = new ArrayList<>();
        final AtomicInteger pendingRequests = new AtomicInteger(recipeIds.size());
        ApiService apiService = RetrofitClient.getApiService();

        for (String recipeId : recipeIds) {
            Log.d(TAG, "Fetching details for recipe ID: " + recipeId);
            Call<ModelResponse.RecipeDetailResponse> call = apiService.getRecipeDetail("Bearer " + token, recipeId);

            call.enqueue(new Callback<ModelResponse.RecipeDetailResponse>() {
                @Override
                public void onResponse(@NonNull Call<ModelResponse.RecipeDetailResponse> call, @NonNull Response<ModelResponse.RecipeDetailResponse> response) {
                    Log.d(TAG, "API Response Code: " + response.code());

                    if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        ModelResponse.RecipeDetailResponse.Recipe recipe = response.body().getData().getRecipe();
                        if (recipe != null) {
                            savedRecipes.add(recipe);
                            Log.d(TAG, "Fetched Recipe: " + recipe.getTitle());
                        } else {
                            Log.d(TAG, "No recipe found for ID: " + recipeId);
                        }
                    } else {
                        Log.e(TAG, "Error response: " + response.errorBody());
                    }

                    if (pendingRequests.decrementAndGet() == 0) {
                        updateUI(savedRecipes);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ModelResponse.RecipeDetailResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "API Call Failed: " + t.getMessage());

                    if (pendingRequests.decrementAndGet() == 0) {
                        updateUI(savedRecipes);
                    }
                }
            });
        }
    }

    /**
     * Update the UI with recipe data after ratings are fetched
     */
    private void updateUI(List<ModelResponse.RecipeDetailResponse.Recipe> savedRecipes) {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                Log.d(TAG, "Updating UI with saved recipes count: " + savedRecipes.size());
                for (ModelResponse.RecipeDetailResponse.Recipe recipe : savedRecipes) {
                    Log.d(TAG, "Recipe Title: " + recipe.getTitle());
                }

                hideLoading();

                if (savedRecipes.isEmpty()) {
                    Log.e(TAG, "No recipes available to display in RecyclerView!");
                }

                adapter = new OtherProfile_SavedAdapter(getContext(), savedRecipes, token);
                recyclerView.setAdapter(adapter);

                adapter.setOnItemClickListener((int position) -> {
                    if (position < 0 || position >= savedRecipes.size()) return;

                    ModelResponse.RecipeDetailResponse.Recipe recipe = savedRecipes.get(position);
                    Intent intent = new Intent(getActivity(), MainRecipe.class);
                    intent.putExtra("recipe_id", recipe.getId());
                    intent.putExtra("token", token);
                    startActivity(intent);
                });

                Log.d(TAG, "RecyclerView Adapter is set successfully.");
            });
        }
    }

    private void showLoading() {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        if (loadingOverlay != null && isAdded()) {
            requireActivity().runOnUiThread(() -> {
                loadingOverlay.setVisibility(View.GONE);
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (token != null && !token.isEmpty()) {
            updateUI(savedRecipes);
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(View.VISIBLE);
            }
        }
    }
}