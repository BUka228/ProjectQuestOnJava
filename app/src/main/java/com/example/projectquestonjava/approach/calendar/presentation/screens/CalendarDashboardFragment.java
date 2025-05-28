package com.example.projectquestonjava.approach.calendar.presentation.screens;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
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
import com.example.projectquestonjava.feature.gamification.data.model.Gamification;
import com.example.projectquestonjava.utils.dialogs.DeleteConfirmationDialogFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
// Убрал TabLayout, так как он скрыт
// import com.google.android.material.tabs.TabLayout;
// import com.google.android.material.tabs.TabLayoutMediator;

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

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CalendarDashboardFragment extends BaseFragment implements TaskDashboardListAdapter.OnTaskItemClickListener {

    private CalendarDashboardViewModel viewModel;
    private ProgressBar progressBarDashboardTasks;
    private HorizontalScrollView horizontalScrollViewFilterTags;
    private MaterialButton buttonClearAllTags;
    private ChipGroup chipGroupFilterTags;
    //    private TabLayout tabLayoutPagerIndicator; // Закомментировано, так как скрыто
    private ViewPager2 viewPagerDashboardDays;
    private DashboardPagerAdapter pagerAdapter;

    private boolean isDetailsSheetShown = false;
    private MenuItem filterSortMenuItem;
    private View filterIndicatorDotView;

    // View для кастомного заголовка Toolbar
    private View customToolbarTitleLayout;
    private FrameLayout frameDateCircleToolbar;
    private TextView textViewDateNumberToolbar;
    private TextView textViewDayNameToolbar;

    // Для ActionView уровня
    private ProgressBar toolbarLevelProgressBarAction;
    private TextView toolbarLevelTextAction;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.dashboard_toolbar_menu, menu);
                filterSortMenuItem = menu.findItem(R.id.action_filter_sort_dashboard);
                if (filterSortMenuItem != null && filterSortMenuItem.getActionView() != null) {
                    filterIndicatorDotView = filterSortMenuItem.getActionView().findViewById(R.id.filter_indicator_dot_view);
                    // Устанавливаем OnClickListener на сам ActionView, чтобы он реагировал на клик
                    filterSortMenuItem.getActionView().setOnClickListener(v -> {
                        // Вызываем стандартный обработчик для MenuItem
                        onOptionsItemSelected(filterSortMenuItem);
                    });
                }

                MenuItem levelItem = menu.findItem(R.id.action_level_indicator_dashboard);
                if (levelItem != null && levelItem.getActionView() != null) {
                    View actionView = levelItem.getActionView();
                    toolbarLevelProgressBarAction = actionView.findViewById(R.id.toolbar_level_progress_bar_action);
                    toolbarLevelTextAction = actionView.findViewById(R.id.toolbar_level_text_action);
                    // Можно добавить OnClickListener для actionView, если нужно действие по клику на уровень
                    // actionView.setOnClickListener(v -> { /* Действие */ });
                }
                updateFilterIndicatorDot();
                // Обновление данных уровня при создании меню, если ViewModel уже доступна
                if (viewModel != null && viewModel.dashboardDataLiveData.getValue() != null) {
                    updateLevelIndicator(viewModel.dashboardDataLiveData.getValue().getGamification());
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int itemId = menuItem.getItemId();
                if (itemId == R.id.action_open_calendar_dialog_dashboard) {
                    viewModel.showCalendarDialog();
                    return true;
                } else if (itemId == R.id.action_filter_sort_dashboard) {
                    SortFilterBottomSheetDialogFragment.newInstance()
                            .show(getChildFragmentManager(), "DashboardSortFilterSheet");
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        return inflater.inflate(R.layout.fragment_calendar_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState); // Вызывает setupToolbar и setupFab
        viewModel = new ViewModelProvider(this).get(CalendarDashboardViewModel.class);

        progressBarDashboardTasks = view.findViewById(R.id.progressBar_dashboard_tasks);
        horizontalScrollViewFilterTags = view.findViewById(R.id.horizontalScrollView_filter_tags_dashboard);
        buttonClearAllTags = view.findViewById(R.id.button_clear_all_tags_dashboard);
        chipGroupFilterTags = view.findViewById(R.id.chipGroup_filter_tags_dashboard);
        // tabLayoutPagerIndicator = view.findViewById(R.id.tabLayout_pager_indicator_dashboard); // Закомментировано
        viewPagerDashboardDays = view.findViewById(R.id.viewPager_dashboard_days);

        // if (tabLayoutPagerIndicator != null) { // Проверка на null перед использованием
        //     tabLayoutPagerIndicator.setVisibility(View.GONE);
        // }

        buttonClearAllTags.setOnClickListener(v -> viewModel.clearTagFilters());

        setupViewPager();
        setupObservers();
    }

    @Override
    protected void setupToolbar() {
        MainActivity mainActivity = getMainActivity();
        // MaterialToolbar toolbar = getToolbar(); // Получаем Toolbar из BaseFragment (уже есть в mainActivity)

        if (mainActivity != null) {
            // Инфлейтим наш кастомный макет для заголовка
            LayoutInflater inflater = LayoutInflater.from(getContext());
            customToolbarTitleLayout = inflater.inflate(R.layout.toolbar_dashboard_custom_title, mainActivity.getToolbar(), false);
            // Не нужно устанавливать ID для customToolbarTitleLayout

            frameDateCircleToolbar = customToolbarTitleLayout.findViewById(R.id.frame_date_circle_toolbar);
            textViewDateNumberToolbar = customToolbarTitleLayout.findViewById(R.id.textView_date_number_toolbar);
            textViewDayNameToolbar = customToolbarTitleLayout.findViewById(R.id.textView_day_name_toolbar);

            mainActivity.setCustomToolbarTitleView(customToolbarTitleLayout);

            viewModel.selectedDateLiveData.observe(getViewLifecycleOwner(), selectedDate -> {
                if (selectedDate != null && textViewDateNumberToolbar != null && textViewDayNameToolbar != null && frameDateCircleToolbar != null) {
                    DateTimeFormatter dayOfMonthFormatter = DateTimeFormatter.ofPattern("d");
                    DateTimeFormatter dayOfWeekFormatter = DateTimeFormatter.ofPattern("EE", new Locale("ru"));

                    textViewDateNumberToolbar.setText(selectedDate.format(dayOfMonthFormatter));
                    String dayOfWeekStr = selectedDate.format(dayOfWeekFormatter);
                    textViewDayNameToolbar.setText(dayOfWeekStr.substring(0, 1).toUpperCase(Locale.forLanguageTag("ru")) + dayOfWeekStr.substring(1));

                    boolean isToday = selectedDate.toLocalDate().isEqual(LocalDate.now());
                    frameDateCircleToolbar.setActivated(isToday);
                    textViewDateNumberToolbar.setTextColor(ContextCompat.getColor(requireContext(),
                            isToday ? R.color.onPrimaryLight : R.color.onSurfaceVariantLight));
                }
            });
        }
    }


    @Override
    protected void setupFab() {
        FloatingActionButton fab = getStandardFab();
        if (fab != null) {
            fab.setImageResource(R.drawable.add);
            fab.setContentDescription(getString(R.string.add_task_fab_description));
            fab.setOnClickListener(v -> {
                NavHostFragment.findNavController(CalendarDashboardFragment.this)
                        .navigate(R.id.action_global_to_calendarTaskCreationFragment);
            });
            fab.show();
        }
        if (getExtendedFab() != null) getExtendedFab().hide();
    }

    private void updateLevelIndicator(Gamification gamification) {
        if (toolbarLevelProgressBarAction == null || toolbarLevelTextAction == null) return;
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
        List<LocalDateTime> dates = new ArrayList<>();
        for (int i = 0; i < CalendarDashboardViewModel.DATE_RANGE * 2 + 1; i++) {
            dates.add(viewModel.getDateForPage(i));
        }
        pagerAdapter = new DashboardPagerAdapter(this, dates);
        viewPagerDashboardDays.setAdapter(pagerAdapter);

        Integer initialPage = viewModel.currentPageLiveData.getValue();
        viewPagerDashboardDays.setCurrentItem(initialPage != null ? initialPage : CalendarDashboardViewModel.INITIAL_PAGE, false);

        viewPagerDashboardDays.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                viewModel.onPageChanged(position);
            }
        });
    }

    private void setupObservers() {
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
                    Chip chip = (Chip) LayoutInflater.from(getContext()).inflate(R.layout.chip_filter_tag_item, chipGroupFilterTags, false);
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
                viewPagerDashboardDays.setCurrentItem(page, true);
            }
        });

        viewModel.showCalendarDialogLiveData.observe(getViewLifecycleOwner(), show -> {
            if (Boolean.TRUE.equals(show)) {
                showDatePickerDialog();
            }
        });

        viewModel.swipeActionStateLiveData.observe(getViewLifecycleOwner(), state -> {
            if (state instanceof CalendarDashboardViewModel.SwipeActionState.ConfirmingDelete) {
                CalendarTaskSummary taskSummary = ((CalendarDashboardViewModel.SwipeActionState.ConfirmingDelete) state).getTaskSummary();
                DeleteConfirmationDialogFragment.newInstance(
                        "Удалить задачу?",
                        "Вы уверены, что хотите удалить задачу \"" + taskSummary.getTitle() + "\"?",
                        "Удалить",
                        R.drawable.warning,
                        () -> viewModel.confirmDeleteTask(taskSummary.getId())
                ).show(getChildFragmentManager(), "DeleteConfirmDashboard");
                viewModel.clearSwipeActionState(); // Сбрасываем состояние после показа диалога
            }
        });

        viewModel.filterOptionsLiveData.observe(getViewLifecycleOwner(), filters -> updateFilterIndicatorDot());
        viewModel.sortOptionLiveData.observe(getViewLifecycleOwner(), sort -> updateFilterIndicatorDot());

        viewModel.navigateToEditTaskEvent.observe(getViewLifecycleOwner(), taskId -> {
            if (taskId != null) {
                Bundle args = new Bundle();
                args.putLong("taskId", taskId);
                NavHostFragment.findNavController(this).navigate(R.id.action_global_to_calendarTaskCreationFragment, args);
                viewModel.clearNavigateToEditTask();
            }
        });
        viewModel.navigateToPomodoroEvent.observe(getViewLifecycleOwner(), taskId -> {
            if (taskId != null) {
                Bundle args = new Bundle();
                args.putLong("taskId", taskId);
                NavHostFragment.findNavController(this).navigate(R.id.action_global_to_pomodoroFragment, args);
                viewModel.clearNavigateToPomodoro();
            }
        });

        viewModel.taskDetailsForBottomSheetLiveData.observe(getViewLifecycleOwner(), summary -> {
            if (summary != null && !isDetailsSheetShown) {
                TaskDetailsBottomSheetFragment.newInstance(summary.getId(), true)
                        .show(getChildFragmentManager(), "TaskDetailsDashboardSheet");
                isDetailsSheetShown = true;
            } else if (summary == null && isDetailsSheetShown) {
                // Эта логика может быть избыточной, если BottomSheet сам себя закрывает при отсутствии данных
                Fragment existingSheet = getChildFragmentManager().findFragmentByTag("TaskDetailsDashboardSheet");
                if (existingSheet instanceof TaskDetailsBottomSheetFragment && existingSheet.isVisible()) {
                    ((TaskDetailsBottomSheetFragment) existingSheet).dismissAllowingStateLoss();
                }
                isDetailsSheetShown = false;
            }
        });
    }
    private void updateFilterIndicatorDot() {
        if (filterIndicatorDotView == null && filterSortMenuItem != null && filterSortMenuItem.getActionView() != null) {
            filterIndicatorDotView = filterSortMenuItem.getActionView().findViewById(R.id.filter_indicator_dot_view);
        }
        if (filterIndicatorDotView == null) return;

        Set<TaskFilterOption> filters = viewModel.filterOptionsLiveData.getValue();
        TaskSortOption sort = viewModel.sortOptionLiveData.getValue();
        boolean hasActiveFilters = (filters != null && (!filters.contains(TaskFilterOption.ALL) || filters.size() > 1));
        boolean activeSortNotDefault = (sort != null && sort != TaskSortOption.TIME_ASC);

        filterIndicatorDotView.setVisibility(hasActiveFilters || activeSortNotDefault ? View.VISIBLE : View.GONE);
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
                viewModel.selectDate(newSelectedDate);
            }
            viewModel.hideCalendarDialog();
        });
        picker.addOnDismissListener(dialog -> viewModel.hideCalendarDialog());
        picker.addOnCancelListener(dialog -> viewModel.hideCalendarDialog());
        picker.show(getChildFragmentManager(), picker.toString());
    }

    public void onDetailsSheetDismissed() {
        isDetailsSheetShown = false;
        viewModel.clearRequestedTaskDetails();
    }

    // Реализация интерфейса OnTaskItemClickListener (заглушки)
    @Override public void onTaskClick(CalendarTaskSummary task) { viewModel.showTaskDetailsBottomSheet(task); }
    @Override public void onTaskCheckedChange(CalendarTaskSummary task, boolean isChecked) {
        viewModel.handleSwipeAction(task.getId(), CalendarDashboardViewModel.SwipeDirection.RIGHT);
    }
    @Override public void onEditTask(CalendarTaskSummary task) { viewModel.editTask(task.getId()); }
    @Override public void onPomodoroStart(CalendarTaskSummary task) { viewModel.startPomodoroForTask(task.getId()); }
    @Override public void onTagClick(Tag tag) { viewModel.addTagToFilter(tag); }


    @Override
    public void onDestroyView() {
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
        // tabLayoutPagerIndicator = null;
        if (viewPagerDashboardDays != null) {
            viewPagerDashboardDays.setAdapter(null); // Отвязываем адаптер
        }
        viewPagerDashboardDays = null;
        pagerAdapter = null;
        filterSortMenuItem = null;
        filterIndicatorDotView = null;
        super.onDestroyView();
    }
}