package com.example.projectquestonjava.approach.calendar.presentation.screens;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.approach.calendar.domain.model.CalendarTaskSummary;
import com.example.projectquestonjava.approach.calendar.presentation.viewmodels.CalendarDashboardViewModel;
import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.core.data.model.enums.TaskStatus;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DayTasksFragment extends Fragment implements TaskDashboardListAdapter.OnTaskItemClickListener {

    private static final String ARG_DATE = "arg_date";
    private CalendarDashboardViewModel sharedViewModel;
    private RecyclerView recyclerViewTasks;
    private TaskDashboardListAdapter adapter;
    private LinearLayout emptyTasksPlaceholder; // Переименовали для ясности
    private LocalDate pageDate;

    public static DayTasksFragment newInstance(LocalDate date) {
        DayTasksFragment fragment = new DayTasksFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DATE, date);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            pageDate = (LocalDate) getArguments().getSerializable(ARG_DATE);
        }
        // ViewModel получается от родительского фрагмента (CalendarDashboardFragment)
        // или Activity, если используется напрямую в Activity.
        // Для ViewPager2 внутри фрагмента, обычно это getChildFragmentManager() или requireParentFragment().
        // Если CalendarDashboardFragment является хостом для ViewPager2, то:
        try {
            sharedViewModel = new ViewModelProvider(requireParentFragment()).get(CalendarDashboardViewModel.class);
        } catch (IllegalStateException e) {
            // Если requireParentFragment() не работает (например, DayTasksFragment используется вне CalendarDashboardFragment),
            // пробуем requireActivity(). Это менее предпочтительно, если ViewModel специфична для фичи.
            sharedViewModel = new ViewModelProvider(requireActivity()).get(CalendarDashboardViewModel.class);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_day_tasks, container, false);
        recyclerViewTasks = view.findViewById(R.id.recycler_view_day_tasks);
        emptyTasksPlaceholder = view.findViewById(R.id.layout_empty_tasks_placeholder_day);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new TaskDashboardListAdapter(this);
        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewTasks.setAdapter(adapter);

        // Наблюдаем за pagerDataLiveData из общей ViewModel
        sharedViewModel.pagerDataLiveData.observe(getViewLifecycleOwner(), pagerData -> {
            if (pageDate != null && pagerData != null && pagerData.containsKey(pageDate)) {
                // Получаем задачи для ЭТОЙ страницы (даты)
                List<CalendarTaskSummary> tasksForThisDay = pagerData.get(pageDate).getTasks();
                if (tasksForThisDay != null) {
                    adapter.submitList(tasksForThisDay);
                    emptyTasksPlaceholder.setVisibility(tasksForThisDay.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerViewTasks.setVisibility(tasksForThisDay.isEmpty() ? View.GONE : View.VISIBLE);
                } else {
                    adapter.submitList(Collections.emptyList());
                    emptyTasksPlaceholder.setVisibility(View.VISIBLE);
                    recyclerViewTasks.setVisibility(View.GONE);
                }
            } else {
                // Если для этой даты нет данных (например, при первоначальной загрузке или ошибке)
                adapter.submitList(Collections.emptyList());
                emptyTasksPlaceholder.setVisibility(View.VISIBLE);
                recyclerViewTasks.setVisibility(View.GONE);
            }
        });

        setupItemTouchHelper();
    }

    private void setupItemTouchHelper() {
        ItemTouchHelper.SimpleCallback itemTouchCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition(); // Используем getAbsoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    CalendarTaskSummary task = adapter.getCurrentList().get(position);
                    if (direction == ItemTouchHelper.LEFT) {
                        sharedViewModel.handleSwipeAction(task.getId(), CalendarDashboardViewModel.SwipeDirection.LEFT);
                    } else if (direction == ItemTouchHelper.RIGHT) {
                        sharedViewModel.handleSwipeAction(task.getId(), CalendarDashboardViewModel.SwipeDirection.RIGHT);
                    }
                    adapter.notifyItemChanged(position); // Важно для возврата элемента на место
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                View itemView = viewHolder.itemView;
                // Получаем фон из view_swipe_background_dashboard.xml
                // Для простоты, создадим его программно здесь, но лучше инфлейтить и кешировать.
                // Этот подход очень упрощенный.
                Drawable icon = null;
                ColorDrawable background = new ColorDrawable();
                String text = "";
                int iconResId = 0;
                int backgroundColor = Color.TRANSPARENT;
                int textColor = Color.WHITE;
                int textVisibility = View.GONE;
                int iconVisibility = View.GONE;


                View swipeBackgroundView = LayoutInflater.from(itemView.getContext()).inflate(R.layout.view_swipe_background_dashboard, (ViewGroup) itemView.getParent(), false);
                RelativeLayout rootLayout = swipeBackgroundView.findViewById(R.id.layout_swipe_background);
                LinearLayout startActionLayout = swipeBackgroundView.findViewById(R.id.layout_swipe_action_start_to_end);
                ImageView startActionIcon = swipeBackgroundView.findViewById(R.id.imageView_swipe_action_start);
                TextView startActionText = swipeBackgroundView.findViewById(R.id.textView_swipe_action_start);
                LinearLayout endActionLayout = swipeBackgroundView.findViewById(R.id.layout_swipe_action_end_to_start);
                ImageView endActionIcon = swipeBackgroundView.findViewById(R.id.imageView_swipe_action_end);
                TextView endActionText = swipeBackgroundView.findViewById(R.id.textView_swipe_action_end);


                if (dX > 0) { // Свайп вправо
                    int position = viewHolder.getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        CalendarTaskSummary task = adapter.getCurrentList().get(position);
                        boolean isDone = task.getStatus() == TaskStatus.DONE;

                        backgroundColor = isDone ? ContextCompat.getColor(itemView.getContext(), R.color.swipe_action_undo) : ContextCompat.getColor(itemView.getContext(), R.color.swipe_action_complete);
                        text = isDone ? "Вернуть" : "Выполнить";
                        iconResId = isDone ? R.drawable.undo : R.drawable.check;
                        textColor = Color.WHITE;
                        textVisibility = View.VISIBLE;
                        iconVisibility = View.VISIBLE;

                        startActionLayout.setVisibility(View.VISIBLE);
                        endActionLayout.setVisibility(View.GONE);
                        startActionIcon.setImageResource(iconResId);
                        startActionText.setText(text);
                        startActionText.setTextColor(textColor);
                        ImageViewCompat.setImageTintList(startActionIcon, ColorStateList.valueOf(textColor));
                    }
                } else if (dX < 0) { // Свайп влево
                    backgroundColor = ContextCompat.getColor(itemView.getContext(), R.color.swipe_action_delete);
                    text = "Удалить";
                    iconResId = R.drawable.delete;
                    textColor = Color.WHITE;
                    textVisibility = View.VISIBLE;
                    iconVisibility = View.VISIBLE;

                    startActionLayout.setVisibility(View.GONE);
                    endActionLayout.setVisibility(View.VISIBLE);
                    endActionIcon.setImageResource(iconResId);
                    endActionText.setText(text);
                    endActionText.setTextColor(textColor);
                    ImageViewCompat.setImageTintList(endActionIcon, ColorStateList.valueOf(textColor));
                } else {
                    startActionLayout.setVisibility(View.GONE);
                    endActionLayout.setVisibility(View.GONE);
                }

                // Устанавливаем фон для корневого элемента swipeBackgroundView
                ((GradientDrawable) rootLayout.getBackground().mutate()).setColor(backgroundColor);


                // Рисуем наш кастомный фон
                // Измеряем и располагаем swipeBackgroundView
                int widthSpec = View.MeasureSpec.makeMeasureSpec(itemView.getWidth(), View.MeasureSpec.EXACTLY);
                int heightSpec = View.MeasureSpec.makeMeasureSpec(itemView.getHeight(), View.MeasureSpec.EXACTLY);
                swipeBackgroundView.measure(widthSpec, heightSpec);
                swipeBackgroundView.layout(itemView.getLeft(), itemView.getTop(), itemView.getRight(), itemView.getBottom());

                c.save();
                if (dX > 0) { // Свайп вправо
                    c.clipRect(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + dX, itemView.getBottom());
                } else if (dX < 0) { // Свайп влево
                    c.clipRect(itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                }
                swipeBackgroundView.draw(c);
                c.restore();
            }
        };
        new ItemTouchHelper(itemTouchCallback).attachToRecyclerView(recyclerViewTasks);
    }

    // Реализация интерфейса OnTaskItemClickListener
    @Override
    public void onTaskClick(CalendarTaskSummary task) {
        // В XML, BottomSheet обычно является DialogFragment
        // TaskDetailsBottomSheetFragment bottomSheet = TaskDetailsBottomSheetFragment.newInstance(task);
        // bottomSheet.show(getChildFragmentManager(), "TaskDetailsSheet");
        sharedViewModel.showTaskDetailsBottomSheet(task); // Если ViewModel управляет показом
    }

    @Override
    public void onTaskCheckedChange(CalendarTaskSummary task, boolean isChecked) {
        // Имитируем свайп вправо, ViewModel обработает логику
        sharedViewModel.handleSwipeAction(task.getId(), CalendarDashboardViewModel.SwipeDirection.RIGHT);
    }

    @Override
    public void onEditTask(CalendarTaskSummary task) {
        Bundle args = new Bundle();
        args.putLong("taskId", task.getId());
        // Навигация к экрану редактирования
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_global_to_calendarTaskCreationFragment, args);
    }

    @Override
    public void onPomodoroStart(CalendarTaskSummary task) {
        Bundle args = new Bundle();
        args.putLong("taskId", task.getId());
        // Навигация на экран Pomodoro
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_global_to_pomodoroFragment, args);
    }

    @Override
    public void onTagClick(Tag tag) {
        sharedViewModel.addTagToFilter(tag);
    }
}