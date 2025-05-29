package com.example.projectquestonjava.approach.calendar.presentation.screens;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.approach.calendar.domain.model.CalendarTaskSummary;
import com.example.projectquestonjava.approach.calendar.presentation.viewmodels.CalendarPlanningViewModel;
import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.core.data.model.enums.TaskStatus;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Objects;

public class TaskPlanningListAdapter extends ListAdapter<CalendarTaskSummary, TaskPlanningListAdapter.PlanningTaskViewHolder> {

    private final OnPlanningTaskItemClickListener listener;
    private final CalendarPlanningViewModel viewModel;

    public interface OnPlanningTaskItemClickListener {
        void onTaskCardClick(CalendarTaskSummary task);
        void onTaskEditRequest(CalendarTaskSummary task);
        void onPomodoroStartRequest(CalendarTaskSummary task);
    }

    public TaskPlanningListAdapter(@NonNull OnPlanningTaskItemClickListener listener, @NonNull CalendarPlanningViewModel viewModel) {
        super(DIFF_CALLBACK_PLANNING);
        this.listener = listener;
        this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public PlanningTaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_planning, parent, false);
        return new PlanningTaskViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull PlanningTaskViewHolder holder, int position) {
        CalendarTaskSummary currentTask = getItem(position);
        holder.bind(currentTask, listener, viewModel);
    }

    static class PlanningTaskViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final View priorityIndicator;
        private final TextView textTaskHour, textTaskMinute, textTaskTitle, textPomodoroCount = null;
        private final ChipGroup chipGroupTags;
        private final Space spacerTagsEnd;
        private final FrameLayout pomodoroContainer;
        // private final LinearLayout layoutTaskLeftPanel; // Не используется напрямую в bind, но есть в XML

        public PlanningTaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_task_planning_item);
            priorityIndicator = itemView.findViewById(R.id.view_priority_indicator_planning);
            textTaskHour = itemView.findViewById(R.id.text_task_hour_planning);
            textTaskMinute = itemView.findViewById(R.id.text_task_minute_planning);
            textTaskTitle = itemView.findViewById(R.id.textView_task_title_planning);
            chipGroupTags = itemView.findViewById(R.id.chip_group_task_tags_planning);
            spacerTagsEnd = itemView.findViewById(R.id.spacer_tags_planning_end);
            pomodoroContainer = itemView.findViewById(R.id.pomodoro_counter_container_planning);
            // layoutTaskLeftPanel = itemView.findViewById(R.id.layout_task_left_panel_planning);
        }

        public void bind(final CalendarTaskSummary task, final OnPlanningTaskItemClickListener listener, CalendarPlanningViewModel viewModel) {
            Context context = itemView.getContext();
            textTaskTitle.setText(task.getTitle());

            LocalTime dueTime = task.getDueDate().toLocalTime();
            textTaskHour.setText(String.format(Locale.getDefault(), "%02d", dueTime.getHour()));
            textTaskMinute.setText(String.format(Locale.getDefault(), "%02d", dueTime.getMinute()));

            // Устанавливаем цвет текста для времени такой же, как у заголовка (onSurface)
            int onSurfaceColor = ContextCompat.getColor(context, R.color.onSurfaceDark); // Или R.color.onSurfaceLight для светлой темы
            // Это можно улучшить, получая цвет из атрибута темы ?attr/colorOnSurface
            textTaskHour.setTextColor(onSurfaceColor);
            textTaskMinute.setTextColor(onSurfaceColor);


            Color priorityAndroidColor = viewModel.getPriorityColor(task.getPriority());
            priorityIndicator.setBackgroundColor(priorityAndroidColor.toArgb());


            boolean isChecked = task.getStatus() == TaskStatus.DONE;
            cardView.setAlpha(isChecked ? 0.65f : 1f);
            if (isChecked) {
                textTaskTitle.setPaintFlags(textTaskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                textTaskTitle.setPaintFlags(textTaskTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }

            if (task.getPomodoroCount() > 0) {
                pomodoroContainer.setVisibility(View.VISIBLE);
                textPomodoroCount.setText(String.valueOf(task.getPomodoroCount()));
                pomodoroContainer.setOnClickListener(v -> listener.onPomodoroStartRequest(task));
            } else {
                pomodoroContainer.setVisibility(View.GONE);
            }

            chipGroupTags.removeAllViews();
            boolean hasTags = task.getTags() != null && !task.getTags().isEmpty();
            if (hasTags) {
                chipGroupTags.setVisibility(View.VISIBLE);
                spacerTagsEnd.setVisibility(View.GONE);
                for (Tag tag : task.getTags()) {
                    Chip chip = (Chip) LayoutInflater.from(context).inflate(R.layout.item_tag, chipGroupTags, false);
                    chip.setText(tag.getName());
                    chipGroupTags.addView(chip);
                }
            } else {
                chipGroupTags.setVisibility(View.GONE);
                // spacerTagsEnd УПРАВЛЯЕТСЯ В XML через layout_weight и minHeight,
                // поэтому его видимость здесь менять не нужно, если он всегда должен занимать место.
                // Если нужно динамически убирать/показывать Space, то:
                spacerTagsEnd.setVisibility(View.VISIBLE);
            }

            cardView.setOnClickListener(v -> listener.onTaskCardClick(task));
            cardView.setOnLongClickListener(v -> {
                listener.onTaskEditRequest(task);
                return true;
            });
        }
    }

    private static final DiffUtil.ItemCallback<CalendarTaskSummary> DIFF_CALLBACK_PLANNING =
            new DiffUtil.ItemCallback<CalendarTaskSummary>() {
                @Override
                public boolean areItemsTheSame(@NonNull CalendarTaskSummary oldItem, @NonNull CalendarTaskSummary newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull CalendarTaskSummary oldItem, @NonNull CalendarTaskSummary newItem) {
                    return Objects.equals(oldItem.getTitle(), newItem.getTitle()) &&
                            oldItem.getStatus().equals( newItem.getStatus()) &&
                            oldItem.getPriority().equals(newItem.getPriority() )&&
                            oldItem.getDueDate().equals(newItem.getDueDate()) &&
                            oldItem.getPomodoroCount() == newItem.getPomodoroCount() &&
                            Objects.equals(oldItem.getRecurrenceRule(), newItem.getRecurrenceRule()) &&
                            Objects.equals(oldItem.getTags(), newItem.getTags()) &&
                            Objects.equals(oldItem.getSubtaskProgress(), newItem.getSubtaskProgress());
                }
            };
}