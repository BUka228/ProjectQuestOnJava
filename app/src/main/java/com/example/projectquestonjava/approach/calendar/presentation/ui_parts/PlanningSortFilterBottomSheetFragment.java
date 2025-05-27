package com.example.projectquestonjava.approach.calendar.presentation.ui_parts; // Убедитесь, что пакет правильный

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
import com.example.projectquestonjava.approach.calendar.domain.model.TaskFilterOption;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskSortOption;
import com.example.projectquestonjava.approach.calendar.presentation.viewmodels.CalendarPlanningViewModel; // Используем Planning ViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class PlanningSortFilterBottomSheetFragment extends BottomSheetDialogFragment {

    private CalendarPlanningViewModel viewModel; // Изменен тип ViewModel
    private MaterialButtonToggleGroup toggleSortTime, toggleSortPriority, toggleSortCreated;
    private MaterialButton buttonSortStatus; // Изменен тип на MaterialButton
    private ChipGroup chipGroupFilters;
    private Map<TaskFilterOption, Chip> filterChipMap = new HashMap<>();

    public static PlanningSortFilterBottomSheetFragment newInstance() {
        return new PlanningSortFilterBottomSheetFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireParentFragment()).get(CalendarPlanningViewModel.class);
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
        return inflater.inflate(R.layout.bottom_sheet_planning_sort_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        toggleSortTime = view.findViewById(R.id.toggle_sort_time_planning);
        toggleSortPriority = view.findViewById(R.id.toggle_sort_priority_planning);
        buttonSortStatus = view.findViewById(R.id.button_sort_status_planning);
        toggleSortCreated = view.findViewById(R.id.toggle_sort_created_planning);
        chipGroupFilters = view.findViewById(R.id.chipGroup_filters_planning);

        // Инициализация Chip-ов для фильтров (как в предыдущем)
        filterChipMap.put(TaskFilterOption.ALL, view.findViewById(R.id.chip_filter_all_planning));
        filterChipMap.put(TaskFilterOption.INCOMPLETE, view.findViewById(R.id.chip_filter_incomplete_planning));
        filterChipMap.put(TaskFilterOption.COMPLETE, view.findViewById(R.id.chip_filter_complete_planning));
        filterChipMap.put(TaskFilterOption.OVERDUE, view.findViewById(R.id.chip_filter_overdue_planning));
        filterChipMap.put(TaskFilterOption.CRITICAL_PRIORITY, view.findViewById(R.id.chip_filter_critical_planning));
        filterChipMap.put(TaskFilterOption.HIGH_PRIORITY, view.findViewById(R.id.chip_filter_high_planning));


        view.findViewById(R.id.button_reset_filters_planning).setOnClickListener(v -> {
            viewModel.resetSortAndFilters();
        });
        view.findViewById(R.id.button_apply_filters_planning).setOnClickListener(v -> dismiss());

        setupSortControls();
        setupFilterChips();

        viewModel.sortOptionLiveData.observe(getViewLifecycleOwner(), this::updateSortSelectionInDialog);
        viewModel.filterOptionsLiveData.observe(getViewLifecycleOwner(), this::updateFilterSelectionInDialog);
    }

    private void setupSortControls() {
        toggleSortTime.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                uncheckOtherSortGroups(group);
                if (checkedId == R.id.button_sort_time_asc_planning) viewModel.updateSortOption(TaskSortOption.TIME_ASC);
                else if (checkedId == R.id.button_sort_time_desc_planning) viewModel.updateSortOption(TaskSortOption.TIME_DESC);
            }
        });
        toggleSortPriority.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                uncheckOtherSortGroups(group);
                if (checkedId == R.id.button_sort_priority_desc_planning) viewModel.updateSortOption(TaskSortOption.PRIORITY_DESC);
                else if (checkedId == R.id.button_sort_priority_asc_planning) viewModel.updateSortOption(TaskSortOption.PRIORITY_ASC);
            }
        });
        buttonSortStatus.setOnClickListener(v -> {
            uncheckOtherSortGroups(null); // Передаем null, т.к. это не ToggleGroup
            viewModel.updateSortOption(TaskSortOption.STATUS);
            // Визуальное выделение кнопки статуса
            buttonSortStatus.setSelected(true); // Если MaterialButton поддерживает это
            // Или меняем стиль/фон:
            // buttonSortStatus.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primaryContainerLight));
            // buttonSortStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.onPrimaryContainerLight));
        });
        toggleSortCreated.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                uncheckOtherSortGroups(group);
                if (checkedId == R.id.button_sort_created_newest_planning) viewModel.updateSortOption(TaskSortOption.CREATED_NEWEST);
                else if (checkedId == R.id.button_sort_created_oldest_planning) viewModel.updateSortOption(TaskSortOption.CREATED_OLDEST);
            }
        });
    }

    private void uncheckOtherSortGroups(@Nullable MaterialButtonToggleGroup currentActiveGroup) {
        if (currentActiveGroup != toggleSortTime) toggleSortTime.clearChecked();
        if (currentActiveGroup != toggleSortPriority) toggleSortPriority.clearChecked();
        if (currentActiveGroup != toggleSortCreated) toggleSortCreated.clearChecked();
        // Сброс стиля/выделения для кнопки статуса, если она не была нажата
        if (currentActiveGroup != null) { // Если currentActiveGroup - это одна из групп, сбрасываем кнопку статуса
            buttonSortStatus.setSelected(false);
            // buttonSortStatus.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.mtrl_btn_transparent_bg_color)); // Сброс на дефолтный
            // buttonSortStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.material_on_surface_emphasis_medium));
        }
    }


    private void setupFilterChips() {
        for (Map.Entry<TaskFilterOption, Chip> entry : filterChipMap.entrySet()) {
            Chip chip = entry.getValue();
            TaskFilterOption option = entry.getKey();
            // Устанавливаем слушатель
            chip.setOnClickListener(v -> viewModel.toggleFilterOption(option));
            // Убедимся, что checkedIcon виден, если чип выбран
            chip.setEnsureMinTouchTargetSize(false); // Для компактности
            chip.setChipIconVisible(false); // Скрываем иконку по умолчанию
            chip.setCheckedIconVisible(true); // Показываем галочку при выборе
        }
    }

    private void updateSortSelectionInDialog(TaskSortOption selectedSort) {
        if (selectedSort == null) return;
        uncheckOtherSortGroups(null); // Сброс всех перед установкой нового

        switch (selectedSort) {
            case TIME_ASC: toggleSortTime.check(R.id.button_sort_time_asc_planning); break;
            case TIME_DESC: toggleSortTime.check(R.id.button_sort_time_desc_planning); break;
            case PRIORITY_DESC: toggleSortPriority.check(R.id.button_sort_priority_desc_planning); break;
            case PRIORITY_ASC: toggleSortPriority.check(R.id.button_sort_priority_asc_planning); break;
            case CREATED_NEWEST: toggleSortCreated.check(R.id.button_sort_created_newest_planning); break;
            case CREATED_OLDEST: toggleSortCreated.check(R.id.button_sort_created_oldest_planning); break;
            case STATUS:
                buttonSortStatus.setSelected(true);
                // buttonSortStatus.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primaryContainerLight));
                break;
        }
    }

    private void updateFilterSelectionInDialog(Set<TaskFilterOption> selectedFilters) {
        if (selectedFilters == null) return;
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