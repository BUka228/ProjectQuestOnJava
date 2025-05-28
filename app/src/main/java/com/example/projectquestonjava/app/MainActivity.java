// File: A:\Progects\ProjectQuestOnJava\app\src\main\java\com\example\projectquestonjava\app\MainActivity.java
package com.example.projectquestonjava.app;

import android.os.Bundle;
import android.view.Gravity;
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
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import lombok.Getter;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    @Getter
    private MaterialToolbar toolbar;
    private AppBarLayout appBarLayout;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabStandard;
    private ExtendedFloatingActionButton fabExtended;
    private CoordinatorLayout coordinatorLayout;

    @Nullable private View currentCustomTitleView = null;

    @Inject
    SnackbarManager snackbarManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar_main);
        appBarLayout = findViewById(R.id.appBarLayout_main);
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

            // --- ОБНОВЛЕННАЯ ОЧИСТКА Toolbar ---
            removeCurrentCustomTitleView(); // Удаляем кастомный заголовок, если он был
            toolbar.getMenu().clear();      // Очищаем меню

            // Устанавливаем стандартный заголовок, если он есть в NavGraph
            // и если фрагмент сам не установит кастомный заголовок позже
            CharSequence destinationLabel = destination.getLabel();
            if (destinationLabel != null && !destinationLabel.toString().isEmpty()) {
                toolbar.setTitle(destinationLabel);
            } else {
                toolbar.setTitle(""); // Очищаем заголовок, если label нет
            }
            toolbar.setSubtitle(null);
            // ------------------------------------
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

        ViewCompat.setOnApplyWindowInsetsListener(coordinatorLayout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (appBarLayout != null) {
                ViewGroup.MarginLayoutParams appBarParams = (ViewGroup.MarginLayoutParams) appBarLayout.getLayoutParams();
                if (appBarParams.topMargin != insets.top) {
                    appBarParams.topMargin = insets.top;
                    appBarLayout.setLayoutParams(appBarParams);
                }
            }
            v.setPadding(insets.left, 0, insets.right, insets.bottom);
            return windowInsets.inset(0, insets.top, 0, insets.bottom);
        });
    }

    public void setCustomToolbarTitleView(@Nullable View customTitleView) {
        removeCurrentCustomTitleView(); // Удаляем предыдущий
        if (customTitleView != null) {
            toolbar.setTitle(null); // Убираем стандартный текст заголовка
            toolbar.setSubtitle(null);
            // Устанавливаем параметры для центрирования, если это LinearLayout или FrameLayout
            if (customTitleView.getLayoutParams() instanceof MaterialToolbar.LayoutParams) {
                ((MaterialToolbar.LayoutParams) customTitleView.getLayoutParams()).gravity = Gravity.START | Gravity.CENTER_VERTICAL;
            } else {
                MaterialToolbar.LayoutParams params = new MaterialToolbar.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, // или MATCH_PARENT, если нужно растянуть
                        ViewGroup.LayoutParams.MATCH_PARENT
                );
                params.gravity = Gravity.START | Gravity.CENTER_VERTICAL; // Выравнивание по левому краю и по центру вертикали
                customTitleView.setLayoutParams(params);
            }
            toolbar.addView(customTitleView);
            this.currentCustomTitleView = customTitleView;
        }
        // Если customTitleView == null, стандартный title (если он был установлен из NavGraph) должен восстановиться,
        // так как мы не устанавливаем его в "" здесь.
        // NavController при смене destination снова вызовет toolbar.setTitle(destination.getLabel()).
    }

    private void removeCurrentCustomTitleView() {
        if (currentCustomTitleView != null && currentCustomTitleView.getParent() == toolbar) {
            toolbar.removeView(currentCustomTitleView);
        }
        currentCustomTitleView = null;
    }

    public FloatingActionButton getStandardFab() { return fabStandard; }
    public ExtendedFloatingActionButton getExtendedFab() { return fabExtended; }
    public void setCustomTopBar(View customTopBarView) {} // Не используется
    public void setCustomFab(View customFabView) {} // Не используется
}