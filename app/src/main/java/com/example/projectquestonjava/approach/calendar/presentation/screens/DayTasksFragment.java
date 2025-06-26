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
    private LinearLayout emptyTasksPlaceholder;
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
        try {
            sharedViewModel = new ViewModelProvider(requireParentFragment()).get(CalendarDashboardViewModel.class);
        } catch (IllegalStateException e) {
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

        sharedViewModel.pagerDataLiveData.observe(getViewLifecycleOwner(), pagerData -> {
            if (pageDate != null && pagerData != null && pagerData.containsKey(pageDate)) {
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
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    CalendarTaskSummary task = adapter.getCurrentList().get(position);
                    if (direction == ItemTouchHelper.LEFT) {
                        sharedViewModel.handleSwipeAction(task.getId(), CalendarDashboardViewModel.SwipeDirection.LEFT);
                    } else if (direction == ItemTouchHelper.RIGHT) {
                        sharedViewModel.handleSwipeAction(task.getId(), CalendarDashboardViewModel.SwipeDirection.RIGHT);
                    }
                    adapter.notifyItemChanged(position);
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                // super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive); // Не вызываем super, чтобы полностью контролировать отрисовку

                View itemView = viewHolder.itemView;
                float cornerRadius = itemView.getContext().getResources().getDimension(R.dimen.card_corner_radius_large); // Берем радиус из ресурсов

                // Используем кастомный макет для фона свайпа
                View swipeBackgroundContentView = LayoutInflater.from(itemView.getContext())
                        .inflate(R.layout.view_swipe_background_dashboard, (ViewGroup) itemView.getParent(), false);
                LinearLayout startActionLayout = swipeBackgroundContentView.findViewById(R.id.layout_swipe_action_start_to_end);
                ImageView startActionIcon = swipeBackgroundContentView.findViewById(R.id.imageView_swipe_action_start);
                TextView startActionText = swipeBackgroundContentView.findViewById(R.id.textView_swipe_action_start);
                LinearLayout endActionLayout = swipeBackgroundContentView.findViewById(R.id.layout_swipe_action_end_to_start);
                ImageView endActionIcon = swipeBackgroundContentView.findViewById(R.id.imageView_swipe_action_end);
                TextView endActionText = swipeBackgroundContentView.findViewById(R.id.textView_swipe_action_end);
                View rootBackgroundLayout = swipeBackgroundContentView.findViewById(R.id.layout_swipe_background);


                GradientDrawable backgroundDrawable = new GradientDrawable();
                backgroundDrawable.setCornerRadius(cornerRadius);

                int backgroundColor = Color.TRANSPARENT;

                if (dX > 0) { // Свайп вправо
                    int position = viewHolder.getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && position < adapter.getItemCount()) {
                        CalendarTaskSummary task = adapter.getCurrentList().get(position);
                        boolean isDone = task.getStatus() == TaskStatus.DONE;

                        backgroundColor = ContextCompat.getColor(itemView.getContext(), isDone ? R.color.swipe_action_undo : R.color.swipe_action_complete);
                        startActionText.setText(isDone ? "Вернуть" : "Выполнить");
                        startActionIcon.setImageResource(isDone ? R.drawable.undo : R.drawable.check);

                        startActionLayout.setVisibility(View.VISIBLE);
                        endActionLayout.setVisibility(View.GONE);
                        startActionText.setTextColor(Color.WHITE);
                        ImageViewCompat.setImageTintList(startActionIcon, ColorStateList.valueOf(Color.WHITE));
                    }
                    backgroundDrawable.setColor(backgroundColor);
                    backgroundDrawable.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + (int) dX, itemView.getBottom());

                } else if (dX < 0) { // Свайп влево
                    backgroundColor = ContextCompat.getColor(itemView.getContext(), R.color.swipe_action_delete);
                    endActionText.setText("Удалить");
                    endActionIcon.setImageResource(R.drawable.delete);

                    startActionLayout.setVisibility(View.GONE);
                    endActionLayout.setVisibility(View.VISIBLE);
                    endActionText.setTextColor(Color.WHITE);
                    ImageViewCompat.setImageTintList(endActionIcon, ColorStateList.valueOf(Color.WHITE));

                    backgroundDrawable.setColor(backgroundColor);
                    backgroundDrawable.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                } else {
                    startActionLayout.setVisibility(View.GONE);
                    endActionLayout.setVisibility(View.GONE);
                    backgroundDrawable.setColor(Color.TRANSPARENT); // Если нет сдвига, фон прозрачный
                    backgroundDrawable.setBounds(0,0,0,0); // Сбрасываем границы
                }

                backgroundDrawable.draw(c); // Рисуем фон


                // --- Вариант 2: Рисуем контент из swipeBackgroundContentView поверх ---
                if (Math.abs(dX) > 0) {
                    int contentWidth = itemView.getWidth();
                    int contentHeight = itemView.getHeight();
                    swipeBackgroundContentView.measure(
                            View.MeasureSpec.makeMeasureSpec(contentWidth, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(contentHeight, View.MeasureSpec.EXACTLY)
                    );
                    swipeBackgroundContentView.layout(itemView.getLeft(), itemView.getTop(), itemView.getRight(), itemView.getBottom());

                    c.save();
                    if (dX > 0) { // Свайп вправо
                        c.clipRect(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + dX, itemView.getBottom());
                    } else if (dX < 0) { // Свайп влево
                        c.clipRect(itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                    }

                    if (dX > 0) {
                        // Позиционируем startActionLayout
                        startActionLayout.setAlpha(Math.min(1f, Math.abs(dX) / (float)(startActionLayout.getWidth() + itemView.getContext().getResources().getDimensionPixelSize(R.dimen.padding_large_for_swipe))));
                        c.translate(itemView.getLeft() + itemView.getContext().getResources().getDimensionPixelSize(R.dimen.padding_large_for_swipe) , itemView.getTop() + (itemView.getHeight() - startActionLayout.getMeasuredHeight()) / 2f);
                        startActionLayout.draw(c);
                        c.translate(-(itemView.getLeft() + itemView.getContext().getResources().getDimensionPixelSize(R.dimen.padding_large_for_swipe)), -(itemView.getTop() + (itemView.getHeight() - startActionLayout.getMeasuredHeight()) / 2f));

                    } else if (dX < 0) {
                        // Позиционируем endActionLayout
                        endActionLayout.setAlpha(Math.min(1f, Math.abs(dX) / (float)(endActionLayout.getWidth() + itemView.getContext().getResources().getDimensionPixelSize(R.dimen.padding_large_for_swipe))));
                        c.translate(itemView.getRight() - endActionLayout.getMeasuredWidth() - itemView.getContext().getResources().getDimensionPixelSize(R.dimen.padding_large_for_swipe) , itemView.getTop() + (itemView.getHeight() - endActionLayout.getMeasuredHeight()) / 2f);
                        endActionLayout.draw(c);
                        c.translate(-(itemView.getRight() - endActionLayout.getMeasuredWidth() - itemView.getContext().getResources().getDimensionPixelSize(R.dimen.padding_large_for_swipe)) , -(itemView.getTop() + (itemView.getHeight() - endActionLayout.getMeasuredHeight()) / 2f));
                    }
                    c.restore();
                }


                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        new ItemTouchHelper(itemTouchCallback).attachToRecyclerView(recyclerViewTasks);
    }

    @Override
    public void onTaskClick(CalendarTaskSummary task) {
        sharedViewModel.showTaskDetailsBottomSheet(task);
    }
    @Override
    public void onTaskCheckedChange(CalendarTaskSummary task, boolean isChecked) {
        sharedViewModel.handleSwipeAction(task.getId(), CalendarDashboardViewModel.SwipeDirection.RIGHT);
    }
    @Override
    public void onEditTask(CalendarTaskSummary task) {
        sharedViewModel.editTask(task.getId());
    }
    @Override
    public void onPomodoroStart(CalendarTaskSummary task) {
        sharedViewModel.startPomodoroForTask(task.getId());
    }
    @Override
    public void onTagClick(Tag tag) {
        sharedViewModel.addTagToFilter(tag);
    }
}