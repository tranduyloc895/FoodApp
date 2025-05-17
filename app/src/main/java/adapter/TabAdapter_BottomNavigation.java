package adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.ArrayList;
import java.util.List;

import fragment.HomeFragment;
import fragment.NotificationsFragment;
import fragment.ProfileFragment;
import fragment.SavedRecipeFragment;

public class TabAdapter_BottomNavigation extends FragmentStateAdapter {
    private final List<Fragment> fragmentList = new ArrayList<>();

    public TabAdapter_BottomNavigation(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        fragmentList.add(new HomeFragment());
        fragmentList.add(new SavedRecipeFragment());
        fragmentList.add(new NotificationsFragment());
        fragmentList.add(new ProfileFragment());
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