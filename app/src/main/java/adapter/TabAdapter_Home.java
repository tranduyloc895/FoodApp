package adapter;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import fragment.HomeFragment;
import fragment.NotificationsFragment;
import fragment.ProfileFragment;
import fragment.SavedRecipeFragment;

public class TabAdapter_Home extends FragmentStateAdapter {
    public TabAdapter_Home(FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new HomeFragment();
            case 1: return new SavedRecipeFragment();
            case 2: return new NotificationsFragment();
            case 3: return new ProfileFragment();
            default: return new HomeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}