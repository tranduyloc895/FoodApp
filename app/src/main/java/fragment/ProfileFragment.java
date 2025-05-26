package fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.example.appfood.R;
import com.example.appfood.UserProfileActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

import adapter.ProfilePagerAdapter;
import api.ApiService;
import api.RetrofitClient;
import api.ModelResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private TextView tvProfileName, tvNumberUploadedRecipes, tvNumberSavedRecipes, tvCountry, tvLevel;
    private String token, currentUserId;
    private ImageView ivProfileImage;
    private View loadingOverlay;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvProfileName.setSelected(true);
        tvNumberUploadedRecipes = view.findViewById(R.id.tv_number_uploaded_recipes);
        tvNumberSavedRecipes = view.findViewById(R.id.tv_number_saved_recipes);
        tvCountry = view.findViewById(R.id.tv_country_code);
        tvLevel = view.findViewById(R.id.tv_profile_level);
        ivProfileImage = view.findViewById(R.id.iv_profile_picture);
        TabLayout tabLayout = view.findViewById(R.id.tl_category);
        ViewPager2 viewPager = view.findViewById(R.id.vp_category);
        loadingOverlay = view.findViewById(R.id.loading_overlay);

        ProfilePagerAdapter adapter = new ProfilePagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            SpannableString spannableString;
            switch (position) {
                case 0: spannableString = new SpannableString("Uploaded"); break;
                case 1: spannableString = new SpannableString("Saved"); break;
                default: spannableString = new SpannableString("");
            }

            spannableString.setSpan(new AbsoluteSizeSpan(16, true), 0, spannableString.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            tab.setText(spannableString);
        }).attach();

        if (extractToken()) {
            fetchUserProfile();
            fetchSavedRecipesCount();
        }

        ivProfileImage.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), UserProfileActivity.class);
            intent.putExtra("token", token);
            startActivity(intent);
        });
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
                    String userName = response.body().getData().getUser().getName();
                    tvProfileName.setText(userName);

                    String country = response.body().getData().getUser().getCountry();
                    String countryCode = (country != null && !country.isEmpty()) ? country.substring(0, Math.min(3, country.length())).toUpperCase() : "NT118";
                    tvCountry.setText(countryCode);

                    currentUserId = response.body().getData().getUser().getId();
                    fetchUploadedRecipesCount();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchUploadedRecipesCount() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.RecipeResponse> call = apiService.getAllRecipes("Bearer " + token);

        call.enqueue(new Callback<ModelResponse.RecipeResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Response<ModelResponse.RecipeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ModelResponse.RecipeResponse.Recipe> allRecipes = response.body().getData().getRecipes();

                    int uploadedCount = 0;
                    for (ModelResponse.RecipeResponse.Recipe recipe : allRecipes) {
                        Log.d(TAG, "Recipe Author ID: " + recipe.getAuthor());
                        Log.d(TAG, "Current User ID: " + currentUserId);

                        if (recipe.getAuthor().equals(currentUserId)) {
                            uploadedCount++;
                        }
                    }

                    Log.d(TAG, "Uploaded recipes count: " + uploadedCount);

                    if (tvNumberUploadedRecipes != null) {
                        tvNumberUploadedRecipes.setText(String.valueOf(uploadedCount));
                    }

                    if(uploadedCount == 0) {
                        tvLevel.setText("Guest");
                    } else if (uploadedCount < 5) {
                        tvLevel.setText("Beginner");
                    } else if (uploadedCount < 15) {
                        tvLevel.setText("Intermediate");
                    } else if (uploadedCount < 30) {
                        tvLevel.setText("Chef");
                    } else {
                        tvLevel.setText("Master Chef");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.RecipeResponse> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchSavedRecipesCount() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ModelResponse.UserResponse> call = apiService.getUserInfo("Bearer " + token);

        call.enqueue(new Callback<ModelResponse.UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Response<ModelResponse.UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<String> savedRecipes = response.body().getData().getUser().getSavedRecipes();
                    int savedCount = (savedRecipes != null) ? savedRecipes.size() : 0;

                    Log.d(TAG, "Saved recipes count: " + savedCount);

                    if (tvNumberSavedRecipes != null) {
                        tvNumberSavedRecipes.setText(String.valueOf(savedCount));
                    }
                } else {
                    Toast.makeText(requireContext(), "Không thể tải số lượng công thức đã lưu.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ModelResponse.UserResponse> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}