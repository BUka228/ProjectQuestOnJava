package com.example.projectquestonjava.feature.gamification.presentation.screens;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.core.ui.BaseFragment;
import com.example.projectquestonjava.core.utils.Logger; // Предполагаем, что Logger можно получить из ViewModel
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengePeriod;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeProgressFullDetails;
import com.example.projectquestonjava.feature.gamification.presentation.adapters.ChallengesAdapter;
import com.example.projectquestonjava.feature.gamification.presentation.ui_parts.ChallengeDetailsBottomSheetFragment;
import com.example.projectquestonjava.feature.gamification.presentation.ui_parts.ChallengesSortFilterBottomSheetFragment;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.ChallengeFilterOption;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.ChallengeSortOption;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.ChallengesScreenUiState;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.ChallengesViewModel;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.GamificationViewModel; // Для показа деталей
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import java.util.Collections;
import java.util.Objects;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChallengesFragment extends BaseFragment implements ChallengesAdapter.OnChallengeItemClickListener {

    private ChallengesViewModel challengesViewModel;
    private GamificationViewModel gamificationViewModelShared; // Для показа деталей через общую VM
    private Logger logger; // Логгер для этого фрагмента

    private RecyclerView recyclerViewChallenges;
    private ChallengesAdapter challengesAdapter;
    private TabLayout tabLayoutPeriods;
    private ProgressBar progressBarLoading;
    private LinearLayout emptyStateLayout;
    private TextView textViewError;
    private View errorStateViewRoot;
    private Button buttonRetryError;

    private MenuItem filterSortMenuItem;
    private View filterIndicatorDot;

    private boolean isChallengeDetailsSheetShown = false;

    public ChallengesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        challengesViewModel = new ViewModelProvider(this).get(ChallengesViewModel.class);
        logger = challengesViewModel.getLogger(); // Получаем логгер из основной ViewModel этого экрана

        try {
            // Получаем общую GamificationViewModel через Activity для показа деталей
            gamificationViewModelShared = new ViewModelProvider(requireActivity()).get(GamificationViewModel.class);
            logger.debug("ChallengesFragment", "Shared GamificationViewModel obtained from Activity. VM Hash: " + gamificationViewModelShared.hashCode());
        } catch (Exception e) {
            logger.error("ChallengesFragment", "Could not obtain shared GamificationViewModel from Activity", e);
            // gamificationViewModelShared останется null, нужно будет проверять перед использованием
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (logger != null) logger.debug("ChallengesFragment", "onCreateView - Adding MenuProvider");
        else android.util.Log.d("ChallengesFragment", "onCreateView - Adding MenuProvider (logger was null)");

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                if (logger != null) logger.debug("ChallengesFragment_MenuProvider", "onCreateMenu called");
                else android.util.Log.d("ChallengesFragment_MenuProvider", "onCreateMenu called (logger was null)");

                menuInflater.inflate(R.menu.challenges_toolbar_menu, menu);
                filterSortMenuItem = menu.findItem(R.id.action_filter_sort_challenges);

                if (filterSortMenuItem == null) {
                    if (logger != null) logger.error("ChallengesFragment_MenuProvider", "action_filter_sort_challenges MenuItem NOT FOUND!");
                    else android.util.Log.e("ChallengesFragment_MenuProvider", "action_filter_sort_challenges MenuItem NOT FOUND!");
                    return;
                }

                // Логика для ActionView, если он используется
                View actionView = filterSortMenuItem.getActionView();
                if (actionView != null) {
                    if (logger != null) logger.debug("ChallengesFragment_MenuProvider", "ActionView found for filter/sort menu item.");
                    filterIndicatorDot = actionView.findViewById(R.id.filter_indicator_dot_view);
                    actionView.setOnClickListener(v -> {
                        if (logger != null) logger.info("ChallengesFragment_MenuProvider", "Filter/Sort ActionView CLICKED! Showing BottomSheet...");
                        else android.util.Log.i("ChallengesFragment_MenuProvider", "Filter/Sort ActionView CLICKED! Showing BottomSheet...");
                        // Прямой вызов обработчика
                        showSortFilterBottomSheet();
                    });
                    if (filterIndicatorDot == null && logger != null) {
                        logger.warn("ChallengesFragment_MenuProvider", "filter_indicator_dot_view not found within ActionView.");
                    }
                } else {
                    if (logger != null) logger.debug("ChallengesFragment_MenuProvider", "No ActionView for filter/sort menu item. Standard click will be used.");
                }
                updateFilterIndicatorDotVisibility(); // Обновляем индикатор
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (logger != null) logger.debug("ChallengesFragment_MenuProvider", "onMenuItemSelected: " + menuItem.getTitle() + " (ID: " + menuItem.getItemId() + ")");
                else android.util.Log.d("ChallengesFragment_MenuProvider", "onMenuItemSelected: " + menuItem.getTitle() + " (ID: " + menuItem.getItemId() + ")");

                if (menuItem.getItemId() == R.id.action_filter_sort_challenges) {
                    // Этот блок может не вызываться, если клик обработан слушателем ActionView
                    if (logger != null) logger.info("ChallengesFragment_MenuProvider", "Standard onMenuItemSelected for Filter/Sort. Showing BottomSheet...");
                    else android.util.Log.i("ChallengesFragment_MenuProvider", "Standard onMenuItemSelected for Filter/Sort. Showing BottomSheet...");
                    showSortFilterBottomSheet();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        return inflater.inflate(R.layout.fragment_challenges, container, false);
    }

    // Отдельный метод для показа BottomSheet, чтобы избежать дублирования
    private void showSortFilterBottomSheet() {
        ChallengesSortFilterBottomSheetFragment.newInstance()
                .show(getChildFragmentManager(), "ChallengesSortFilterSheet");
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        logger.debug("ChallengesFragment", "onViewCreated");
        bindViews(view);
        setupTabLayout();
        setupRecyclerView();
        setupObservers();
        setupSharedObservers();
    }

    private void bindViews(View view) {
        tabLayoutPeriods = view.findViewById(R.id.tabLayout_challenges_period);
        recyclerViewChallenges = view.findViewById(R.id.recyclerView_challenges_list);
        progressBarLoading = view.findViewById(R.id.progressBar_challenges_loading);
        emptyStateLayout = view.findViewById(R.id.layout_empty_challenges);
        errorStateViewRoot = view.findViewById(R.id.error_state_challenges_include);
        if (errorStateViewRoot != null) {
            textViewError = errorStateViewRoot.findViewById(R.id.textView_error_message_stats);
            buttonRetryError = errorStateViewRoot.findViewById(R.id.button_retry_stats);
            if(buttonRetryError != null) {
                buttonRetryError.setOnClickListener(v -> {
                    logger.debug("ChallengesFragment", "Retry button clicked.");
                    challengesViewModel.retryLoad();
                });
            }
        }
        logger.debug("ChallengesFragment", "Views bound.");
    }

    private void setupTabLayout() {
        tabLayoutPeriods.addTab(tabLayoutPeriods.newTab().setText("Все").setTag(null));
        tabLayoutPeriods.addTab(tabLayoutPeriods.newTab().setText("Дневные").setTag(ChallengePeriod.DAILY));
        tabLayoutPeriods.addTab(tabLayoutPeriods.newTab().setText("Недельные").setTag(ChallengePeriod.WEEKLY));
        tabLayoutPeriods.addTab(tabLayoutPeriods.newTab().setText("Месячные").setTag(ChallengePeriod.MONTHLY));

        tabLayoutPeriods.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                logger.debug("ChallengesFragment", "Tab selected: " + tab.getText());
                challengesViewModel.selectPeriod((ChallengePeriod) tab.getTag());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        logger.debug("ChallengesFragment", "TabLayout setup done.");
    }

    private void setupRecyclerView() {
        challengesAdapter = new ChallengesAdapter(this, requireContext());
        recyclerViewChallenges.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewChallenges.setAdapter(challengesAdapter);
        logger.debug("ChallengesFragment", "RecyclerView setup done.");
    }

    private void setupObservers() {
        logger.debug("ChallengesFragment", "Setting up observers for ChallengesViewModel.");
        challengesViewModel.uiStateLiveData.observe(getViewLifecycleOwner(), uiState -> {
            if (uiState == null) {
                logger.warn("ChallengesFragment", "ChallengesScreenUiState is null.");
                return;
            }
            logger.debug("ChallengesFragment", "Observed ChallengesScreenUiState: isLoading=" + uiState.isLoading() + ", error=" + uiState.getError() + ", challengesCount=" + uiState.getChallenges().size());

            progressBarLoading.setVisibility(uiState.isLoading() ? View.VISIBLE : View.GONE);
            errorStateViewRoot.setVisibility(uiState.getError() != null && !uiState.isLoading() ? View.VISIBLE : View.GONE);
            tabLayoutPeriods.setVisibility(!uiState.isLoading() && uiState.getError() == null ? View.VISIBLE : View.GONE);
            recyclerViewChallenges.setVisibility(!uiState.isLoading() && uiState.getError() == null && !uiState.getChallenges().isEmpty() ? View.VISIBLE : View.GONE);
            emptyStateLayout.setVisibility(!uiState.isLoading() && uiState.getError() == null && uiState.getChallenges().isEmpty() ? View.VISIBLE : View.GONE);

            if (uiState.getError() != null && textViewError != null) {
                textViewError.setText(uiState.getError());
            }

            if (!uiState.isLoading() && uiState.getError() == null) {
                challengesAdapter.submitList(uiState.getChallenges());
                ChallengePeriod selectedPeriod = uiState.getSelectedPeriod();
                for (int i = 0; i < tabLayoutPeriods.getTabCount(); i++) {
                    TabLayout.Tab tab = tabLayoutPeriods.getTabAt(i);
                    if (tab != null && Objects.equals(tab.getTag(), selectedPeriod)) {
                        if (tab.getPosition() != tabLayoutPeriods.getSelectedTabPosition()) {
                            tab.select();
                        }
                        break;
                    }
                }
            }
            updateFilterIndicatorDotVisibility();
        });
    }

    private void setupSharedObservers() {
        if (gamificationViewModelShared == null) {
            logger.error("ChallengesFragment", "setupSharedObservers: gamificationViewModelShared is null. Cannot observe details.");
            return;
        }
        logger.debug("ChallengesFragment", "Setting up observers for shared GamificationViewModel.");
        gamificationViewModelShared.challengeToShowDetails.observe(getViewLifecycleOwner(), info -> {
            logger.debug("ChallengesFragment", "Observed shared challengeToShowDetails. Info is " + (info == null ? "null" : "NOT NULL"));
            if (info != null && !isChallengeDetailsSheetShown) {
                NavDestination currentDestination = NavHostFragment.findNavController(this).getCurrentDestination();
                if (currentDestination != null && currentDestination.getId() == R.id.challengesFragment) {
                    logger.info("ChallengesFragment", "Showing ChallengeDetailsBottomSheetFragment for challenge ID: " + info.id());
                    ChallengeDetailsBottomSheetFragment.newInstance()
                            .show(getChildFragmentManager(), "ChallengeDetailsSheetAllChallenges");
                    isChallengeDetailsSheetShown = true;
                } else {
                    String destLabel = currentDestination != null && currentDestination.getLabel() != null ? currentDestination.getLabel().toString() : "unknown";
                    int destId = currentDestination != null ? currentDestination.getId() : 0;
                    logger.warn("ChallengesFragment", "Attempted to show details sheet, but not on ChallengesFragment. Current dest: " + destLabel + " (ID: " + destId + ")");
                }
            } else if (info == null && isChallengeDetailsSheetShown) {
                logger.debug("ChallengesFragment", "Info is null, dismissing details sheet if visible.");
                Fragment existingSheet = getChildFragmentManager().findFragmentByTag("ChallengeDetailsSheetAllChallenges");
                if (existingSheet instanceof ChallengeDetailsBottomSheetFragment && existingSheet.isVisible()) {
                    ((ChallengeDetailsBottomSheetFragment) existingSheet).dismissAllowingStateLoss();
                }
                isChallengeDetailsSheetShown = false;
            }
        });
    }

    private void updateFilterIndicatorDotVisibility() {
        if (filterIndicatorDot == null || challengesViewModel == null) return;
        ChallengesScreenUiState uiState = challengesViewModel.uiStateLiveData.getValue();
        boolean hasActiveFiltersOrSort = uiState != null &&
                (uiState.getSortOption() != ChallengeSortOption.DEADLINE_ASC ||
                        (uiState.getFilterOptions() != null && !uiState.getFilterOptions().equals(Collections.singleton(ChallengeFilterOption.ACTIVE))));
        filterIndicatorDot.setVisibility(hasActiveFiltersOrSort ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void setupToolbar() {
        MaterialToolbar toolbar = getToolbar();
        if (toolbar != null) {
            toolbar.setTitle("Испытания");
            toolbar.setNavigationIcon(R.drawable.arrow_back_ios_new);
            toolbar.setNavigationOnClickListener(v -> {
                logger.debug("ChallengesFragment", "Toolbar navigation clicked (Back).");
                NavHostFragment.findNavController(this).popBackStack();
            });
        } else {
            logger.warn("ChallengesFragment", "Toolbar is null in setupToolbar.");
        }
    }

    @Override
    protected void setupFab() {
        if (getStandardFab() != null) getStandardFab().hide();
        if (getExtendedFab() != null) getExtendedFab().hide();
    }

    @Override
    public void onChallengeItemClicked(ChallengeProgressFullDetails challengeDetails) {
        logger.debug("ChallengesFragment", "onChallengeItemClicked: ID " + challengeDetails.getChallengeAndReward().getChallenge().getId());
        if (gamificationViewModelShared != null) {
            gamificationViewModelShared.showChallengeDetailsFromFullDetails(challengeDetails);
        } else {
            logger.error("ChallengesFragment", "gamificationViewModelShared is null in onChallengeItemClicked. Cannot show details.");
        }
    }

    public void onDetailsSheetDismissed() {
        logger.debug("ChallengesFragment", "onDetailsSheetDismissed callback received.");
        isChallengeDetailsSheetShown = false;
        if (gamificationViewModelShared != null) {
            gamificationViewModelShared.clearChallengeDetails(); // Убедимся, что данные в общей VM очищены
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        logger.debug("ChallengesFragment", "onDestroyView");
        tabLayoutPeriods = null;
        recyclerViewChallenges = null;
        challengesAdapter = null;
        progressBarLoading = null;
        emptyStateLayout = null;
        textViewError = null;
        errorStateViewRoot = null;
        buttonRetryError = null;
        filterSortMenuItem = null;
        filterIndicatorDot = null;
    }
}