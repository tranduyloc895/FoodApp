package adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.ArrayList;
import java.util.List;

import fragment.Profile_UploadedFragment;
import fragment.Profile_SavedFragment;

public class ProfilePagerAdapter extends FragmentStateAdapter {

    private final List<Fragment> fragmentList = new ArrayList<>();

    public ProfilePagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
        fragmentList.add(new Profile_UploadedFragment());
        fragmentList.add(new Profile_SavedFragment());
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragmentList.get(position);
    }

    @Override
    public int getItemCount() {
        return fragmentList.size();
    }
}