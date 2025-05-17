package fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import adapter.Common_RecipeAdapter;
import com.example.appfood.R;

import java.util.ArrayList;
import java.util.List;

import api.ApiService;
import api.ModelResponse;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {
    private RecyclerView recyclerView;
    private Common_RecipeAdapter adapter;
    private List<ModelResponse.RecipeResponse.Recipe> recipeList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.rv_common_recipe);
        recyclerView.setHasFixedSize(true);

        recipeList = new ArrayList<>();
        adapter = new Common_RecipeAdapter(requireContext(), recipeList);
        recyclerView.setAdapter(adapter);

        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 1, GridLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);

        loadJsonData();

        return view;
    }

    private void loadJsonData() {

        String token = getActivity().getIntent().getStringExtra("token");
        if (token == null || token.isEmpty()) {
            Toast.makeText(requireContext(), "Token không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.RecipeResponse> call = apiService.getRecipeLatest("Bearer " + token);

        call.enqueue(new Callback<ModelResponse.RecipeResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Response<ModelResponse.RecipeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    recipeList.clear();

                    List<ModelResponse.RecipeResponse.Recipe> allRecipes = response.body().getData().getRecipes();

                    recipeList.addAll(allRecipes.subList(0, Math.min(allRecipes.size(), 10)));

                    int spanCount = Math.max(1, recipeList.size());
                    GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), spanCount, GridLayoutManager.VERTICAL, false);
                    recyclerView.setLayoutManager(layoutManager);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Throwable t) {
                Log.e("HomeFragment", "API Call Failed: " + t.getMessage());
            }
        });
    }
}