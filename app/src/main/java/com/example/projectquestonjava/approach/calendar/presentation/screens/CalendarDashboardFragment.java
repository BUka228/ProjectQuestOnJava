package com.example.projectquestonjava.approach.calendar.presentation.screens;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.projectquestonjava.R;
import com.example.projectquestonjava.app.MainActivity;
import com.example.projectquestonjava.approach.calendar.domain.model.CalendarTaskSummary;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskFilterOption;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskSortOption;
import com.example.projectquestonjava.approach.calendar.presentation.ui_parts.SortFilterBottomSheetDialogFragment;
import com.example.projectquestonjava.approach.calendar.presentation.ui_parts.TaskDetailsBottomSheetFragment;
import com.example.projectquestonjava.approach.calendar.presentation.viewmodels.CalendarDashboardViewModel;
import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.core.ui.BaseFragment;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.model.Gamification;
import com.example.projectquestonjava.utils.dialogs.DeleteConfirmationDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CalendarDashboardFragment extends BaseFragment implements TaskDashboardListAdapter.OnTaskItemClickListener {

    private static final String TAG = "CalDashboardFrag";

    private CalendarDashboardViewModel viewModel;
    private ProgressBar progressBarDashboardTasks;
    private HorizontalScrollView horizontalScrollViewFilterTags;
    private MaterialButton buttonClearAllTags;
    private ChipGroup chipGroupFilterTags;
    private ViewPager2 viewPagerDashboardDays;
    private DashboardPagerAdapter pagerAdapter;

    private boolean isDetailsSheetShown = false;
    private MenuItem filterSortMenuItem;
    private View filterIndicatorDotView;

    private View customToolbarTitleLayout;
    private FrameLayout frameDateCircleToolbar;
    private TextView textViewDateNumberToolbar;
    private TextView textViewDayNameToolbar;

    private ProgressBar toolbarLevelProgressBarAction;
    private TextView toolbarLevelTextAction;

    @Inject
    Logger logger;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        logger.debug(TAG, "onCreateView called");
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                logger.debug(TAG, "MenuProvider: onCreateMenu called");
                menuInflater.inflate(R.menu.dashboard_toolbar_menu, menu);
                filterSortMenuItem = menu.findItem(R.id.action_filter_sort_dashboard);
                if (filterSortMenuItem != null && filterSortMenuItem.getActionView() != null) {
                    logger.debug(TAG, "MenuProvider: Found filterSortMenuItem with ActionView.");
                    View actionView = filterSortMenuItem.getActionView();
                    filterIndicatorDotView = actionView.findViewById(R.id.filter_indicator_dot_view);
                    actionView.setOnClickListener(v -> {
                        logger.info(TAG, "Filter/Sort ActionView directly clicked! Showing BottomSheet..."); // <--- ИЗМЕНЕННЫЙ ЛОГ
                        SortFilterBottomSheetDialogFragment.newInstance()
                                .show(getChildFragmentManager(), "DashboardSortFilterSheet");
                    });
                    logger.debug(TAG, "MenuProvider: Filter/Sort ActionView and listener set up.");
                } else if (filterSortMenuItem != null) {
                    logger.warn(TAG, "MenuProvider: Filter/Sort MenuItem found, but NO ActionView. Standard click will be used.");
                } else {
                    logger.error(TAG, "MenuProvider: Filter/Sort MenuItem (action_filter_sort_dashboard) NOT FOUND in toolbar!");
                }

                MenuItem levelItem = menu.findItem(R.id.action_level_indicator_dashboard);
                if (levelItem != null && levelItem.getActionView() != null) {
                    View actionView = levelItem.getActionView();
                    toolbarLevelProgressBarAction = actionView.findViewById(R.id.toolbar_level_progress_bar_action);
                    toolbarLevelTextAction = actionView.findViewById(R.id.toolbar_level_text_action);
                    logger.debug(TAG, "MenuProvider: Level indicator ActionView set up.");
                } else {
                    logger.warn(TAG, "MenuProvider: Level indicator MenuItem or its ActionView NOT FOUND.");
                }
                updateFilterIndicatorDot();
                if (viewModel != null && viewModel.dashboardDataLiveData.getValue() != null && viewModel.dashboardDataLiveData.getValue().getGamification() != null) {
                    updateLevelIndicator(viewModel.dashboardDataLiveData.getValue().getGamification());
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int itemId = menuItem.getItemId();
                logger.debug(TAG, "MenuProvider: onMenuItemSelected for: " + menuItem.getTitle() + " (ID: " + itemId + ")");
                if (itemId == R.id.action_open_calendar_dialog_dashboard) {
                    logger.debug(TAG, "MenuProvider: Calendar dialog action selected.");
                    if (viewModel != null) viewModel.showCalendarDialog();
                    return true;
                } else if (itemId == R.id.action_filter_sort_dashboard) {
                    // Этот блок теперь может не вызываться, если клик обрабатывается слушателем ActionView
                    // Но оставим его на случай, если ActionView нет (например, на других размерах экрана)
                    logger.info(TAG, "MenuProvider: Filter/Sort standard menu item selected. Showing BottomSheet...");
                    SortFilterBottomSheetDialogFragment.newInstance()
                            .show(getChildFragmentManager(), "DashboardSortFilterSheet");
                    return true;
                } else if (itemId == R.id.action_level_indicator_dashboard) {
                    logger.debug(TAG, "MenuProvider: Level indicator standard menu item selected (should be ActionView).");
                    // Тут можно добавить какое-то действие, если клик по пустому месту элемента (маловероятно)
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        return inflater.inflate(R.layout.fragment_calendar_dashboard, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        logger.debug(TAG, "onViewCreated called");
        viewModel = new ViewModelProvider(this).get(CalendarDashboardViewModel.class);

        progressBarDashboardTasks = view.findViewById(R.id.progressBar_dashboard_tasks);
        horizontalScrollViewFilterTags = view.findViewById(R.id.horizontalScrollView_filter_tags_dashboard);
        buttonClearAllTags = view.findViewById(R.id.button_clear_all_tags_dashboard);
        chipGroupFilterTags = view.findViewById(R.id.chipGroup_filter_tags_dashboard);
        viewPagerDashboardDays = view.findViewById(R.id.viewPager_dashboard_days);

        buttonClearAllTags.setOnClickListener(v -> {
            logger.debug(TAG, "Clear All Tags button clicked.");
            viewModel.clearTagFilters();
        });

        setupViewPager();
        setupObservers();
        logger.debug(TAG, "onViewCreated finished setup.");
    }

    @Override
    protected void setupToolbar() {
        logger.debug(TAG, "setupToolbar called");
        MainActivity mainActivity = getMainActivity();
        if (mainActivity != null && getContext() != null) { // Добавил getContext() != null
            LayoutInflater inflater = LayoutInflater.from(getContext());
            customToolbarTitleLayout = inflater.inflate(R.layout.toolbar_dashboard_custom_title, mainActivity.getToolbar(), false);

            frameDateCircleToolbar = customToolbarTitleLayout.findViewById(R.id.frame_date_circle_toolbar);
            textViewDateNumberToolbar = customToolbarTitleLayout.findViewById(R.id.textView_date_number_toolbar);
            textViewDayNameToolbar = customToolbarTitleLayout.findViewById(R.id.textView_day_name_toolbar);

            mainActivity.setCustomToolbarTitleView(customToolbarTitleLayout);
            logger.debug(TAG, "setupToolbar: Custom toolbar title view set.");

            if (viewModel != null) { // viewModel может быть еще null, если setupToolbar вызывается до onViewCreated
                viewModel.selectedDateLiveData.observe(getViewLifecycleOwner(), selectedDate -> {
                    if (selectedDate != null && textViewDateNumberToolbar != null && textViewDayNameToolbar != null && frameDateCircleToolbar != null) {
                        DateTimeFormatter dayOfMonthFormatter = DateTimeFormatter.ofPattern("d");
                        DateTimeFormatter dayOfWeekFormatter = DateTimeFormatter.ofPattern("EE", new Locale("ru"));

                        textViewDateNumberToolbar.setText(selectedDate.format(dayOfMonthFormatter));
                        String dayOfWeekStr = selectedDate.format(dayOfWeekFormatter);
                        textViewDayNameToolbar.setText(dayOfWeekStr.substring(0, 1).toUpperCase(Locale.forLanguageTag("ru")) + dayOfWeekStr.substring(1));

                        boolean isToday = selectedDate.toLocalDate().isEqual(LocalDate.now());
                        frameDateCircleToolbar.setActivated(isToday); // Для state_activated в drawable
                        textViewDateNumberToolbar.setTextColor(ContextCompat.getColor(requireContext(),
                                isToday ? R.color.onPrimaryLight : R.color.onSurfaceVariantLight)); // Пример цветов
                    }
                });
            } else {
                logger.warn(TAG, "setupToolbar: ViewModel is null, cannot observe selectedDateLiveData yet.");
            }
        } else {
            logger.warn(TAG, "setupToolbar: MainActivity or Context is null.");
        }
    }


    @Override
    protected void setupFab() {
        logger.debug(TAG, "setupFab called");
        FloatingActionButton fab = getStandardFab();
        if (fab != null) {
            fab.setImageResource(R.drawable.add);
            fab.setContentDescription(getString(R.string.add_task_fab_description));
            fab.setOnClickListener(v -> {
                logger.debug(TAG, "Standard FAB clicked, navigating to task creation.");
                NavHostFragment.findNavController(CalendarDashboardFragment.this)
                        .navigate(R.id.action_global_to_calendarTaskCreationFragment);
            });
            fab.show();
            logger.debug(TAG, "setupFab: Standard FAB configured and shown.");
        } else {
            logger.warn(TAG, "setupFab: Standard FAB is null.");
        }
        if (getExtendedFab() != null) {
            getExtendedFab().hide();
            logger.debug(TAG, "setupFab: Extended FAB hidden.");
        }
    }

    private void updateLevelIndicator(Gamification gamification) {
        if (toolbarLevelProgressBarAction == null || toolbarLevelTextAction == null) {
            // logger.warn(TAG, "updateLevelIndicator: Toolbar level views are null.");
            return;
        }
        if (gamification != null) {
            toolbarLevelTextAction.setText("Ур. " + gamification.getLevel());
            if (gamification.getMaxExperienceForLevel() > 0) {
                int progress = (int) ((float) gamification.getExperience() / gamification.getMaxExperienceForLevel() * 100);
                ObjectAnimator.ofInt(toolbarLevelProgressBarAction, "progress", progress)
                        .setDuration(300)
                        .start();
            } else {
                toolbarLevelProgressBarAction.setProgress(0);
            }
            toolbarLevelProgressBarAction.setVisibility(View.VISIBLE);
            toolbarLevelTextAction.setVisibility(View.VISIBLE);
        } else {
            toolbarLevelProgressBarAction.setVisibility(View.GONE);
            toolbarLevelTextAction.setText("Ур. -");
        }
    }

    private void setupViewPager() {
        logger.debug(TAG, "setupViewPager called");
        List<LocalDateTime> dates = new ArrayList<>();
        for (int i = 0; i < CalendarDashboardViewModel.DATE_RANGE * 2 + 1; i++) {
            dates.add(viewModel.getDateForPage(i));
        }
        pagerAdapter = new DashboardPagerAdapter(this, dates);
        viewPagerDashboardDays.setAdapter(pagerAdapter);

        Integer initialPage = viewModel.currentPageLiveData.getValue();
        viewPagerDashboardDays.setCurrentItem(initialPage != null ? initialPage : CalendarDashboardViewModel.INITIAL_PAGE, false);
        logger.debug(TAG, "setupViewPager: Adapter set, initial page: " + viewPagerDashboardDays.getCurrentItem());


        viewPagerDashboardDays.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                logger.debug(TAG, "ViewPager: onPageSelected: " + position);
                viewModel.onPageChanged(position);
            }
        });
    }

    private void setupObservers() {
        logger.debug(TAG, "setupObservers called");
        viewModel.currentProgressLiveData.observe(getViewLifecycleOwner(), progress -> {
            if (progress != null && progressBarDashboardTasks != null) {
                ObjectAnimator.ofInt(progressBarDashboardTasks, "progress", progressBarDashboardTasks.getProgress(), (int) (progress * 100))
                        .setDuration(500).start();
            } else if (progressBarDashboardTasks != null) {
                progressBarDashboardTasks.setProgress(0);
            }
        });

        viewModel.dashboardDataLiveData.observe(getViewLifecycleOwner(), dashboardData -> {
            if (dashboardData != null) {
                updateLevelIndicator(dashboardData.getGamification());
            }
        });

        viewModel.selectedTagsLiveData.observe(getViewLifecycleOwner(), tags -> {
            chipGroupFilterTags.removeAllViews();
            if (tags != null && !tags.isEmpty()) {
                horizontalScrollViewFilterTags.setVisibility(View.VISIBLE);
                buttonClearAllTags.setVisibility(tags.size() > 1 ? View.VISIBLE : View.GONE);
                for (Tag tag : tags) {
                    Chip chip = (Chip) LayoutInflater.from(getContext()).inflate(R.layout.chip_filter_tag_dashboard, chipGroupFilterTags, false);
                    chip.setText(tag.getName());
                    chip.setCloseIconVisible(true);
                    chip.setOnCloseIconClickListener(v -> viewModel.removeTagFromFilter(tag));
                    chipGroupFilterTags.addView(chip);
                }
            } else {
                horizontalScrollViewFilterTags.setVisibility(View.GONE);
                buttonClearAllTags.setVisibility(View.GONE);
            }
        });

        viewModel.currentPageLiveData.observe(getViewLifecycleOwner(), page -> {
            if (page != null && viewPagerDashboardDays.getCurrentItem() != page) {
                logger.debug(TAG, "currentPageLiveData observed, setting ViewPager to page: " + page);
                viewPagerDashboardDays.setCurrentItem(page, true);
            }
        });

        viewModel.showCalendarDialogLiveData.observe(getViewLifecycleOwner(), show -> {
            if (Boolean.TRUE.equals(show)) {
                logger.debug(TAG, "showCalendarDialogLiveData observed, showing DatePickerDialog.");
                showDatePickerDialog();
            }
        });

        viewModel.swipeActionStateLiveData.observe(getViewLifecycleOwner(), state -> {
            if (state instanceof CalendarDashboardViewModel.SwipeActionState.ConfirmingDelete) {
                CalendarTaskSummary taskSummary = ((CalendarDashboardViewModel.SwipeActionState.ConfirmingDelete) state).getTaskSummary();
                logger.debug(TAG, "swipeActionStateLiveData: ConfirmingDelete for task: " + taskSummary.getTitle());
                DeleteConfirmationDialogFragment.newInstance(
                        "Удалить задачу?",
                        "Вы уверены, что хотите удалить задачу \"" + taskSummary.getTitle() + "\"?",
                        "Удалить",
                        R.drawable.warning,
                        () -> viewModel.confirmDeleteTask(taskSummary.getId())
                ).show(getChildFragmentManager(), "DeleteConfirmDashboard");
                viewModel.clearSwipeActionState();
            }
        });

        viewModel.filterOptionsLiveData.observe(getViewLifecycleOwner(), filters -> updateFilterIndicatorDot());
        viewModel.sortOptionLiveData.observe(getViewLifecycleOwner(), sort -> updateFilterIndicatorDot());

        viewModel.navigateToEditTaskEvent.observe(getViewLifecycleOwner(), taskId -> {
            if (taskId != null) {
                logger.debug(TAG, "navigateToEditTaskEvent observed, navigating to edit task ID: " + taskId);
                Bundle args = new Bundle();
                args.putLong("taskId", taskId);
                NavHostFragment.findNavController(this).navigate(R.id.action_global_to_calendarTaskCreationFragment, args);
                viewModel.clearNavigateToEditTask();
            }
        });
        viewModel.navigateToPomodoroEvent.observe(getViewLifecycleOwner(), taskId -> {
            if (taskId != null) {
                logger.debug(TAG, "navigateToPomodoroEvent observed, navigating to pomodoro for task ID: " + taskId);
                Bundle args = new Bundle();
                args.putLong("taskId", taskId);
                NavHostFragment.findNavController(this).navigate(R.id.action_global_to_pomodoroFragment, args);
                viewModel.clearNavigateToPomodoro();
            }
        });

        viewModel.taskDetailsForBottomSheetLiveData.observe(getViewLifecycleOwner(), summary -> {
            if (summary != null && !isDetailsSheetShown) {
                logger.debug(TAG, "taskDetailsForBottomSheetLiveData: Showing details for task ID: " + summary.getId());
                TaskDetailsBottomSheetFragment.newInstance(summary.getId(), true)
                        .show(getChildFragmentManager(), "TaskDetailsDashboardSheet");
                isDetailsSheetShown = true;
            } else if (summary == null && isDetailsSheetShown) {
                logger.debug(TAG, "taskDetailsForBottomSheetLiveData: Summary is null, attempting to dismiss sheet.");
                Fragment existingSheet = getChildFragmentManager().findFragmentByTag("TaskDetailsDashboardSheet");
                if (existingSheet instanceof TaskDetailsBottomSheetFragment && existingSheet.isVisible()) {
                    ((TaskDetailsBottomSheetFragment) existingSheet).dismissAllowingStateLoss();
                }
                isDetailsSheetShown = false; // Сбрасываем флаг в любом случае, если summary null
            }
        });
    }

    private void updateFilterIndicatorDot() {
        if (filterIndicatorDotView == null && filterSortMenuItem != null && filterSortMenuItem.getActionView() != null) {
            filterIndicatorDotView = filterSortMenuItem.getActionView().findViewById(R.id.filter_indicator_dot_view);
        }
        if (filterIndicatorDotView == null) {
            // logger.warn(TAG, "updateFilterIndicatorDot: filterIndicatorDotView is null.");
            return;
        }

        Set<TaskFilterOption> filters = viewModel.filterOptionsLiveData.getValue();
        TaskSortOption sort = viewModel.sortOptionLiveData.getValue();
        boolean hasActiveFilters = (filters != null && (!filters.contains(TaskFilterOption.ALL) || filters.size() > 1));
        boolean activeSortNotDefault = (sort != null && sort != TaskSortOption.TIME_ASC);

        filterIndicatorDotView.setVisibility(hasActiveFilters || activeSortNotDefault ? View.VISIBLE : View.GONE);
        // logger.debug(TAG, "updateFilterIndicatorDot: Visible = " + (hasActiveFilters || activeSortNotDefault));
    }

    private void showDatePickerDialog() {
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
        builder.setTitleText("Выберите дату");
        LocalDateTime selectedDate = viewModel.selectedDateLiveData.getValue();
        if (selectedDate != null) {
            builder.setSelection(selectedDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        MaterialDatePicker<Long> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                LocalDateTime newSelectedDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(selection), ZoneId.systemDefault());
                logger.debug(TAG, "DatePickerDialog: Date selected: " + newSelectedDate);
                viewModel.selectDate(newSelectedDate);
            }
            viewModel.hideCalendarDialog(); // Прячем диалог после выбора
        });
        picker.addOnDismissListener(dialog -> {
            logger.debug(TAG, "DatePickerDialog: Dismissed.");
            viewModel.hideCalendarDialog();
        });
        picker.addOnCancelListener(dialog -> {
            logger.debug(TAG, "DatePickerDialog: Cancelled.");
            viewModel.hideCalendarDialog();
        });
        picker.show(getChildFragmentManager(), picker.toString());
    }

    public void onDetailsSheetDismissed() { // Вызывается из TaskDetailsBottomSheetFragment
        logger.debug(TAG, "onDetailsSheetDismissed called from BottomSheet.");
        isDetailsSheetShown = false;
        viewModel.clearRequestedTaskDetails();
    }

    @Override public void onTaskClick(CalendarTaskSummary task) {
        logger.debug(TAG, "onTaskClick: Task '" + task.getTitle() + "' (ID: " + task.getId() + ")");
        viewModel.showTaskDetailsBottomSheet(task);
    }
    @Override public void onTaskCheckedChange(CalendarTaskSummary task, boolean isChecked) {
        logger.debug(TAG, "onTaskCheckedChange: Task '" + task.getTitle() + "', isChecked: " + isChecked);
        viewModel.handleSwipeAction(task.getId(), CalendarDashboardViewModel.SwipeDirection.RIGHT); // Имитируем свайп
    }
    @Override public void onEditTask(CalendarTaskSummary task) {
        logger.debug(TAG, "onEditTask: Task '" + task.getTitle() + "' (ID: " + task.getId() + ")");
        viewModel.editTask(task.getId());
    }
    @Override public void onPomodoroStart(CalendarTaskSummary task) {
        logger.debug(TAG, "onPomodoroStart: Task '" + task.getTitle() + "' (ID: " + task.getId() + ")");
        viewModel.startPomodoroForTask(task.getId());
    }
    @Override public void onTagClick(Tag tag) {
        logger.debug(TAG, "onTagClick: Tag '" + tag.getName() + "'");
        viewModel.addTagToFilter(tag);
    }

    @Override
    public void onDestroyView() {
        logger.debug(TAG, "onDestroyView called");
        MainActivity mainActivity = getMainActivity();
        if (mainActivity != null && customToolbarTitleLayout != null) {
            mainActivity.setCustomToolbarTitleView(null);
        }
        customToolbarTitleLayout = null;
        frameDateCircleToolbar = null;
        textViewDateNumberToolbar = null;
        textViewDayNameToolbar = null;
        toolbarLevelProgressBarAction = null;
        toolbarLevelTextAction = null;
        progressBarDashboardTasks = null;
        horizontalScrollViewFilterTags = null;
        buttonClearAllTags = null;
        chipGroupFilterTags = null;
        if (viewPagerDashboardDays != null) {
            viewPagerDashboardDays.setAdapter(null);
        }
        viewPagerDashboardDays = null;
        pagerAdapter = null;
        filterSortMenuItem = null;
        filterIndicatorDotView = null;
        super.onDestroyView();
    }
}