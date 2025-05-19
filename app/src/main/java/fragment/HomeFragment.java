package fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.RatingBar;
import android.widget.Toast;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import adapter.Common_RecipeAdapter;
import adapter.New_RecipeAdapter;

import com.example.appfood.MainRecipe;
import com.example.appfood.R;

import java.util.ArrayList;
import java.util.List;

import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * HomeFragment displays lists of common and new recipes.
 * Handles loading data from API and updating RecyclerViews.
 */
public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private static final int MAX_RECIPES_TO_DISPLAY = 10;
    private RecyclerView recyclerView_common, recyclerView_new;
    private Common_RecipeAdapter adapter_common;
    private New_RecipeAdapter adapter_new;
    private List<ModelResponse.RecipeResponse.Recipe> recipeList_common, recipeList_new;
    private String token;

    /**
     * Inflates the fragment layout and initializes RecyclerViews and adapters.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Get token from activity
        token = getActivity().getIntent().getStringExtra("token");
        if (token == null || token.isEmpty()) {
            Toast.makeText(requireContext(), "Token không hợp lệ!", Toast.LENGTH_SHORT).show();
            return view;
        }

        initRecyclerViews(view);
        loadRecipeData();

        return view;
    }

    /**
     * Initializes RecyclerViews and their adapters for common and new recipes.
     */
    private void initRecyclerViews(View view) {
        // Common recipes RecyclerView
        recyclerView_common = view.findViewById(R.id.rv_common_recipe);
        recyclerView_common.setHasFixedSize(true);
        recipeList_common = new ArrayList<>();
        adapter_common = new Common_RecipeAdapter(requireContext(), recipeList_common, this::navigateToRecipeDetails);
        recyclerView_common.setAdapter(adapter_common);
        recyclerView_common.setLayoutManager(new GridLayoutManager(requireContext(), 1, GridLayoutManager.VERTICAL, false));

        // New recipes RecyclerView
        recyclerView_new = view.findViewById(R.id.rv_new_recipe);
        recyclerView_new.setHasFixedSize(true);
        recipeList_new = new ArrayList<>();
        adapter_new = new New_RecipeAdapter(requireContext(), recipeList_new, this::navigateToRecipeDetails);
        recyclerView_new.setAdapter(adapter_new);
        recyclerView_new.setLayoutManager(new GridLayoutManager(requireContext(), 2, GridLayoutManager.VERTICAL, false));
    }

    /**
     * Navigate to recipe details screen with recipe ID and token
     * @param recipeId The ID of the recipe to display
     */
    private void navigateToRecipeDetails(String recipeId) {
        Intent intent = new Intent(requireContext(), MainRecipe.class);
        intent.putExtra("recipe_id", recipeId);
        intent.putExtra("token", token);
        startActivity(intent);
    }

    /**
     * Loads recipe data from the API for both common and new recipes.
     */
    private void loadRecipeData() {
        loadCommonRecipes();
        loadNewRecipes();
    }

    /**
     * Loads common recipes from the API and updates the corresponding RecyclerView.
     */
    private void loadCommonRecipes() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.RecipeResponse> call = apiService.getRecipeLatest("Bearer " + token);

        call.enqueue(new Callback<ModelResponse.RecipeResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Response<ModelResponse.RecipeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    recipeList_common.clear();
                    List<ModelResponse.RecipeResponse.Recipe> allRecipes = response.body().getData().getRecipes();
                    List<ModelResponse.RecipeResponse.Recipe> displayRecipes =
                            allRecipes.subList(0, Math.min(allRecipes.size(), MAX_RECIPES_TO_DISPLAY));
                    recipeList_common.addAll(displayRecipes);

                    // Fetch ratings for each recipe
                    fetchRatingsForRecipes(apiService, recipeList_common);

                    // Notify adapter of data changes
                    adapter_common.notifyDataSetChanged();

                    recyclerView_common.setLayoutManager(new GridLayoutManager(requireContext(),
                            Math.max(1, recipeList_common.size()), GridLayoutManager.VERTICAL, false));
                } else {
                    Log.e(TAG, "Failed to load common recipes: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "API Call Failed (Common): " + t.getMessage());
            }
        });
    }

    /**
     * Loads new recipes from the API and updates the corresponding RecyclerView.
     */
    private void loadNewRecipes() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.RecipeResponse> call = apiService.getRecipeLatest("Bearer " + token);

        call.enqueue(new Callback<ModelResponse.RecipeResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Response<ModelResponse.RecipeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    recipeList_new.clear();
                    List<ModelResponse.RecipeResponse.Recipe> allRecipes = response.body().getData().getRecipes();
                    List<ModelResponse.RecipeResponse.Recipe> displayRecipes =
                            allRecipes.subList(0, Math.min(allRecipes.size(), MAX_RECIPES_TO_DISPLAY));
                    recipeList_new.addAll(displayRecipes);

                    // Fetch ratings for each recipe
                    fetchRatingsForRecipes(apiService, recipeList_new);

                    // Notify adapter of data changes
                    adapter_new.notifyDataSetChanged();

                    int spanCount = Math.max(1, recipeList_new.size());
                    recyclerView_new.setLayoutManager(new GridLayoutManager(requireContext(),
                            spanCount, GridLayoutManager.VERTICAL, false));
                } else {
                    Log.e(TAG, "Failed to load new recipes: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "API Call Failed (New): " + t.getMessage());
            }
        });
    }

    /**
     * Fetches ratings for a list of recipes and updates their rating values.
     * @param apiService The API service to use for requests
     * @param recipes The list of recipes to fetch ratings for
     */
    private void fetchRatingsForRecipes(ApiService apiService, List<ModelResponse.RecipeResponse.Recipe> recipes) {
        for (ModelResponse.RecipeResponse.Recipe recipe : recipes) {
            String recipeId = recipe.getId();
            Call<ModelResponse.AverageRatingResponse> ratingCall = apiService.getRecipeRating("Bearer " + token, recipeId);

            ratingCall.enqueue(new Callback<ModelResponse.AverageRatingResponse>() {
                @Override
                public void onResponse(@NonNull Call<ModelResponse.AverageRatingResponse> call,
                                       @NonNull Response<ModelResponse.AverageRatingResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Double averageRating = response.body().getData().getAverageRating();
                        recipe.setAverageRating(averageRating != null ? averageRating : 0.0);

                        // Notify of data change for the specific adapters
                        if (recipeList_common.contains(recipe)) {
                            adapter_common.notifyDataSetChanged();
                        } else if (recipeList_new.contains(recipe)) {
                            adapter_new.notifyDataSetChanged();
                        }
                    } else {
                        Log.e(TAG, "Failed to get rating for Recipe ID: " + recipeId);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ModelResponse.AverageRatingResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "API Call Failed (Rating) - Recipe ID: " + recipeId + ", Error: " + t.getMessage());
                }
            });
        }
    }
}
