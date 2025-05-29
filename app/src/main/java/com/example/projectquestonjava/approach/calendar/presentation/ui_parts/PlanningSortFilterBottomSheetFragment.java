package com.example.projectquestonjava.approach.calendar.presentation.ui_parts;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskFilterOption;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskSortOption;
import com.example.projectquestonjava.approach.calendar.presentation.viewmodels.CalendarPlanningViewModel;
import com.example.projectquestonjava.core.utils.Logger; // Предполагаем, что логгер есть
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject; // Для логгера
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PlanningSortFilterBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String TAG = "PlanningSortFilterSheet";

    private CalendarPlanningViewModel viewModel;
    private MaterialButtonToggleGroup toggleSortTime, toggleSortPriority, toggleSortCreated;
    private MaterialButton buttonSortStatus;
    private ChipGroup chipGroupFiltersStatus, chipGroupFiltersPriority;
    private Map<TaskFilterOption, Chip> filterChipMap = new HashMap<>();

    private boolean isUpdatingSortFromVm = false;
    private boolean isUpdatingFiltersFromVm = false;

    @Inject // Внедряем логгер, если он нужен
    Logger logger;

    public static PlanningSortFilterBottomSheetFragment newInstance() {
        return new PlanningSortFilterBottomSheetFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            viewModel = new ViewModelProvider(requireParentFragment()).get(CalendarPlanningViewModel.class);
            if (logger != null) logger.debug(TAG, "ViewModel obtained successfully.");
        } catch (IllegalStateException e) {
            if (logger != null) logger.error(TAG, "Error getting ViewModel from parentFragment", e);
            dismissAllowingStateLoss();
        }
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
        if (logger != null) logger.debug(TAG, "onCreateView called.");
        return inflater.inflate(R.layout.bottom_sheet_planning_sort_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (logger != null) logger.debug(TAG, "onViewCreated called.");

        bindViews(view);
        setupSortControls();
        setupFilterChips();

        view.findViewById(R.id.button_reset_filters_planning).setOnClickListener(v -> {
            if (logger != null) logger.debug(TAG, "Reset button clicked.");
            viewModel.resetSortAndFilters();
        });
        view.findViewById(R.id.button_apply_filters_planning).setOnClickListener(v -> dismiss());

        viewModel.sortOptionLiveData.observe(getViewLifecycleOwner(), selectedSort -> {
            if (selectedSort != null) {
                if (logger != null) logger.debug(TAG, "sortOptionLiveData observed: " + selectedSort);
                isUpdatingSortFromVm = true;
                updateSortSelectionInDialog(selectedSort);
                isUpdatingSortFromVm = false;
            }
        });
        viewModel.filterOptionsLiveData.observe(getViewLifecycleOwner(), selectedFilters -> {
            if (selectedFilters != null) {
                if (logger != null) logger.debug(TAG, "filterOptionsLiveData observed: " + selectedFilters);
                isUpdatingFiltersFromVm = true;
                updateFilterSelectionInDialog(selectedFilters);
                isUpdatingFiltersFromVm = false;
            }
        });
    }

    private void bindViews(View view) {
        toggleSortTime = view.findViewById(R.id.toggle_sort_time_planning);
        toggleSortPriority = view.findViewById(R.id.toggle_sort_priority_planning);
        buttonSortStatus = view.findViewById(R.id.button_sort_status_planning);
        toggleSortCreated = view.findViewById(R.id.toggle_sort_created_planning);

        chipGroupFiltersStatus = view.findViewById(R.id.chipGroup_filters_status_challenges); // ID изменен на корректный
        chipGroupFiltersPriority = view.findViewById(R.id.chipGroup_filters_priority_planning); // Новый ChipGroup

        // Фильтры статуса
        filterChipMap.put(TaskFilterOption.ALL, view.findViewById(R.id.chip_filter_all_planning));
        filterChipMap.put(TaskFilterOption.INCOMPLETE, view.findViewById(R.id.chip_filter_incomplete_planning));
        filterChipMap.put(TaskFilterOption.COMPLETE, view.findViewById(R.id.chip_filter_complete_planning));
        filterChipMap.put(TaskFilterOption.OVERDUE, view.findViewById(R.id.chip_filter_overdue_planning));
        // Фильтры приоритета
        filterChipMap.put(TaskFilterOption.CRITICAL_PRIORITY, view.findViewById(R.id.chip_filter_critical_planning));
        filterChipMap.put(TaskFilterOption.HIGH_PRIORITY, view.findViewById(R.id.chip_filter_high_planning));
    }

    private void setupSortControls() {
        MaterialButtonToggleGroup.OnButtonCheckedListener listener = (group, checkedId, isChecked) -> {
            if (isChecked && !isUpdatingSortFromVm) {
                uncheckOtherSortGroups(group, buttonSortStatus);
                if (checkedId == R.id.button_sort_time_asc_planning) viewModel.updateSortOption(TaskSortOption.TIME_ASC);
                else if (checkedId == R.id.button_sort_time_desc_planning) viewModel.updateSortOption(TaskSortOption.TIME_DESC);
                else if (checkedId == R.id.button_sort_priority_desc_planning) viewModel.updateSortOption(TaskSortOption.PRIORITY_DESC);
                else if (checkedId == R.id.button_sort_priority_asc_planning) viewModel.updateSortOption(TaskSortOption.PRIORITY_ASC);
                else if (checkedId == R.id.button_sort_created_newest_planning) viewModel.updateSortOption(TaskSortOption.CREATED_NEWEST);
                else if (checkedId == R.id.button_sort_created_oldest_planning) viewModel.updateSortOption(TaskSortOption.CREATED_OLDEST);
            }
        };
        toggleSortTime.addOnButtonCheckedListener(listener);
        toggleSortPriority.addOnButtonCheckedListener(listener);
        toggleSortCreated.addOnButtonCheckedListener(listener);

        buttonSortStatus.setOnClickListener(v -> {
            if (!isUpdatingSortFromVm) {
                uncheckOtherSortGroups(null, buttonSortStatus); // Передаем null, так как это не ToggleGroup
                viewModel.updateSortOption(TaskSortOption.STATUS);
                updateButtonSortStatusSelection(true); // Визуально выделяем кнопку
            }
        });
    }

    private void uncheckOtherSortGroups(@Nullable MaterialButtonToggleGroup currentActiveGroup, MaterialButton statusButton) {
        if (currentActiveGroup != toggleSortTime) toggleSortTime.clearChecked();
        if (currentActiveGroup != toggleSortPriority) toggleSortPriority.clearChecked();
        if (currentActiveGroup != toggleSortCreated) toggleSortCreated.clearChecked();

        // Сбрасываем выделение кнопки статуса, если активна одна из групп
        if (currentActiveGroup != null && statusButton.isSelected()) {
            updateButtonSortStatusSelection(false);
        }
    }

    private void updateButtonSortStatusSelection(boolean isSelected) {
        buttonSortStatus.setSelected(isSelected); // MaterialButton поддерживает состояние selected
        if (isSelected) {
            buttonSortStatus.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primaryContainerLight)); // Пример цвета выделения
            buttonSortStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.onPrimaryContainerLight));
            buttonSortStatus.setIconTintResource(R.color.onPrimaryContainerLight);
        } else {
            // Сброс на стиль по умолчанию (лучше брать из атрибутов темы или ?attr/materialButtonOutlinedStyle)
            buttonSortStatus.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent)); // Прозрачный фон для Outlined
            buttonSortStatus.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.button_outlined_text_color_selector_planning)); // Селектор для текста
            buttonSortStatus.setIconTintResource(R.color.button_outlined_icon_color_selector_planning); // Селектор для иконки
        }
    }


    private void setupFilterChips() {
        for (Map.Entry<TaskFilterOption, Chip> entry : filterChipMap.entrySet()) {
            Chip chip = entry.getValue();
            TaskFilterOption option = entry.getKey();
            if (chip != null) {
                chip.setOnClickListener(v -> {
                    if (!isUpdatingFiltersFromVm) {
                        viewModel.toggleFilterOption(option);
                    }
                });
                chip.setEnsureMinTouchTargetSize(false);
                chip.setChipIconVisible(false);
                chip.setCheckedIconVisible(true);
            } else {
                if (logger != null) logger.warn(TAG, "Chip for filter option " + option + " not found in layout!");
            }
        }
    }

    private void updateSortSelectionInDialog(TaskSortOption selectedSort) {
        uncheckOtherSortGroups(null, buttonSortStatus); // Сначала все сбрасываем
        if (selectedSort == TaskSortOption.STATUS) {
            updateButtonSortStatusSelection(true);
        } else {
            updateButtonSortStatusSelection(false); // Убедимся, что кнопка статуса сброшена
            switch (selectedSort) {
                case TIME_ASC: toggleSortTime.check(R.id.button_sort_time_asc_planning); break;
                case TIME_DESC: toggleSortTime.check(R.id.button_sort_time_desc_planning); break;
                case PRIORITY_DESC: toggleSortPriority.check(R.id.button_sort_priority_desc_planning); break;
                case PRIORITY_ASC: toggleSortPriority.check(R.id.button_sort_priority_asc_planning); break;
                case CREATED_NEWEST: toggleSortCreated.check(R.id.button_sort_created_newest_planning); break;
                case CREATED_OLDEST: toggleSortCreated.check(R.id.button_sort_created_oldest_planning); break;
                default: // Если какой-то другой или null, ничего не выбираем в группах
                    toggleSortTime.clearChecked();
                    toggleSortPriority.clearChecked();
                    toggleSortCreated.clearChecked();
                    break;
            }
        }
    }

    private void updateFilterSelectionInDialog(Set<TaskFilterOption> selectedFilters) {
        if (selectedFilters == null) selectedFilters = new HashSet<>(); // Защита от NPE
        boolean isAllSelected = selectedFilters.contains(TaskFilterOption.ALL);

        for (Map.Entry<TaskFilterOption, Chip> entry : filterChipMap.entrySet()) {
            Chip chip = entry.getValue();
            TaskFilterOption option = entry.getKey();
            if (chip != null) {
                chip.setChecked(selectedFilters.contains(option));
                // Фильтр "Все" блокирует остальные фильтры статуса и приоритета,
                // но не должен блокировать сам себя.
                if (option != TaskFilterOption.ALL) {
                    chip.setEnabled(!isAllSelected);
                } else {
                    chip.setEnabled(true); // "Все" всегда доступен
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        toggleSortTime = null;
        toggleSortPriority = null;
        toggleSortCreated = null;
        buttonSortStatus = null;
        chipGroupFiltersStatus = null;
        chipGroupFiltersPriority = null;
        filterChipMap.clear();
        if (logger != null) logger.debug(TAG, "onDestroyView called.");
    }
}