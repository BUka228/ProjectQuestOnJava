package com.example.projectquestonjava.feature.gamification.presentation.ui_parts;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.core.utils.Logger; // Добавим импорт
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.ChallengeFilterOption;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.ChallengeSortOption;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.ChallengesViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject; // Для логгера
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChallengesSortFilterBottomSheetFragment extends BottomSheetDialogFragment {

    private ChallengesViewModel viewModel;
    private Logger logger; // Для логгирования

    private MaterialButtonToggleGroup toggleSortDeadline, toggleSortProgress, toggleSortReward, toggleSortName;
    private ChipGroup chipGroupFiltersStatus, chipGroupFiltersRewardType;
    private Map<ChallengeFilterOption, Chip> filterChipMap = new HashMap<>();

    private boolean isUpdatingSortUiFromVm = false; // Флаг для предотвращения циклов сортировки
    private boolean isUpdatingFilterUiFromVm = false; // Флаг для предотвращения циклов фильтров


    public static ChallengesSortFilterBottomSheetFragment newInstance() {
        return new ChallengesSortFilterBottomSheetFragment();
    }

    public ChallengesSortFilterBottomSheetFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireParentFragment()).get(ChallengesViewModel.class);
        logger = viewModel.getLogger(); // Получаем логгер из ViewModel
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            BottomSheetDialog dInternal = (BottomSheetDialog) d;
            View bottomSheetInternal = dInternal.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheetInternal != null) {
                BottomSheetBehavior.from(bottomSheetInternal).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_challenges_sort_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupSortControls();
        setupFilterChipsListeners();

        view.findViewById(R.id.button_reset_filters_challenges).setOnClickListener(v -> {
            logger.debug("ChallengesSortFilterBS", "Reset button clicked.");
            viewModel.resetFiltersAndSort();
        });
        view.findViewById(R.id.button_apply_filters_challenges).setOnClickListener(v -> dismiss());

        viewModel.uiStateLiveData.observe(getViewLifecycleOwner(), uiState -> {
            if (uiState != null) {
                logger.debug("ChallengesSortFilterBS", "Observed UIState. Sort: " + uiState.getSortOption() + ", Filters: " + uiState.getFilterOptions());
                isUpdatingSortUiFromVm = true;
                updateSortSelectionInDialog(uiState.getSortOption());
                isUpdatingSortUiFromVm = false;

                isUpdatingFilterUiFromVm = true;
                updateFilterSelectionInDialog(uiState.getFilterOptions());
                isUpdatingFilterUiFromVm = false;
            }
        });
    }

    private void bindViews(View view) {
        toggleSortDeadline = view.findViewById(R.id.toggle_sort_deadline_challenges);
        toggleSortProgress = view.findViewById(R.id.toggle_sort_progress_challenges);
        toggleSortReward = view.findViewById(R.id.toggle_sort_reward_challenges);
        toggleSortName = view.findViewById(R.id.toggle_sort_name_challenges);
        chipGroupFiltersStatus = view.findViewById(R.id.chipGroup_filters_status_challenges);
        chipGroupFiltersRewardType = view.findViewById(R.id.chipGroup_filters_reward_type_challenges);
        filterChipMap.put(ChallengeFilterOption.ALL, view.findViewById(R.id.chip_filter_all_challenges));
        filterChipMap.put(ChallengeFilterOption.ACTIVE, view.findViewById(R.id.chip_filter_active_challenges));
        filterChipMap.put(ChallengeFilterOption.COMPLETED, view.findViewById(R.id.chip_filter_completed_challenges));
        filterChipMap.put(ChallengeFilterOption.EXPIRED, view.findViewById(R.id.chip_filter_expired_challenges));
        filterChipMap.put(ChallengeFilterOption.MISSED, view.findViewById(R.id.chip_filter_missed_challenges));
        filterChipMap.put(ChallengeFilterOption.URGENT, view.findViewById(R.id.chip_filter_urgent_challenges));
        filterChipMap.put(ChallengeFilterOption.HAS_COIN_REWARD, view.findViewById(R.id.chip_filter_reward_coin_challenges));
        filterChipMap.put(ChallengeFilterOption.HAS_XP_REWARD, view.findViewById(R.id.chip_filter_reward_xp_challenges));
        filterChipMap.put(ChallengeFilterOption.HAS_BADGE_REWARD, view.findViewById(R.id.chip_filter_reward_badge_challenges));
    }

    private void setupSortControls() {
        MaterialButtonToggleGroup.OnButtonCheckedListener listener = (group, checkedId, isChecked) -> {
            if (isChecked && !isUpdatingSortUiFromVm) { // Проверяем флаг
                uncheckOtherSortGroups(group); // Снимаем выбор с других групп
                ChallengeSortOption selectedOption = null;
                if (checkedId == R.id.button_sort_deadline_asc_challenges) selectedOption = ChallengeSortOption.DEADLINE_ASC;
                else if (checkedId == R.id.button_sort_deadline_desc_challenges) selectedOption = ChallengeSortOption.DEADLINE_DESC;
                else if (checkedId == R.id.button_sort_progress_desc_challenges) selectedOption = ChallengeSortOption.PROGRESS_DESC;
                else if (checkedId == R.id.button_sort_progress_asc_challenges) selectedOption = ChallengeSortOption.PROGRESS_ASC;
                else if (checkedId == R.id.button_sort_reward_desc_challenges) selectedOption = ChallengeSortOption.REWARD_VALUE_DESC;
                else if (checkedId == R.id.button_sort_reward_asc_challenges) selectedOption = ChallengeSortOption.REWARD_VALUE_ASC;
                else if (checkedId == R.id.button_sort_name_asc_challenges) selectedOption = ChallengeSortOption.NAME_ASC;
                else if (checkedId == R.id.button_sort_name_desc_challenges) selectedOption = ChallengeSortOption.NAME_DESC;

                if (selectedOption != null) {
                    logger.debug("ChallengesSortFilterBS", "User selected sort: " + selectedOption);
                    viewModel.updateSortOption(selectedOption);
                }
            }
        };
        toggleSortDeadline.addOnButtonCheckedListener(listener);
        toggleSortProgress.addOnButtonCheckedListener(listener);
        toggleSortReward.addOnButtonCheckedListener(listener);
        toggleSortName.addOnButtonCheckedListener(listener);
    }

    private void uncheckOtherSortGroups(@Nullable MaterialButtonToggleGroup currentActiveGroup) {
        // Эта функция должна вызываться только при пользовательском действии
        if (currentActiveGroup != toggleSortDeadline) toggleSortDeadline.clearChecked();
        if (currentActiveGroup != toggleSortProgress) toggleSortProgress.clearChecked();
        if (currentActiveGroup != toggleSortReward) toggleSortReward.clearChecked();
        if (currentActiveGroup != toggleSortName) toggleSortName.clearChecked();
    }

    private void setupFilterChipsListeners() {
        for (Map.Entry<ChallengeFilterOption, Chip> entry : filterChipMap.entrySet()) {
            Chip chip = entry.getValue();
            ChallengeFilterOption option = entry.getKey();
            if (chip != null) {
                chip.setOnClickListener(v -> {
                    if (!isUpdatingFilterUiFromVm) { // Проверяем флаг
                        logger.debug("ChallengesSortFilterBS", "User toggled filter: " + option);
                        viewModel.toggleFilterOption(option);
                    }
                });
                chip.setEnsureMinTouchTargetSize(false);
                chip.setChipIconVisible(false);
                chip.setCheckedIconVisible(true);
            }
        }
    }

    private void updateSortSelectionInDialog(ChallengeSortOption selectedSort) {
        if (selectedSort == null) return;
        logger.debug("ChallengesSortFilterBS", "Updating sort UI to: " + selectedSort);

        // Сначала очищаем все, чтобы избежать множественного выбора при программном обновлении
        toggleSortDeadline.clearChecked();
        toggleSortProgress.clearChecked();
        toggleSortReward.clearChecked();
        toggleSortName.clearChecked();

        // Затем выбираем нужную кнопку
        switch (selectedSort) {
            case DEADLINE_ASC: toggleSortDeadline.check(R.id.button_sort_deadline_asc_challenges); break;
            case DEADLINE_DESC: toggleSortDeadline.check(R.id.button_sort_deadline_desc_challenges); break;
            case PROGRESS_DESC: toggleSortProgress.check(R.id.button_sort_progress_desc_challenges); break;
            case PROGRESS_ASC: toggleSortProgress.check(R.id.button_sort_progress_asc_challenges); break;
            case REWARD_VALUE_DESC: toggleSortReward.check(R.id.button_sort_reward_desc_challenges); break;
            case REWARD_VALUE_ASC: toggleSortReward.check(R.id.button_sort_reward_asc_challenges); break;
            case NAME_ASC: toggleSortName.check(R.id.button_sort_name_asc_challenges); break;
            case NAME_DESC: toggleSortName.check(R.id.button_sort_name_desc_challenges); break;
        }
    }

    private void updateFilterSelectionInDialog(Set<ChallengeFilterOption> selectedFilters) {
        if (selectedFilters == null) return;
        logger.debug("ChallengesSortFilterBS", "Updating filter UI to: " + selectedFilters);
        boolean isAllSelected = selectedFilters.contains(ChallengeFilterOption.ALL);
        for (Map.Entry<ChallengeFilterOption, Chip> entry : filterChipMap.entrySet()) {
            Chip chip = entry.getValue();
            ChallengeFilterOption option = entry.getKey();
            if (chip != null) {
                chip.setChecked(selectedFilters.contains(option));
                // Фильтр "Все" блокирует остальные, но сам всегда активен
                chip.setEnabled(option == ChallengeFilterOption.ALL || !isAllSelected);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        toggleSortDeadline = null; toggleSortProgress = null; toggleSortReward = null; toggleSortName = null;
        chipGroupFiltersStatus = null; chipGroupFiltersRewardType = null;
        filterChipMap.clear();
        if (logger != null) logger.debug("ChallengesSortFilterBS", "onDestroyView called.");
    }
}