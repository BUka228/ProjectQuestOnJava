package com.example.projectquestonjava.app;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets; // Убедитесь, что импорт androidx
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

    // Методы для доступа к UI элементам из BaseFragment
    @Getter
    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabStandard; // Стандартный FAB
    private ExtendedFloatingActionButton fabExtended; // Расширенный FAB
    private CoordinatorLayout coordinatorLayout;


    @Inject
    SnackbarManager snackbarManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // androidx.activity.EdgeToEdge.enable(this); // Если используется
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar_main);
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
            // Можно завершить Activity, если навигация невозможна
            // finish();
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
            // Управление BottomNavigationView
            if (topLevelDestinations.contains(destination.getId())) {
                bottomNavigationView.setVisibility(View.VISIBLE);
            } else {
                bottomNavigationView.setVisibility(View.GONE);
            }

            // Сбрасываем оба FAB по умолчанию при смене экрана.
            // Фрагменты сами решат, какой FAB им нужен и как его настроить.
            fabStandard.hide();
            fabExtended.hide();
            fabExtended.shrink(); // Сжимаем расширенный FAB

            if (destination.getLabel() != null) {
                toolbar.setTitle(destination.getLabel());
            }
            toolbar.getMenu().clear(); // Очищаем меню Toolbar от предыдущего фрагмента
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.coordinatorLayout_main), (v, insets) -> {
            // Ваша логика для EdgeToEdge, если используется.
            // Например, установка padding для CoordinatorLayout.
            // androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public FloatingActionButton getStandardFab() {
        return fabStandard;
    }

    public ExtendedFloatingActionButton getExtendedFab() {
        return fabExtended;
    }

    // Методы setCustom... можно оставить для обратной совместимости,
    // но они не будут активно использоваться при таком подходе.
    public void setCustomTopBar(View customTopBarView) {
        // logger.warn(TAG, "setCustomTopBar(View) called, but standard Toolbar is managed by MainActivity.");
    }

    public void setCustomFab(View customFabView) {
        // logger.warn(TAG, "setCustomFab(View) called, but FABs are managed by MainActivity.");
    }
}