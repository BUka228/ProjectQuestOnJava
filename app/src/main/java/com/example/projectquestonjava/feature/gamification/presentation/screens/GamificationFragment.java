package com.example.projectquestonjava.feature.gamification.presentation.screens;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager2.widget.ViewPager2;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.core.ui.BaseFragment;
import com.example.projectquestonjava.feature.gamification.presentation.adapters.GamificationPagerAdapter;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.GamificationViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.Objects;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class GamificationFragment extends BaseFragment {

    private GamificationViewModel viewModel; // Общая ViewModel для хоста и дочерних фрагментов
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private GamificationPagerAdapter pagerAdapter;

    // Конструктор по умолчанию обязателен для фрагментов
    public GamificationFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Инфлейтим макет хоста, который содержит TabLayout и ViewPager2
        return inflater.inflate(R.layout.fragment_gamification_host, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Получаем ViewModel. Так как это хост-фрагмент, его ViewModel
        // будет доступна дочерним фрагментам через requireParentFragment().
        viewModel = new ViewModelProvider(this).get(GamificationViewModel.class);

        tabLayout = view.findViewById(R.id.tabLayout_gamification);
        viewPager = view.findViewById(R.id.viewPager_gamification);

        // Адаптер для ViewPager2
        // Передаем `this` (GamificationFragment), так как дочерние фрагменты будут его частью.
        pagerAdapter = new GamificationPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Связываем TabLayout с ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            // Устанавливаем текст для вкладок
            tab.setText(position == 0 ? "Основной" : "Значки");
            // Здесь можно добавить кастомные иконки для вкладок, если нужно
            // if (position == 0) tab.setIcon(R.drawable.ic_main_gamification);
            // else tab.setIcon(R.drawable.ic_badges);
        }).attach();

        // Наблюдение за выбранной вкладкой из ViewModel для программного переключения (если нужно)
        viewModel.selectedTab.observe(getViewLifecycleOwner(), selectedTabIndex -> {
            if (selectedTabIndex != null && viewPager.getCurrentItem() != selectedTabIndex) {
                viewPager.setCurrentItem(selectedTabIndex, true); // Плавное переключение
            }
        });

        // Установка слушателя для выбора вкладок пользователем для обновления ViewModel
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewModel.selectTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Ничего не делаем
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Ничего не делаем
            }
        });
    }

    @Override
    protected void setupToolbar() {
        MaterialToolbar toolbar = getToolbar();
        if (toolbar != null) {
            // Название экрана "Игра" будет установлено из NavGraph label
            // Toolbar в MainActivity.
            // Если нужно кастомное название, можно установить его здесь.
            toolbar.setTitle(getString(R.string.title_gamification)); // Убедись, что строка есть в strings.xml
            toolbar.getMenu().clear(); // Очищаем меню от предыдущих экранов
            // Здесь не добавляем меню, так как основное меню (три точки) управляется из ProfileScreen
        }
    }

    @Override
    protected void setupFab() {
        // На экране геймификации FAB обычно не нужен или специфичен для вкладки.
        // Дочерние фрагменты (если им нужен FAB) должны будут запросить его настройку.
        // По умолчанию скрываем оба.
        if (getStandardFab() != null) {
            getStandardFab().hide();
        }
        if (getExtendedFab() != null) {
            getExtendedFab().hide();
        }
    }

    // Методы для навигации, которые могут быть вызваны из GamificationMainTabFragment
    public void navigateToGardenScreen() {
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_navigation_gamification_to_gardenFragment);
    }

    public void navigateToAllChallengesScreen() {
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_navigation_gamification_to_challengesFragment);
    }

    @Override
    public void onDestroyView() {
        // Отсоединяем TabLayoutMediator, чтобы избежать утечек
        if (tabLayout != null && viewPager != null) {
            // TabLayoutMediator не имеет явного метода detach,
            // но при уничтожении ViewPager2 и TabLayout связи должны корректно разорваться.
            // Для большей уверенности можно обнулить адаптер ViewPager2
            if(viewPager.getAdapter() != null){
                viewPager.setAdapter(null);
            }
        }
        super.onDestroyView();
        tabLayout = null;
        viewPager = null;
        pagerAdapter = null;
    }
}