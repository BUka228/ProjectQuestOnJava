package com.example.projectquestonjava.approach.calendar.presentation.ui_parts;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskFilterOption;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskSortOption;
import com.example.projectquestonjava.approach.calendar.presentation.viewmodels.CalendarDashboardViewModel;
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

public class SortFilterBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private CalendarDashboardViewModel viewModel;
    private MaterialButtonToggleGroup toggleSortTime, toggleSortCreated;
    private ChipGroup chipGroupFilters;
    private Map<TaskFilterOption, Chip> filterChipMap = new HashMap<>();

    // Флаги для предотвращения циклов
    private boolean isUpdatingSortFromVm = false;
    private boolean isUpdatingFiltersFromVm = false;

    public static SortFilterBottomSheetDialogFragment newInstance() {
        return new SortFilterBottomSheetDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireParentFragment()).get(CalendarDashboardViewModel.class);
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
        return inflater.inflate(R.layout.bottom_sheet_dashboard_sort_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        toggleSortTime = view.findViewById(R.id.toggle_sort_time_dashboard);
        toggleSortCreated = view.findViewById(R.id.toggle_sort_created_dashboard);
        chipGroupFilters = view.findViewById(R.id.chipGroup_filters_dashboard);

        filterChipMap.put(TaskFilterOption.ALL, view.findViewById(R.id.chip_filter_all_dashboard));
        filterChipMap.put(TaskFilterOption.INCOMPLETE, view.findViewById(R.id.chip_filter_incomplete_dashboard));
        filterChipMap.put(TaskFilterOption.COMPLETE, view.findViewById(R.id.chip_filter_complete_dashboard));
        filterChipMap.put(TaskFilterOption.HIGH_PRIORITY, view.findViewById(R.id.chip_filter_high_priority_dashboard));

        view.findViewById(R.id.button_reset_sort_filter_dashboard).setOnClickListener(v -> {
            viewModel.resetSortAndFilters();
        });
        view.findViewById(R.id.button_apply_sort_filter_dashboard).setOnClickListener(v -> dismiss());

        setupSortControls();
        setupFilterChips();

        viewModel.sortOptionLiveData.observe(getViewLifecycleOwner(), selectedSort -> {
            if (selectedSort != null) {
                isUpdatingSortFromVm = true; // Устанавливаем флаг перед обновлением UI
                updateSortSelectionInDialog(selectedSort);
                isUpdatingSortFromVm = false; // Сбрасываем флаг
            }
        });
        viewModel.filterOptionsLiveData.observe(getViewLifecycleOwner(), selectedFilters -> {
            if (selectedFilters != null) {
                isUpdatingFiltersFromVm = true; // Устанавливаем флаг
                updateFilterSelectionInDialog(selectedFilters);
                isUpdatingFiltersFromVm = false; // Сбрасываем флаг
            }
        });
    }

    private void setupSortControls() {
        toggleSortTime.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked && !isUpdatingSortFromVm) { // Проверяем флаг
                uncheckOtherSortGroups(group);
                if (checkedId == R.id.button_sort_time_asc_dashboard) viewModel.updateSortOption(TaskSortOption.TIME_ASC);
                else if (checkedId == R.id.button_sort_time_desc_dashboard) viewModel.updateSortOption(TaskSortOption.TIME_DESC);
            }
        });
        toggleSortCreated.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked && !isUpdatingSortFromVm) { // Проверяем флаг
                uncheckOtherSortGroups(group);
                if (checkedId == R.id.button_sort_created_newest_dashboard) viewModel.updateSortOption(TaskSortOption.CREATED_NEWEST);
                else if (checkedId == R.id.button_sort_created_oldest_dashboard) viewModel.updateSortOption(TaskSortOption.CREATED_OLDEST);
            }
        });
    }

    private void uncheckOtherSortGroups(MaterialButtonToggleGroup currentGroup) {
        // Эта логика не меняет состояние ViewModel, только UI, поэтому флаг isUpdatingSortFromVm здесь не нужен
        if (currentGroup != toggleSortTime) toggleSortTime.clearChecked();
        if (currentGroup != toggleSortCreated) toggleSortCreated.clearChecked();
    }

    private void setupFilterChips() {
        for (Map.Entry<TaskFilterOption, Chip> entry : filterChipMap.entrySet()) {
            entry.getValue().setOnClickListener(v -> {
                if (!isUpdatingFiltersFromVm) { // Проверяем флаг
                    viewModel.toggleFilterOption(entry.getKey());
                }
            });
            // Настройка внешнего вида чипов (checkedIcon и т.д.)
            Chip chip = entry.getValue();
            chip.setEnsureMinTouchTargetSize(false);
            chip.setChipIconVisible(false); // Если у вас нет иконок по умолчанию
            chip.setCheckedIconVisible(true); // Показываем галочку при выборе
        }
    }

    private void updateSortSelectionInDialog(TaskSortOption selectedSort) {
        // Эта функция только обновляет UI, не вызывая viewModel
        uncheckOtherSortGroups(null);
        switch (selectedSort) {
            case TIME_ASC: toggleSortTime.check(R.id.button_sort_time_asc_dashboard); break;
            case TIME_DESC: toggleSortTime.check(R.id.button_sort_time_desc_dashboard); break;
            case CREATED_NEWEST: toggleSortCreated.check(R.id.button_sort_created_newest_dashboard); break;
            case CREATED_OLDEST: toggleSortCreated.check(R.id.button_sort_created_oldest_dashboard); break;
        }
    }

    private void updateFilterSelectionInDialog(Set<TaskFilterOption> selectedFilters) {
        // Эта функция только обновляет UI
        boolean isAllSelected = selectedFilters.contains(TaskFilterOption.ALL);
        for (Map.Entry<TaskFilterOption, Chip> entry : filterChipMap.entrySet()) {
            Chip chip = entry.getValue();
            TaskFilterOption option = entry.getKey();
            chip.setChecked(selectedFilters.contains(option));
            if (option != TaskFilterOption.ALL) {
                chip.setEnabled(!isAllSelected);
            }
        }
    }
}