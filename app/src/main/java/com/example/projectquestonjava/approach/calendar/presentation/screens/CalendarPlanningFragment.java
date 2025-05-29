package com.example.projectquestonjava.approach.calendar.presentation.screens;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.approach.calendar.domain.model.CalendarTaskSummary;
import com.example.projectquestonjava.approach.calendar.presentation.ui_parts.CalendarMoveSheetFragment;
import com.example.projectquestonjava.approach.calendar.presentation.ui_parts.CalendarViewPagerAdapter;
import com.example.projectquestonjava.approach.calendar.presentation.ui_parts.PlanningSortFilterBottomSheetFragment; // Импорт
import com.example.projectquestonjava.approach.calendar.presentation.ui_parts.SwipeToDeleteMoveCallback;
import com.example.projectquestonjava.approach.calendar.presentation.ui_parts.TaskDetailsBottomSheetFragment; // Импорт
import com.example.projectquestonjava.approach.calendar.presentation.viewmodels.CalendarPlanningViewModel;
import com.example.projectquestonjava.core.ui.BaseFragment;
import com.example.projectquestonjava.utils.dialogs.DeleteConfirmationDialogFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CalendarPlanningFragment extends BaseFragment
        implements TaskPlanningListAdapter.OnPlanningTaskItemClickListener, SwipeToDeleteMoveCallback.SwipeListener {

    private CalendarPlanningViewModel viewModel;
    private ViewPager2 calendarViewPager;
    private CalendarViewPagerAdapter calendarPagerAdapter;
    private RecyclerView tasksRecyclerView;
    private TaskPlanningListAdapter tasksAdapter;
    private FrameLayout calendarViewContainer;
    private LinearLayout emptyTasksLayout;
    private TextView emptyTasksMessage;
    private MaterialToolbar mainToolbar; // Ссылка на Toolbar из MainActivity

    private boolean isTaskDetailsSheetShown = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.planning_toolbar_menu, menu);
                // Можно здесь же настроить видимость индикатора фильтров, если он есть в меню
            }
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_filter_sort_planning) {
                    PlanningSortFilterBottomSheetFragment.newInstance()
                            .show(getChildFragmentManager(), "PlanningSortFilterSheet");
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        return inflater.inflate(R.layout.fragment_calendar_planning, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CalendarPlanningViewModel.class);
        mainToolbar = getToolbar(); // Получаем Toolbar из BaseFragment/MainActivity

        calendarViewContainer = view.findViewById(R.id.frameLayout_calendar_view_container_planning);
        calendarViewPager = view.findViewById(R.id.viewPager_calendar_months_planning);
        tasksRecyclerView = view.findViewById(R.id.recyclerView_planning_tasks);
        emptyTasksLayout = view.findViewById(R.id.layout_empty_planning_tasks);
        emptyTasksMessage = view.findViewById(R.id.textView_empty_planning_message);

        setupCalendarViewPager();
        setupTasksRecyclerView();
        setupObservers();
    }

    @Override
    protected void setupToolbar() {
        if (mainToolbar != null) {
            viewModel.currentMonthLiveData.observe(getViewLifecycleOwner(), yearMonth -> {
                if (yearMonth != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("LLLL yyyy", new Locale("ru"));
                    String formattedMonthYear = yearMonth.format(formatter);
                    formattedMonthYear = formattedMonthYear.substring(0, 1).toUpperCase() + formattedMonthYear.substring(1);
                    mainToolbar.setTitle(formattedMonthYear);
                }
            });
            mainToolbar.setOnClickListener(v -> viewModel.toggleCalendarExpanded());
        }
    }

    @Override
    protected void setupFab() {
        if (getStandardFab() != null) getStandardFab().hide();
    }

    private void setupCalendarViewPager() {
        YearMonth initialMonth = viewModel.currentMonthLiveData.getValue();
        if (initialMonth == null) initialMonth = YearMonth.now();

        calendarPagerAdapter = new CalendarViewPagerAdapter(getChildFragmentManager(), getLifecycle(), initialMonth);
        calendarViewPager.setAdapter(calendarPagerAdapter);
        calendarViewPager.setCurrentItem(CalendarViewPagerAdapter.INITIAL_PAGE, false);

        final YearMonth finalInitialMonth = initialMonth;
        calendarViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            private boolean firstSelection = true;
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (firstSelection && position == CalendarViewPagerAdapter.INITIAL_PAGE) {
                    firstSelection = false;
                    return;
                }
                YearMonth selectedMonth = finalInitialMonth.plusMonths((long) position - CalendarViewPagerAdapter.INITIAL_PAGE);
                if (!Objects.equals(viewModel.currentMonthLiveData.getValue(), selectedMonth)) {
                    viewModel.updateMonth(selectedMonth);
                }
            }
        });

        viewModel.currentMonthLiveData.observe(getViewLifecycleOwner(), yearMonth -> {
            if (yearMonth != null) {
                long diffMonths = ChronoUnit.MONTHS.between(finalInitialMonth, yearMonth);
                int targetPage = CalendarViewPagerAdapter.INITIAL_PAGE + (int) diffMonths;
                if (calendarViewPager.getCurrentItem() != targetPage) {
                    calendarViewPager.setCurrentItem(targetPage, true);
                }
            }
        });
    }

    private void setupTasksRecyclerView() {
        tasksAdapter = new TaskPlanningListAdapter(this, viewModel);
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        tasksRecyclerView.setAdapter(tasksAdapter);

        SwipeToDeleteMoveCallback swipeCallback = new SwipeToDeleteMoveCallback(requireContext(), this);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(tasksRecyclerView);
    }

    private void setupObservers() {
        viewModel.uiStateLiveData.observe(getViewLifecycleOwner(), uiState -> {
            if (uiState == null) return;

            if (uiState.getError() != null && !uiState.getError().isEmpty()) {
                Snackbar.make(requireView(), uiState.getError(), Snackbar.LENGTH_LONG).show();
                viewModel.clearError();
            }
            if (uiState.getSuccessMessage() != null && !uiState.getSuccessMessage().isEmpty()) {
                Snackbar.make(requireView(), uiState.getSuccessMessage(), Snackbar.LENGTH_SHORT).show();
                viewModel.clearSuccessMessage();
            }
        });

        viewModel.calendarExpandedLiveData.observe(getViewLifecycleOwner(), isExpanded -> {
            float targetAlpha = Boolean.TRUE.equals(isExpanded) ? 1.0f : 0.0f;
            int targetVisibility = Boolean.TRUE.equals(isExpanded) ? View.VISIBLE : View.GONE;

            if (calendarViewContainer.getVisibility() != targetVisibility) {
                if (Boolean.TRUE.equals(isExpanded)) {
                    calendarViewContainer.setAlpha(0f);
                    calendarViewContainer.setVisibility(View.VISIBLE);
                    calendarViewContainer.animate().alpha(1f).setDuration(300).setInterpolator(new AccelerateDecelerateInterpolator()).start();
                } else {
                    calendarViewContainer.animate().alpha(0f).setDuration(300).setInterpolator(new AccelerateDecelerateInterpolator()).withEndAction(() -> {
                        calendarViewContainer.setVisibility(View.GONE);
                    }).start();
                }
            }
        });

        viewModel.filteredTasksLiveData.observe(getViewLifecycleOwner(), tasks -> {
            tasksAdapter.submitList(tasks);
            boolean isEmpty = tasks == null || tasks.isEmpty();
            emptyTasksLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            tasksRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            if (isEmpty) {
                LocalDate selectedDate = viewModel.selectedDateLiveData.getValue();
                YearMonth currentMonth = viewModel.currentMonthLiveData.getValue();
                if (selectedDate != null) {
                    emptyTasksMessage.setText("Нет задач на " + selectedDate.format(DateTimeFormatter.ofPattern("d MMMM", new Locale("ru"))));
                } else if (currentMonth != null){
                    emptyTasksMessage.setText("Нет задач в " + currentMonth.format(DateTimeFormatter.ofPattern("LLLL", new Locale("ru"))));
                } else {
                    emptyTasksMessage.setText("Нет задач");
                }
            }
        });

        viewModel.showMoveTaskSheetLiveData.observe(getViewLifecycleOwner(), show -> {
            if (Boolean.TRUE.equals(show)) {
                YearMonth currentMonthForSheet = viewModel.currentMonthLiveData.getValue();
                if (currentMonthForSheet != null) {
                    CalendarMoveSheetFragment.newInstance(currentMonthForSheet)
                            .show(getChildFragmentManager(), "MoveTaskSheetPlanning");
                }
            }
        });

        viewModel.taskDetailsForBottomSheetLiveData.observe(getViewLifecycleOwner(), summary -> {
            if (summary != null && !isTaskDetailsSheetShown) {
                TaskDetailsBottomSheetFragment.newInstance(summary.getId(), false)
                        .show(getChildFragmentManager(), "PlanningTaskDetailsSheet");
                isTaskDetailsSheetShown = true;
            } else if (summary == null && isTaskDetailsSheetShown) {
                Fragment existingSheet = getChildFragmentManager().findFragmentByTag("PlanningTaskDetailsSheet");
                if (existingSheet instanceof TaskDetailsBottomSheetFragment && existingSheet.isVisible()) {
                    ((TaskDetailsBottomSheetFragment) existingSheet).dismissAllowingStateLoss();
                }
                isTaskDetailsSheetShown = false;
            }
        });
    }

    @Override
    public void onTaskCardClick(CalendarTaskSummary task) {
        viewModel.showTaskDetails(task);
    }

    @Override
    public void onTaskEditRequest(CalendarTaskSummary task) {
        Bundle args = new Bundle();
        args.putLong("taskId", task.getId());
        NavHostFragment.findNavController(this).navigate(R.id.action_global_to_calendarTaskCreationFragment, args);
    }

    @Override
    public void onPomodoroStartRequest(CalendarTaskSummary task) {
        Bundle args = new Bundle();
        args.putLong("taskId", task.getId());
        NavHostFragment.findNavController(this).navigate(R.id.action_global_to_pomodoroFragment, args);
    }

    @Override
    public void onTaskDeleteRequested(int position) {
        if (tasksAdapter != null && position != RecyclerView.NO_POSITION && position < tasksAdapter.getCurrentList().size()) {
            CalendarTaskSummary task = tasksAdapter.getCurrentList().get(position);
            DeleteConfirmationDialogFragment.newInstance(
                    "Удалить задачу?",
                    "Вы уверены, что хотите удалить задачу \"" + task.getTitle() + "\"?",
                    "Удалить",
                    R.drawable.warning,
                    () -> viewModel.deleteTask(task.getId())
            ).show(getChildFragmentManager(), "DeleteConfirmPlanningCb");
        }
        if (tasksAdapter != null) tasksAdapter.notifyItemChanged(position);
    }

    @Override
    public void onTaskMoveRequested(int position) {
        if (tasksAdapter != null && position != RecyclerView.NO_POSITION && position < tasksAdapter.getCurrentList().size()) {
            CalendarTaskSummary task = tasksAdapter.getCurrentList().get(position);
            viewModel.requestMoveTask(task.getId());
        }
        if (tasksAdapter != null) tasksAdapter.notifyItemChanged(position);
    }

    public void onDetailsSheetDismissed() {
        isTaskDetailsSheetShown = false;
        viewModel.clearTaskDetails();
    }
}