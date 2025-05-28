package com.example.projectquestonjava.approach.calendar.presentation.screens;

import android.animation.ObjectAnimator;
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
    //    private TabLayout tabLayoutPagerIndicator; // Больше не нужен
    private ViewPager2 viewPagerDashboardDays;
    private DashboardPagerAdapter pagerAdapter;

    private boolean isDetailsSheetShown = false;
    private MenuItem filterSortMenuItem;
    private View filterIndicatorDotView; // View для точки на иконке фильтра

    // Для кастомного заголовка Toolbar
    private View customTitleView;
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
                    filterSortMenuItem.getActionView().setOnClickListener(v -> onMenuItemSelected(filterSortMenuItem));
                }

                MenuItem levelItem = menu.findItem(R.id.action_level_indicator_dashboard);
                if (levelItem != null && levelItem.getActionView() != null) {
                    View actionView = levelItem.getActionView();
                    toolbarLevelProgressBarAction = actionView.findViewById(R.id.toolbar_level_progress_bar_action);
                    toolbarLevelTextAction = actionView.findViewById(R.id.toolbar_level_text_action);
                }
                updateFilterIndicatorDot();
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
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CalendarDashboardViewModel.class);

        progressBarDashboardTasks = view.findViewById(R.id.progressBar_dashboard_tasks);
        horizontalScrollViewFilterTags = view.findViewById(R.id.horizontalScrollView_filter_tags_dashboard);
        buttonClearAllTags = view.findViewById(R.id.button_clear_all_tags_dashboard);
        chipGroupFilterTags = view.findViewById(R.id.chipGroup_filter_tags_dashboard);
        // tabLayoutPagerIndicator = view.findViewById(R.id.tabLayout_pager_indicator_dashboard); // Закомментировано
        viewPagerDashboardDays = view.findViewById(R.id.viewPager_dashboard_days);

        // tabLayoutPagerIndicator.setVisibility(View.GONE); // Убедимся, что он скрыт

        buttonClearAllTags.setOnClickListener(v -> viewModel.clearTagFilters());

        setupViewPager(); // Убрали индикатор из названия метода
        setupObservers();
    }

    @Override
    protected void setupToolbar() {
        MaterialToolbar toolbar = getToolbar();
        if (toolbar != null) {
            // Очищаем стандартный Title, если он был установлен MainActivity
            toolbar.setTitle("");

            // Убираем предыдущий кастомный TitleView, если он был
            if (customTitleView != null && customTitleView.getParent() != null) {
                ((ViewGroup) customTitleView.getParent()).removeView(customTitleView);
            }

            // Инфлейтим и добавляем кастомный TitleView
            LayoutInflater inflater = LayoutInflater.from(toolbar.getContext());
            customTitleView = inflater.inflate(R.layout.toolbar_dashboard_custom_title, toolbar, false);
            frameDateCircleToolbar = customTitleView.findViewById(R.id.frame_date_circle_toolbar);
            textViewDateNumberToolbar = customTitleView.findViewById(R.id.textView_date_number_toolbar);
            textViewDayNameToolbar = customTitleView.findViewById(R.id.textView_day_name_toolbar);

            // Устанавливаем параметры для центрирования кастомного заголовка
            MaterialToolbar.LayoutParams params = new MaterialToolbar.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            params.gravity = Gravity.START; // Выравнивание по левому краю
            customTitleView.setLayoutParams(params);
            toolbar.addView(customTitleView);

            viewModel.selectedDateLiveData.observe(getViewLifecycleOwner(), selectedDate -> {
                if (selectedDate != null) {
                    DateTimeFormatter dayOfMonthFormatter = DateTimeFormatter.ofPattern("d");
                    DateTimeFormatter dayOfWeekFormatter = DateTimeFormatter.ofPattern("EE", new Locale("ru"));

                    textViewDateNumberToolbar.setText(selectedDate.format(dayOfMonthFormatter));
                    String dayOfWeekStr = selectedDate.format(dayOfWeekFormatter);
                    textViewDayNameToolbar.setText(dayOfWeekStr.substring(0, 1).toUpperCase(Locale.forLanguageTag("ru")) + dayOfWeekStr.substring(1));

                    frameDateCircleToolbar.setActivated(selectedDate.toLocalDate().isEqual(LocalDate.now()));
                    // Цвет текста внутри круга будет меняться через селектор drawable для фона
                    if(selectedDate.toLocalDate().isEqual(LocalDate.now())){
                        textViewDateNumberToolbar.setTextColor(ContextCompat.getColor(requireContext(), R.color.onPrimaryLight)); // Пример
                    } else {
                        textViewDateNumberToolbar.setTextColor(ContextCompat.getColor(requireContext(), R.color.onSurfaceVariantLight)); // Пример
                    }
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
                toolbarLevelProgressBarAction.setProgress(progress);
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

    private void setupViewPager() { // Убрали индикатор из названия
        List<LocalDateTime> dates = new ArrayList<>();
        for (int i = 0; i < CalendarDashboardViewModel.DATE_RANGE * 2 + 1; i++) {
            dates.add(viewModel.getDateForPage(i));
        }
        pagerAdapter = new DashboardPagerAdapter(this, dates);
        viewPagerDashboardDays.setAdapter(pagerAdapter);

        Integer initialPage = viewModel.currentPageLiveData.getValue();
        viewPagerDashboardDays.setCurrentItem(initialPage != null ? initialPage : CalendarDashboardViewModel.INITIAL_PAGE, false);

        // TabLayout больше не используется
        // new TabLayoutMediator(tabLayoutPagerIndicator, viewPagerDashboardDays, (tab, position) -> {}).attach();

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
            if (progress != null) {
                ObjectAnimator progressAnimator = ObjectAnimator.ofInt(progressBarDashboardTasks, "progress", progressBarDashboardTasks.getProgress(), (int) (progress * 100));
                progressAnimator.setDuration(500);
                progressAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
                progressAnimator.start();
            } else {
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
                viewModel.clearSwipeActionState();
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
                Fragment existingSheet = getChildFragmentManager().findFragmentByTag("TaskDetailsDashboardSheet");
                if (existingSheet instanceof TaskDetailsBottomSheetFragment && existingSheet.isVisible()) {
                    ((TaskDetailsBottomSheetFragment) existingSheet).dismissAllowingStateLoss();
                }
                isDetailsSheetShown = false;
            }
        });
    }

    private void updateFilterIndicatorDot() {
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

    // Реализация интерфейса OnTaskItemClickListener (заглушки, т.к. логика в ViewModel)
    @Override public void onTaskClick(CalendarTaskSummary task) {}
    @Override public void onTaskCheckedChange(CalendarTaskSummary task, boolean isChecked) {}
    @Override public void onEditTask(CalendarTaskSummary task) {}
    @Override public void onPomodoroStart(CalendarTaskSummary task) {}
    @Override public void onTagClick(Tag tag) {}

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Обнуляем ссылки на View
        progressBarDashboardTasks = null;
        horizontalScrollViewFilterTags = null;
        buttonClearAllTags = null;
        chipGroupFilterTags = null;
        // tabLayoutPagerIndicator = null;
        viewPagerDashboardDays = null;
        pagerAdapter = null;
        filterSortMenuItem = null;
        filterIndicatorDotView = null;
        customTitleView = null;
        frameDateCircleToolbar = null;
        textViewDateNumberToolbar = null;
        textViewDayNameToolbar = null;
        toolbarLevelProgressBarAction = null;
        toolbarLevelTextAction = null;
    }
}