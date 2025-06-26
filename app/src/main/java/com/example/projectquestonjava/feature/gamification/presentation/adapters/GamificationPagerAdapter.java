package com.example.projectquestonjava.feature.gamification.presentation.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.projectquestonjava.feature.gamification.presentation.screens.BadgesTabFragment;
import com.example.projectquestonjava.feature.gamification.presentation.screens.GamificationMainTabFragment;

public class GamificationPagerAdapter extends FragmentStateAdapter {
    public GamificationPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new GamificationMainTabFragment();
        } else {
            return new BadgesTabFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Две вкладки: Основной и Значки
    }
}