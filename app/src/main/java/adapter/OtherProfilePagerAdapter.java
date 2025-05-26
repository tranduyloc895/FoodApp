package adapter;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.ArrayList;
import java.util.List;

import fragment.OtherProfile_UploadedFragment;
import fragment.OtherProfile_SavedFragment;

public class OtherProfilePagerAdapter extends FragmentStateAdapter {
    private static final String TAG = "OtherProfilePagerAdapter";
    private final String currentUserId;
    private final List<String> uploadedRecipeIds;
    private final List<String> savedRecipeIds;

    public OtherProfilePagerAdapter(@NonNull FragmentActivity fragmentActivity, String currentUserId, List<String> uploadedRecipeIds, List<String> savedRecipeIds) {
        super(fragmentActivity);
        this.currentUserId = currentUserId;
        this.uploadedRecipeIds = uploadedRecipeIds;
        this.savedRecipeIds = savedRecipeIds;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                Log.d(TAG, "Creating Uploaded Fragment with " + uploadedRecipeIds.size() + " IDs.");
                OtherProfile_UploadedFragment uploadedFragment = new OtherProfile_UploadedFragment();
                Bundle uploadedBundle = new Bundle();
                uploadedBundle.putStringArrayList("uploaded_recipe_ids", new ArrayList<>(uploadedRecipeIds));
                uploadedFragment.setArguments(uploadedBundle);
                return uploadedFragment;

            case 1:
                Log.d(TAG, "Creating Saved Fragment with " + savedRecipeIds.size() + " IDs.");
                OtherProfile_SavedFragment savedFragment = new OtherProfile_SavedFragment();
                Bundle savedBundle = new Bundle();
                savedBundle.putStringArrayList("saved_recipe_ids", new ArrayList<>(savedRecipeIds));
                savedFragment.setArguments(savedBundle);
                return savedFragment;

            default:
                return new OtherProfile_UploadedFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}