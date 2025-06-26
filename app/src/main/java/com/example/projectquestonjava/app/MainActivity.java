package com.example.projectquestonjava.app;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.projectquestonjava.core.utils.Logger;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import lombok.Getter;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

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

    @Inject
    Logger logger;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logger.debug(TAG, "onCreate: Activity creating.");
        setContentView(R.layout.activity_main);

        // Инициализация TestDataInitializer перенесена в MyApplication.onCreate()

        toolbar = findViewById(R.id.toolbar_main);
        appBarLayout = findViewById(R.id.appBarLayout_main);
        setSupportActionBar(toolbar);

        coordinatorLayout = findViewById(R.id.coordinatorLayout_main);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main);
        NavController navController;
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            logger.debug(TAG, "onCreate: NavController obtained.");
        } else {
            logger.error(TAG, "onCreate: NavHostFragment not found! Navigation will not work.");
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
            String destIdString = String.valueOf(destination.getId());
            CharSequence destLabelCS = destination.getLabel();
            String destLabelString = (destLabelCS != null) ? destLabelCS.toString() : "";

            logger.debug(TAG, "NavController: Destination changed to '" + destLabelString + "' (ID: " + destIdString + ")");

            if (topLevelDestinations.contains(destination.getId())) {
                bottomNavigationView.setVisibility(View.VISIBLE);
                logger.debug(TAG, "NavController: BottomNav VISIBLE for " + destLabelString);
            } else {
                bottomNavigationView.setVisibility(View.GONE);
                logger.debug(TAG, "NavController: BottomNav GONE for " + destLabelString);
            }
            fabStandard.hide();
            fabExtended.hide();
            fabExtended.shrink();
            logger.debug(TAG, "NavController: FABs hidden/shrunk for " + destLabelString);

            removeCurrentCustomTitleView();
            toolbar.getMenu().clear();
            logger.debug(TAG, "NavController: Custom title view removed and menu cleared for " + destLabelString);

            if (!destLabelString.isEmpty() &&
                    !destLabelString.equalsIgnoreCase("null") &&
                    !destLabelString.startsWith("@")) {
                toolbar.setTitle(destLabelString);
                logger.debug(TAG, "NavController: Toolbar title set to '" + destLabelString + "' for destination ID " + destIdString);
            } else {
                try {
                    toolbar.setTitle(getString(R.string.app_name));
                    logger.debug(TAG, "NavController: Toolbar title set to app name because destination label was: '" + destLabelString + "' for destination ID " + destIdString);
                } catch (Exception e) {
                    toolbar.setTitle("");
                    logger.warn(TAG, "NavController: Toolbar title cleared (app name not found or error) for destination ID " + destIdString + " (original label: '" + destLabelString + "')");
                }
            }
            toolbar.setSubtitle(null);
        });

        if (snackbarManager != null) {
            snackbarManager.getMessagesEvent().observe(this, snackbarMessage -> {
                if (snackbarMessage != null && snackbarMessage.getMessage() != null && !snackbarMessage.getMessage().isEmpty()) {
                    logger.info(TAG, "SnackbarManager: Showing message '" + snackbarMessage.getMessage() + "' with duration " + snackbarMessage.getDuration());
                    if (coordinatorLayout != null) {
                        Snackbar.make(coordinatorLayout, snackbarMessage.getMessage(), snackbarMessage.getDuration()).show();
                    } else {
                        logger.warn(TAG, "SnackbarManager: CoordinatorLayout is null, cannot show Snackbar.");
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
        logger.info(TAG, "onCreate: Activity creation finished.");
    }

    public void setCustomToolbarTitleView(@Nullable View customTitleView) {
        removeCurrentCustomTitleView();
        if (customTitleView != null) {
            toolbar.setTitle(null);
            toolbar.setSubtitle(null);
            if (customTitleView.getLayoutParams() instanceof MaterialToolbar.LayoutParams) {
                ((MaterialToolbar.LayoutParams) customTitleView.getLayoutParams()).gravity = Gravity.START | Gravity.CENTER_VERTICAL;
            } else {
                MaterialToolbar.LayoutParams params = new MaterialToolbar.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );
                params.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
                customTitleView.setLayoutParams(params);
            }
            toolbar.addView(customTitleView);
            this.currentCustomTitleView = customTitleView;
            logger.debug(TAG, "setCustomToolbarTitleView: Custom view set.");
        } else {
            logger.debug(TAG, "setCustomToolbarTitleView: Custom view is null.");
        }
    }

    private void removeCurrentCustomTitleView() {
        if (currentCustomTitleView != null && currentCustomTitleView.getParent() == toolbar) {
            toolbar.removeView(currentCustomTitleView);
            logger.debug(TAG, "removeCurrentCustomTitleView: Custom view removed.");
        }
        currentCustomTitleView = null;
    }

    public FloatingActionButton getStandardFab() { return fabStandard; }
    public ExtendedFloatingActionButton getExtendedFab() { return fabExtended; }
    public void setCustomTopBar(View customTopBarView) { logger.warn(TAG, "setCustomTopBar called, but not implemented in MainActivity XML version.");}
    public void setCustomFab(View customFabView) {logger.warn(TAG, "setCustomFab called, but not implemented in MainActivity XML version.");}

    public static void deleteAllDatabases(Context context) {
        // Логика удаления баз данных
        File dbDir = new File(context.getApplicationInfo().dataDir, "databases");
        if (dbDir.exists() && dbDir.isDirectory()) {
            String[] children = dbDir.list();
            if (children != null) {
                for (String child : children) {
                    if (child.endsWith(".db")) {
                        if (!new File(dbDir, child).delete()) {
                            System.err.println("Failed to delete database file: " + child);
                        } else {
                            System.out.println("Deleted database file: " + child);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        logger.debug(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        logger.debug(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        logger.debug(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        logger.debug(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        logger.debug(TAG, "onDestroy");
    }
}