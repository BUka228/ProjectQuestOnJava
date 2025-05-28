// File: A:\Progects\ProjectQuestOnJava\app\src\main\java\com\example\projectquestonjava\app\MainActivity.java
package com.example.projectquestonjava.app;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.core.managers.SnackbarManager;
import com.example.projectquestonjava.core.managers.SnackbarMessage;
import com.google.android.material.appbar.AppBarLayout; // Добавлен импорт
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import lombok.Getter;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    @Getter
    private MaterialToolbar toolbar;
    private AppBarLayout appBarLayout; // Добавили AppBarLayout
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabStandard;
    private ExtendedFloatingActionButton fabExtended;
    private CoordinatorLayout coordinatorLayout;


    @Inject
    SnackbarManager snackbarManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // androidx.activity.EdgeToEdge.enable(this); // Если используется
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar_main);
        appBarLayout = findViewById(R.id.appBarLayout_main); // Инициализируем AppBarLayout
        setSupportActionBar(toolbar);

        coordinatorLayout = findViewById(R.id.coordinatorLayout_main);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main);
        NavController navController;
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        } else {
            if (snackbarManager != null && coordinatorLayout != null) {
                snackbarManager.showMessage("Критическая ошибка: NavHostFragment не найден!", Snackbar.LENGTH_LONG);
            }
            return;
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation_view);
        fabStandard = findViewById(R.id.fab_main_standard);
        fabExtended = findViewById(R.id.fab_main_extended);

        Set<Integer> topLevelDestinations = new HashSet<>();
        topLevelDestinations.add(R.id.navigation_dashboard);
        topLevelDestinations.add(R.id.navigation_calendar_planning);
        topLevelDestinations.add(R.id.navigation_pomodoro);
        topLevelDestinations.add(R.id.navigation_gamification);
        topLevelDestinations.add(R.id.navigation_profile);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(topLevelDestinations).build();

        NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (topLevelDestinations.contains(destination.getId())) {
                bottomNavigationView.setVisibility(View.VISIBLE);
            } else {
                bottomNavigationView.setVisibility(View.GONE);
            }
            fabStandard.hide();
            fabExtended.hide();
            fabExtended.shrink();

            // Очищаем кастомный title view из Toolbar, если он был добавлен фрагментом
            if (toolbar.getChildCount() > 1) { // Обычно 0 или 1 (стандартный Title)
                for (int i = toolbar.getChildCount() - 1; i >= 0; i--) {
                    View child = toolbar.getChildAt(i);
                    // Не удаляем стандартные элементы Toolbar (NavigationIcon, Title TextView, ActionMenuView)
                    // Простой способ - удалять всё, что не является TextView или ImageView с системным ID
                    // Более надежно - если фрагменты сами удаляют свои кастомные View из Toolbar в onDestroyView.
                    // Здесь более простой подход, предполагая, что кастомный title - это единственный дополнительный View.
                    if (!(child instanceof TextView && ((TextView) child).getText().equals(destination.getLabel())) &&
                            !(child instanceof androidx.appcompat.widget.ActionMenuView) &&
                            !(child instanceof ImageView && child.getId() == androidx.appcompat.R.id.home) && // Стандартная иконка "назад" или "бургер"
                            !(child.getId() == androidx.appcompat.R.id.action_bar_title || child.getId() == androidx.appcompat.R.id.action_bar_subtitle)) { // Стандартные TextView для title/subtitle
                        // Для простоты, если у нас есть кастомный title, он скорее всего не будет стандартным TextView с этим ID
                        // Этот блок можно улучшить, если кастомные view будут иметь специальные теги.
                        // Пока что, если мы используем toolbar.addView(customTitleView), то он будет последним.
                        // Но это хрупко.
                    }
                }
            }

            if (destination.getLabel() != null) {
                toolbar.setTitle(destination.getLabel());
            } else {
                toolbar.setTitle(""); // Очищаем, если label нет (для кастомных заголовков)
            }
            toolbar.getMenu().clear();
        });

        if (snackbarManager != null) {
            snackbarManager.getMessagesEvent().observe(this, snackbarMessage -> {
                if (snackbarMessage != null && snackbarMessage.getMessage() != null && !snackbarMessage.getMessage().isEmpty()) {
                    if (coordinatorLayout != null) {
                        Snackbar.make(coordinatorLayout, snackbarMessage.getMessage(), snackbarMessage.getDuration()).show();
                    }
                }
            });
        }

        // Применяем отступы для fitsSystemWindows
        ViewCompat.setOnApplyWindowInsetsListener(coordinatorLayout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Применяем отступ к AppBarLayout
            if (appBarLayout != null) {
                ViewGroup.MarginLayoutParams appBarParams = (ViewGroup.MarginLayoutParams) appBarLayout.getLayoutParams();
                appBarParams.topMargin = insets.top; // Отступ сверху для AppBarLayout
                appBarLayout.setLayoutParams(appBarParams);
            }

            // Обновляем padding для основного контента (FragmentContainerView), чтобы он не уезжал под BottomNavigationView
            // NavHostFragment обычно уже учитывает это, если он находится внутри CoordinatorLayout
            // и BottomNavigationView является его якорем (через layout_behavior).
            // Но для надежности можно применить нижний отступ к FragmentContainerView или его родителю.
            // Здесь мы уже используем fitsSystemWindows="true" для coordinatorLayout,
            // поэтому дополнительный padding для FragmentContainerView обычно не нужен,
            // если AppBarLayout и BottomNavigationView правильно настроены в макете.

            // Отступы для FAB, чтобы они не перекрывались системными элементами (уже учтено через fab_margin_bottom_with_nav)

            // Важно вернуть НЕПОТРЕБЛЕННЫЕ insets, чтобы другие View могли их использовать
            return WindowInsetsCompat.CONSUMED; // WindowInsetsCompat.CONSUMED; // Потребляем все, если fitsSystemWindows="true" у корневого
            // Или: return windowInsets; // Если хотим, чтобы другие View тоже получили отступы
        });
    }

    public FloatingActionButton getStandardFab() {
        return fabStandard;
    }

    public ExtendedFloatingActionButton getExtendedFab() {
        return fabExtended;
    }

    public void setCustomTopBar(View customTopBarView) {
        // Эта логика теперь не актуальна, так как Toolbar управляется централизованно
    }

    public void setCustomFab(View customFabView) {
        // Аналогично, FAB управляется централизованно
    }
}