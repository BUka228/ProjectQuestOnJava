package com.example.projectquestonjava.approach.calendar.presentation.screens;

import android.animation.ObjectAnimator;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import com.example.projectquestonjava.core.data.model.enums.TaskStatus;
import com.example.projectquestonjava.core.ui.BaseFragment;
import com.example.projectquestonjava.utils.dialogs.DeleteConfirmationDialogFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

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
    private TabLayout tabLayoutPagerIndicator;
    private ViewPager2 viewPagerDashboardDays;
    private DashboardPagerAdapter pagerAdapter;
    // Placeholder для ViewPager2, если он пуст, будет управляться в DayTasksFragment

    private boolean isDetailsSheetShown = false;
    private MenuItem filterSortMenuItem; // Для обновления иконки с точкой

    // Для кастомного Toolbar Title View
    private TextView toolbarDateDayNumber;
    private TextView toolbarDateDayName;
    private View toolbarDateIndicatorDot; // Для "сегодня"
    private ProgressBar toolbarLevelProgressBar;
    private TextView toolbarLevelText;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Добавляем MenuProvider для управления элементами меню Toolbar
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.dashboard_toolbar_menu, menu);
                filterSortMenuItem = menu.findItem(R.id.action_filter_sort_dashboard);
                // Инициализируем видимость точки фильтра
                updateFilterIndicatorDot();
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
        tabLayoutPagerIndicator = view.findViewById(R.id.tabLayout_pager_indicator_dashboard);
        viewPagerDashboardDays = view.findViewById(R.id.viewPager_dashboard_days);

        buttonClearAllTags.setOnClickListener(v -> viewModel.clearTagFilters());

        setupViewPagerAndIndicator();
        setupObservers();
    }

    @Override
    protected void setupToolbar() {
        MaterialToolbar toolbar = getToolbar();
        if (toolbar != null) {
            // Используем стандартный title, а сложные элементы вынесем в сам фрагмент ниже Toolbar
            // Или создадим кастомный layout для Toolbar и будем обновлять его.
            // Для простоты, пока оставляем title и actions.
            // MainActivity уже устанавливает title по label из NavGraph.
            // Здесь мы можем его переопределить или добавить subtitle.

            // Кастомизация заголовка (если стандартный Toolbar)
            // Наблюдаем за данными для обновления заголовка Toolbar
            viewModel.selectedDateLiveData.observe(getViewLifecycleOwner(), selectedDate -> {
                if (selectedDate != null && toolbar != null) {
                    String dayOfWeek = selectedDate.format(DateTimeFormatter.ofPattern("EEEE", new Locale("ru")));
                    dayOfWeek = dayOfWeek.substring(0, 1).toUpperCase() + dayOfWeek.substring(1);
                    String dateStr = selectedDate.format(DateTimeFormatter.ofPattern("d MMMM", new Locale("ru")));
                    String title = dayOfWeek + ", " + dateStr;
                    if (selectedDate.toLocalDate().isEqual(LocalDate.now())) {
                        title += " (Сегодня)";
                    }
                    toolbar.setTitle(title);
                }
            });
            // Обновление иконки меню (бургер) можно делать здесь, если нужно
            // toolbar.setNavigationIcon(R.drawable.menu_icon);
            // toolbar.setNavigationOnClickListener(v -> { /* Open drawer */ });
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
    }

    private void setupViewPagerAndIndicator() {
        // Генерация списка дат для пейджера (можно вынести в ViewModel, если даты динамические)
        List<LocalDateTime> dates = new ArrayList<>();
        for (int i = 0; i < CalendarDashboardViewModel.DATE_RANGE * 2 + 1; i++) {
            dates.add(viewModel.getDateForPage(i));
        }
        pagerAdapter = new DashboardPagerAdapter(this, dates); // Передаем `this` (Fragment)
        viewPagerDashboardDays.setAdapter(pagerAdapter);

        // Инициализация ViewPager2 текущей страницей из ViewModel
        Integer initialPage = viewModel.currentPageLiveData.getValue();
        viewPagerDashboardDays.setCurrentItem(initialPage != null ? initialPage : CalendarDashboardViewModel.INITIAL_PAGE, false);

        // Связываем ViewPager2 с TabLayout для индикатора точек
        new TabLayoutMediator(tabLayoutPagerIndicator, viewPagerDashboardDays, (tab, position) -> {
            // Здесь можно кастомизировать вид табов, если нужно (например, сделать их меньше)
        }).attach();

        // Слушатель для обновления ViewModel при смене страницы пользователем
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
                // Анимация ProgressBar
                ObjectAnimator progressAnimator = ObjectAnimator.ofInt(progressBarDashboardTasks, "progress", progressBarDashboardTasks.getProgress(), (int) (progress * 100));
                progressAnimator.setDuration(500); // Длительность анимации
                progressAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
                progressAnimator.start();
            } else {
                progressBarDashboardTasks.setProgress(0);
            }
        });

        viewModel.selectedTagsLiveData.observe(getViewLifecycleOwner(), tags -> {
            chipGroupFilterTags.removeAllViews(); // Очищаем предыдущие чипы
            if (tags != null && !tags.isEmpty()) {
                horizontalScrollViewFilterTags.setVisibility(View.VISIBLE);
                buttonClearAllTags.setVisibility(tags.size() > 1 ? View.VISIBLE : View.GONE);

                for (Tag tag : tags) {
                    Chip chip = (Chip) LayoutInflater.from(getContext()).inflate(R.layout.chip_filter_tag_item, chipGroupFilterTags, false);
                    // Вместо R.layout.chip_filter_tag_item, если он не создан, можно инфлейтить стандартный Chip
                    // Chip chip = new Chip(requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Filter);
                    chip.setText(tag.getName());
                    chip.setCloseIconVisible(true);
                    chip.setEnsureMinTouchTargetSize(false);
                    chip.setOnCloseIconClickListener(v -> viewModel.removeTagFromFilter(tag));
                    // chip.setChipBackgroundColorResource(R.color.your_chip_background_color); // Стилизуйте
                    // chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.your_chip_text_color));
                    chipGroupFilterTags.addView(chip);
                }
            } else {
                horizontalScrollViewFilterTags.setVisibility(View.GONE); // Скрываем, если тегов нет
                buttonClearAllTags.setVisibility(View.GONE);
            }
        });

        // Обновление ViewPager при изменении currentPage в ViewModel
        viewModel.currentPageLiveData.observe(getViewLifecycleOwner(), page -> {
            if (page != null && viewPagerDashboardDays.getCurrentItem() != page) {
                viewPagerDashboardDays.setCurrentItem(page, true); // Плавная прокрутка
            }
        });

        viewModel.showCalendarDialogLiveData.observe(getViewLifecycleOwner(), show -> {
            if (Boolean.TRUE.equals(show)) {
                showDatePickerDialog();
            }
        });

        // Обработка Snackbar сообщений (через MainActivity)
        viewModel.snackbarMessageEvent.observe(getViewLifecycleOwner(), message -> {
            // MainActivity уже подписана на SnackbarManager
        });

        // Показ диалога подтверждения удаления
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

        // Обновление индикатора фильтров
        viewModel.filterOptionsLiveData.observe(getViewLifecycleOwner(), filters -> updateFilterIndicatorDot());
        viewModel.sortOptionLiveData.observe(getViewLifecycleOwner(), sort -> updateFilterIndicatorDot());

        // Навигация
        viewModel.navigateToEditTaskEvent.observe(getViewLifecycleOwner(), taskId -> {
            if (taskId != null) {
                Bundle args = new Bundle();
                args.putLong("taskId", taskId);
                NavHostFragment.findNavController(this).navigate(R.id.action_global_to_calendarTaskCreationFragment, args);
                viewModel.clearNavigateToEditTask(); // Сбросить событие
            }
        });
        viewModel.navigateToPomodoroEvent.observe(getViewLifecycleOwner(), taskId -> {
            if (taskId != null) {
                Bundle args = new Bundle();
                args.putLong("taskId", taskId);
                NavHostFragment.findNavController(this).navigate(R.id.action_global_to_pomodoroFragment, args);
                viewModel.clearNavigateToPomodoro(); // Сбросить событие
            }
        });

        // Обновление BottomSheet для деталей задачи
        viewModel.taskDetailsForBottomSheetLiveData.observe(getViewLifecycleOwner(), summary -> {
            if (summary != null && !isDetailsSheetShown) {
                TaskDetailsBottomSheetFragment.newInstance(summary.getId(), true)
                        .show(getChildFragmentManager(), "TaskDetailsDashboardSheet");
                isDetailsSheetShown = true;
            } else if (summary == null && isDetailsSheetShown) {
                // Если ViewModel сбросила задачу, а BottomSheet был показан,
                // можно попытаться его закрыть.
                Fragment existingSheet = getChildFragmentManager().findFragmentByTag("TaskDetailsDashboardSheet");
                if (existingSheet instanceof TaskDetailsBottomSheetFragment && existingSheet.isVisible()) {
                    ((TaskDetailsBottomSheetFragment) existingSheet).dismissAllowingStateLoss();
                }
                isDetailsSheetShown = false;
            }
        });
    }

    private void updateFilterIndicatorDot() {
        if (filterSortMenuItem == null) return;

        Set<TaskFilterOption> filters = viewModel.filterOptionsLiveData.getValue();
        TaskSortOption sort = viewModel.sortOptionLiveData.getValue();

        boolean hasActiveFilters = (filters != null && (!filters.contains(TaskFilterOption.ALL) || filters.size() > 1));
        boolean activeSortNotDefault = (sort != null && sort != TaskSortOption.TIME_ASC);

        if (hasActiveFilters || activeSortNotDefault) {
            // Показываем точку (можно кастомизировать иконку с точкой)
            // filterSortMenuItem.setIcon(R.drawable.filter_list_active); // Нужна такая иконка
            // Простого способа добавить точку поверх стандартной иконки нет без кастомного ActionView.
            // Как вариант, можно изменить цвет иконки.
            Drawable icon = filterSortMenuItem.getIcon();
            if (icon != null) {
                Drawable mutatedIcon = icon.mutate(); // Важно для изоляции изменений
                mutatedIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primaryLight), PorterDuff.Mode.SRC_IN);
                filterSortMenuItem.setIcon(mutatedIcon);
            }
        } else {
            // Возвращаем стандартную иконку
            Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.filter_list);
            filterSortMenuItem.setIcon(icon); // Сброс фильтра цвета
        }
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
            viewModel.hideCalendarDialog(); // Скрываем диалог в ViewModel
        });
        picker.addOnDismissListener(dialog -> viewModel.hideCalendarDialog());
        picker.addOnCancelListener(dialog -> viewModel.hideCalendarDialog());

        picker.show(getChildFragmentManager(), picker.toString());
    }

    // Вызывается, когда BottomSheet закрывается (из TaskDetailsBottomSheetFragment.onDismiss)
    public void onDetailsSheetDismissed() {
        isDetailsSheetShown = false;
        viewModel.clearRequestedTaskDetails(); // Говорим ViewModel, что детали больше не нужны
    }

    @Override
    public void onTaskClick(CalendarTaskSummary task) {

    }

    @Override
    public void onTaskCheckedChange(CalendarTaskSummary task, boolean isChecked) {

    }

    @Override
    public void onEditTask(CalendarTaskSummary task) {

    }

    @Override
    public void onPomodoroStart(CalendarTaskSummary task) {

    }

    @Override
    public void onTagClick(Tag tag) {

    }
}