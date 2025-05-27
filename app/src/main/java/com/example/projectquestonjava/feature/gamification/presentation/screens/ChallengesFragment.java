package com.example.projectquestonjava.feature.gamification.presentation.screens; // Уточни пакет

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout; // Для индикатора фильтров
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment; // Убедись, что это androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.core.ui.BaseFragment;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengePeriod;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeProgressFullDetails;
import com.example.projectquestonjava.feature.gamification.presentation.adapters.ChallengesAdapter;
import com.example.projectquestonjava.feature.gamification.presentation.ui_parts.ChallengeDetailsBottomSheetFragment;
import com.example.projectquestonjava.feature.gamification.presentation.ui_parts.ChallengesSortFilterBottomSheetFragment;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.ChallengeFilterOption;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.ChallengeSortOption;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.ChallengesScreenUiState;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.ChallengesViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.snackbar.Snackbar; // Для сообщений об ошибках

import java.util.Collections;
import java.util.Objects;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChallengesFragment extends BaseFragment implements ChallengesAdapter.OnChallengeItemClickListener {

    private ChallengesViewModel viewModel;
    private RecyclerView recyclerViewChallenges;
    private ChallengesAdapter challengesAdapter;
    private TabLayout tabLayoutPeriods;
    private ProgressBar progressBarLoading;
    private LinearLayout emptyStateLayout;
    private TextView textViewError;
    private View errorStateView; // Контейнер для состояния ошибки
    private Button buttonRetryError;

    private MenuItem filterSortMenuItem;
    private FrameLayout filterIndicatorDot; // View для точки-индикатора

    private boolean isChallengeDetailsSheetShown = false;


    public ChallengesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.challenges_toolbar_menu, menu); // Создать challenges_toolbar_menu.xml
                filterSortMenuItem = menu.findItem(R.id.action_filter_sort_challenges);
                // Настраиваем ActionView для точки-индикатора
                if (filterSortMenuItem != null) {
                    filterSortMenuItem.setActionView(R.layout.menu_item_filter_indicator); // Создать этот макет
                    View actionView = filterSortMenuItem.getActionView();
                    if (actionView != null) {
                        filterIndicatorDot = actionView.findViewById(R.id.filter_indicator_dot_view);
                        actionView.setOnClickListener(v -> onOptionsItemSelected(filterSortMenuItem)); // Делаем весь ActionView кликабельным
                    }
                }
                updateFilterIndicatorDotVisibility();
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_filter_sort_challenges) {
                    ChallengesSortFilterBottomSheetFragment.newInstance()
                            .show(getChildFragmentManager(), "ChallengesSortFilterSheet");
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        return inflater.inflate(R.layout.fragment_challenges, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ChallengesViewModel.class);

        bindViews(view);
        setupTabLayout();
        setupRecyclerView();
        setupObservers();
    }

    private void bindViews(View view) {
        tabLayoutPeriods = view.findViewById(R.id.tabLayout_challenges_period);
        recyclerViewChallenges = view.findViewById(R.id.recyclerView_challenges_list);
        progressBarLoading = view.findViewById(R.id.progressBar_challenges_loading);
        emptyStateLayout = view.findViewById(R.id.layout_empty_challenges);
        errorStateView = view.findViewById(R.id.error_state_challenges_include);
        if (errorStateView != null) {
            textViewError = errorStateView.findViewById(R.id.textView_error_message_stats); // Используем ID из макета ошибки
            buttonRetryError = errorStateView.findViewById(R.id.button_retry_stats);
            buttonRetryError.setOnClickListener(v -> viewModel.retryLoad()); // Метод для повторной загрузки
        }
    }

    private void setupTabLayout() {
        // Добавляем вкладки
        // "Все" соответствует null в ViewModel
        tabLayoutPeriods.addTab(tabLayoutPeriods.newTab().setText("Все").setTag(null));
        tabLayoutPeriods.addTab(tabLayoutPeriods.newTab().setText("Дневные").setTag(ChallengePeriod.DAILY));
        tabLayoutPeriods.addTab(tabLayoutPeriods.newTab().setText("Недельные").setTag(ChallengePeriod.WEEKLY));
        tabLayoutPeriods.addTab(tabLayoutPeriods.newTab().setText("Месячные").setTag(ChallengePeriod.MONTHLY));
        // ONCE и EVENT обычно не выбираются через табы, а фильтруются иначе

        tabLayoutPeriods.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewModel.selectPeriod((ChallengePeriod) tab.getTag());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        challengesAdapter = new ChallengesAdapter(this, requireContext()); // Передаем this и context
        recyclerViewChallenges.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewChallenges.setAdapter(challengesAdapter);
    }

    private void setupObservers() {
        viewModel.uiStateLiveData.observe(getViewLifecycleOwner(), uiState -> {
            if (uiState == null) return;

            progressBarLoading.setVisibility(uiState.isLoading() ? View.VISIBLE : View.GONE);
            errorStateView.setVisibility(uiState.getError() != null && !uiState.isLoading() ? View.VISIBLE : View.GONE);
            tabLayoutPeriods.setVisibility(!uiState.isLoading() && uiState.getError() == null ? View.VISIBLE : View.GONE);
            recyclerViewChallenges.setVisibility(!uiState.isLoading() && uiState.getError() == null && !uiState.getChallenges().isEmpty() ? View.VISIBLE : View.GONE);
            emptyStateLayout.setVisibility(!uiState.isLoading() && uiState.getError() == null && uiState.getChallenges().isEmpty() ? View.VISIBLE : View.GONE);


            if (uiState.getError() != null && textViewError != null) {
                textViewError.setText(uiState.getError());
            }

            if (!uiState.isLoading() && uiState.getError() == null) {
                challengesAdapter.submitList(uiState.getChallenges());

                // Обновление выбранной вкладки в TabLayout
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
            updateFilterIndicatorDotVisibility(); // Обновляем точку при изменении state
        });

        // Для показа деталей челленджа (используем общую ViewModel от родителя, если так задумано)
        // Если ChallengesViewModel управляет этим сама:
        viewModel.getChallengeToShowDetails().observe(getViewLifecycleOwner(), info -> {
            if (info != null && !isChallengeDetailsSheetShown) {
                ChallengeDetailsBottomSheetFragment.newInstance()
                        .show(getChildFragmentManager(), "ChallengeDetailsSheetAllChallenges");
                isChallengeDetailsSheetShown = true;
            } else if (info == null && isChallengeDetailsSheetShown) {
                Fragment existingSheet = getChildFragmentManager().findFragmentByTag("ChallengeDetailsSheetAllChallenges");
                if (existingSheet instanceof ChallengeDetailsBottomSheetFragment && existingSheet.isVisible()) {
                    ((ChallengeDetailsBottomSheetFragment) existingSheet).dismissAllowingStateLoss();
                }
                isChallengeDetailsSheetShown = false;
            }
        });
    }

    private void updateFilterIndicatorDotVisibility() {
        if (filterIndicatorDot == null) return;
        ChallengesScreenUiState uiState = viewModel.uiStateLiveData.getValue();
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
            toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
            // MenuProvider добавляется в onCreateView
        }
    }

    @Override
    protected void setupFab() {
        if (getStandardFab() != null) getStandardFab().hide();
        if (getExtendedFab() != null) getExtendedFab().hide();
    }

    // --- Реализация ChallengesAdapter.OnChallengeItemClickListener ---
    @Override
    public void onChallengeItemClicked(ChallengeProgressFullDetails challengeDetails) {
        // ViewModel должна будет преобразовать ChallengeProgressFullDetails в ChallengeCardInfo
        // или ChallengeDetailsBottomSheetFragment должен уметь работать с ChallengeProgressFullDetails
        viewModel.showChallengeDetailsFromFullDetails(challengeDetails); // Предполагаем такой метод в ViewModel
    }

    // Вызывается из ChallengeDetailsBottomSheetFragment
    public void onDetailsSheetDismissed() {
        isChallengeDetailsSheetShown = false;
        viewModel.clearChallengeDetails();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        tabLayoutPeriods = null;
        recyclerViewChallenges = null;
        challengesAdapter = null;
        progressBarLoading = null;
        emptyStateLayout = null;
        textViewError = null;
        errorStateView = null;
        buttonRetryError = null;
        filterSortMenuItem = null;
        filterIndicatorDot = null;
    }
}