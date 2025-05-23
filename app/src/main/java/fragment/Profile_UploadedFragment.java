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
import adapter.Profile_UploadedAdapter;
import api.ModelResponse;
import api.ApiService;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Profile_UploadedFragment extends Fragment {
    private static final String TAG = "Profile_UploadedFragment";
    private TextView titleTextView;
    private RecyclerView recyclerView;
    private Profile_UploadedAdapter adapter;
    private String token;
    private String currentUserId;
    private List<ModelResponse.RecipeResponse.Recipe> uploadedRecipes;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_uploaded, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.rv_uploaded_profile);
        titleTextView = view.findViewById(R.id.titleTextView);
        uploadedRecipes = new ArrayList<>();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        if (extractToken()) {
            fetchUserProfile();
        }
    }

    private boolean extractToken() {
        if (getActivity() == null) return false;

        token = getActivity().getIntent().getStringExtra("token");
        if (token == null || token.isEmpty()) {
            Toast.makeText(requireContext(), "Invalid token!", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void fetchUserProfile() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.UserResponse> call = apiService.getUserInfo("Bearer " + token);

        call.enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Response<ModelResponse.UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUserId = response.body().getData().getUser().getId();
                    fetchUploadedRecipes();
                } else {
                    Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchUploadedRecipes() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.RecipeResponse> call = apiService.getRecipeLatest("Bearer " + token);

        call.enqueue(new Callback<ModelResponse.RecipeResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Response<ModelResponse.RecipeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ModelResponse.RecipeResponse.Recipe> allRecipes = response.body().getData().getRecipes();

                    List<ModelResponse.RecipeResponse.Recipe> uploadedRecipes = new ArrayList<>();
                    for (ModelResponse.RecipeResponse.Recipe recipe : allRecipes) {
                        if (recipe.getAuthor().equals(currentUserId)) {
                            uploadedRecipes.add(recipe);
                        }
                    }

                    if (uploadedRecipes.isEmpty()) {
                        Toast.makeText(requireContext(), "Bạn chưa đăng tải bất kỳ công thức nào.", Toast.LENGTH_SHORT).show();
                    } else {
                        adapter = new Profile_UploadedAdapter(getContext(), uploadedRecipes, token);

                        recyclerView.setAdapter(adapter);
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to load uploaded recipes", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchUserProfile();
    }
}