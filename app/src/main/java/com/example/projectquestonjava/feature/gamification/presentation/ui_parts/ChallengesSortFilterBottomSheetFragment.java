package com.example.projectquestonjava.feature.gamification.presentation.ui_parts; // Уточни пакет

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // Стандартная кнопка
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.example.projectquestonjava.R;
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
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChallengesSortFilterBottomSheetFragment extends BottomSheetDialogFragment {

    private ChallengesViewModel viewModel;

    // Элементы UI для сортировки
    private MaterialButtonToggleGroup toggleSortDeadline, toggleSortProgress, toggleSortReward, toggleSortName;

    // Элементы UI для фильтрации
    private ChipGroup chipGroupFiltersStatus, chipGroupFiltersRewardType; // Разделим фильтры на группы
    private Map<ChallengeFilterOption, Chip> filterChipMap = new HashMap<>();

    public static ChallengesSortFilterBottomSheetFragment newInstance() {
        return new ChallengesSortFilterBottomSheetFragment();
    }

    public ChallengesSortFilterBottomSheetFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Получаем ViewModel от родительского фрагмента (ChallengesFragment)
        viewModel = new ViewModelProvider(requireParentFragment()).get(ChallengesViewModel.class);
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

        // Сортировка
        toggleSortDeadline = view.findViewById(R.id.toggle_sort_deadline_challenges);
        toggleSortProgress = view.findViewById(R.id.toggle_sort_progress_challenges);
        toggleSortReward = view.findViewById(R.id.toggle_sort_reward_challenges);
        toggleSortName = view.findViewById(R.id.toggle_sort_name_challenges);

        // Фильтры
        chipGroupFiltersStatus = view.findViewById(R.id.chipGroup_filters_status_challenges);
        chipGroupFiltersRewardType = view.findViewById(R.id.chipGroup_filters_reward_type_challenges);

        // Сопоставление Chip ID с ChallengeFilterOption
        // Статус
        filterChipMap.put(ChallengeFilterOption.ALL, view.findViewById(R.id.chip_filter_all_challenges));
        filterChipMap.put(ChallengeFilterOption.ACTIVE, view.findViewById(R.id.chip_filter_active_challenges));
        filterChipMap.put(ChallengeFilterOption.COMPLETED, view.findViewById(R.id.chip_filter_completed_challenges));
        filterChipMap.put(ChallengeFilterOption.EXPIRED, view.findViewById(R.id.chip_filter_expired_challenges));
        filterChipMap.put(ChallengeFilterOption.MISSED, view.findViewById(R.id.chip_filter_missed_challenges));
        filterChipMap.put(ChallengeFilterOption.URGENT, view.findViewById(R.id.chip_filter_urgent_challenges));
        // Тип награды
        filterChipMap.put(ChallengeFilterOption.HAS_COIN_REWARD, view.findViewById(R.id.chip_filter_reward_coin_challenges));
        filterChipMap.put(ChallengeFilterOption.HAS_XP_REWARD, view.findViewById(R.id.chip_filter_reward_xp_challenges));
        filterChipMap.put(ChallengeFilterOption.HAS_BADGE_REWARD, view.findViewById(R.id.chip_filter_reward_badge_challenges));
        // HIGH_REWARD не представлен как отдельный чип, он может быть логикой внутри ViewModel

        view.findViewById(R.id.button_reset_filters_challenges).setOnClickListener(v -> viewModel.resetFiltersAndSort());
        view.findViewById(R.id.button_apply_filters_challenges).setOnClickListener(v -> dismiss());

        setupSortControls();
        setupFilterChipsListeners(); // Настраиваем слушатели для чипов

        // Наблюдение за LiveData из ViewModel
        viewModel.uiStateLiveData.observe(getViewLifecycleOwner(), uiState -> {
            if (uiState != null) {
                updateSortSelectionInDialog(uiState.getSortOption());
                updateFilterSelectionInDialog(uiState.getFilterOptions());
            }
        });
    }

    private void setupSortControls() {
        // Срок
        toggleSortDeadline.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                uncheckOtherSortGroups(group);
                if (checkedId == R.id.button_sort_deadline_asc_challenges) viewModel.updateSortOption(ChallengeSortOption.DEADLINE_ASC);
                else if (checkedId == R.id.button_sort_deadline_desc_challenges) viewModel.updateSortOption(ChallengeSortOption.DEADLINE_DESC);
            }
        });
        // Прогресс
        toggleSortProgress.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                uncheckOtherSortGroups(group);
                if (checkedId == R.id.button_sort_progress_desc_challenges) viewModel.updateSortOption(ChallengeSortOption.PROGRESS_DESC);
                else if (checkedId == R.id.button_sort_progress_asc_challenges) viewModel.updateSortOption(ChallengeSortOption.PROGRESS_ASC);
            }
        });
        // Награда
        toggleSortReward.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                uncheckOtherSortGroups(group);
                if (checkedId == R.id.button_sort_reward_desc_challenges) viewModel.updateSortOption(ChallengeSortOption.REWARD_VALUE_DESC);
                else if (checkedId == R.id.button_sort_reward_asc_challenges) viewModel.updateSortOption(ChallengeSortOption.REWARD_VALUE_ASC);
            }
        });
        // Имя
        toggleSortName.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                uncheckOtherSortGroups(group);
                if (checkedId == R.id.button_sort_name_asc_challenges) viewModel.updateSortOption(ChallengeSortOption.NAME_ASC);
                else if (checkedId == R.id.button_sort_name_desc_challenges) viewModel.updateSortOption(ChallengeSortOption.NAME_DESC);
            }
        });
    }

    private void uncheckOtherSortGroups(@Nullable MaterialButtonToggleGroup currentActiveGroup) {
        if (currentActiveGroup != toggleSortDeadline) toggleSortDeadline.clearChecked();
        if (currentActiveGroup != toggleSortProgress) toggleSortProgress.clearChecked();
        if (currentActiveGroup != toggleSortReward) toggleSortReward.clearChecked();
        if (currentActiveGroup != toggleSortName) toggleSortName.clearChecked();
    }

    private void setupFilterChipsListeners() {
        for (Map.Entry<ChallengeFilterOption, Chip> entry : filterChipMap.entrySet()) {
            Chip chip = entry.getValue();
            ChallengeFilterOption option = entry.getKey();
            if (chip != null) { // Добавлена проверка на null
                chip.setOnClickListener(v -> viewModel.toggleFilterOption(option));
                chip.setEnsureMinTouchTargetSize(false);
                chip.setChipIconVisible(false); // Скрываем иконку по умолчанию
                chip.setCheckedIconVisible(true); // Показываем галочку при выборе
            }
        }
    }

    private void updateSortSelectionInDialog(ChallengeSortOption selectedSort) {
        if (selectedSort == null) return;
        // Сначала сбрасываем выбор во всех группах, кроме той, которая соответствует selectedSort
        // Это предотвратит одновременный выбор в нескольких группах, если selectedSort изменился извне
        if (selectedSort != ChallengeSortOption.DEADLINE_ASC && selectedSort != ChallengeSortOption.DEADLINE_DESC) toggleSortDeadline.clearChecked();
        if (selectedSort != ChallengeSortOption.PROGRESS_ASC && selectedSort != ChallengeSortOption.PROGRESS_DESC) toggleSortProgress.clearChecked();
        if (selectedSort != ChallengeSortOption.REWARD_VALUE_ASC && selectedSort != ChallengeSortOption.REWARD_VALUE_DESC) toggleSortReward.clearChecked();
        if (selectedSort != ChallengeSortOption.NAME_ASC && selectedSort != ChallengeSortOption.NAME_DESC) toggleSortName.clearChecked();


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
        boolean isAllSelected = selectedFilters.contains(ChallengeFilterOption.ALL);
        for (Map.Entry<ChallengeFilterOption, Chip> entry : filterChipMap.entrySet()) {
            Chip chip = entry.getValue();
            ChallengeFilterOption option = entry.getKey();
            if (chip != null) { // Добавлена проверка на null
                chip.setChecked(selectedFilters.contains(option));
                if (option != ChallengeFilterOption.ALL) {
                    chip.setEnabled(!isAllSelected);
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Очистка ссылок
        toggleSortDeadline = null;
        toggleSortProgress = null;
        toggleSortReward = null;
        toggleSortName = null;
        chipGroupFiltersStatus = null;
        chipGroupFiltersRewardType = null;
        filterChipMap.clear();
    }
}