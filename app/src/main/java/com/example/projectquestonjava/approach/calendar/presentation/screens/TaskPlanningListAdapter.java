package com.example.projectquestonjava.approach.calendar.presentation.screens; // Уточните пакет

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
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
    private final CalendarPlanningViewModel viewModel; // Для доступа к getPriorityColor

    public interface OnPlanningTaskItemClickListener {
        void onTaskCardClick(CalendarTaskSummary task);
        void onTaskEditRequest(CalendarTaskSummary task); // Изменен с taskId на task
        void onPomodoroStartRequest(CalendarTaskSummary task); // Изменен с taskId на task
        // Свайпы теперь обрабатываются через SwipeToDeleteMoveCallback.SwipeListener во фрагменте
    }

    public TaskPlanningListAdapter(@NonNull OnPlanningTaskItemClickListener listener, @NonNull CalendarPlanningViewModel viewModel) {
        super(DIFF_CALLBACK_PLANNING); // Используем свой DiffUtil
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
        private final TextView textTaskHour, textTaskMinute, textTaskTitle, textPomodoroCount;
        private final ChipGroup chipGroupTags;
        private final Space spacerTagsEnd; // Изменен ID, чтобы не конфликтовать с дашбордом
        private final FrameLayout pomodoroContainer;
        // private final LinearLayout layoutTimeDisplay; // Не используется напрямую в bind

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
            textPomodoroCount = itemView.findViewById(R.id.text_pomodoro_count_planning);
            // layoutTimeDisplay = itemView.findViewById(R.id.layout_task_time_display_planning);
        }

        public void bind(final CalendarTaskSummary task, final OnPlanningTaskItemClickListener listener, CalendarPlanningViewModel viewModel) {
            Context context = itemView.getContext();
            textTaskTitle.setText(task.getTitle());

            LocalTime dueTime = task.getDueDate().toLocalTime();
            textTaskHour.setText(String.format(Locale.getDefault(), "%02d", dueTime.getHour()));
            textTaskMinute.setText(String.format(Locale.getDefault(), "%02d", dueTime.getMinute()));

            // Преобразование Compose Color в Android Color
            android.graphics.Color priorityAndroidColor = viewModel.getPriorityColor(task.getPriority());
            Drawable priorityBg = priorityIndicator.getBackground();
            if (priorityBg instanceof GradientDrawable) {
                ((GradientDrawable) priorityBg.mutate()).setColor(priorityAndroidColor.toArgb());
            } else {
                priorityIndicator.setBackgroundColor(priorityAndroidColor.toArgb());
            }


            boolean isChecked = task.getStatus() == TaskStatus.DONE;
            cardView.setAlpha(isChecked ? 0.7f : 1f);
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
            if (task.getTags() != null && !task.getTags().isEmpty()) {
                chipGroupTags.setVisibility(View.VISIBLE);
                spacerTagsEnd.setVisibility(View.GONE);
                for (Tag tag : task.getTags()) {
                    Chip chip = new Chip(context, null, R.style.TextAppearance_App_Chip);
                    chip.setText(tag.getName());
                    chip.setChipBackgroundColorResource(R.color.primaryContainerLight);
                    chip.setTextColor(ContextCompat.getColor(context, R.color.onPrimaryContainerLight));
                    // chip.setOnClickListener(v -> listener.onTagInCardClick(tag)); // Если нужен клик по тегу на карточке
                    chipGroupTags.addView(chip);
                }
            } else {
                chipGroupTags.setVisibility(View.GONE);
                spacerTagsEnd.setVisibility(View.VISIBLE);
            }

            cardView.setOnClickListener(v -> listener.onTaskCardClick(task));
            cardView.setOnLongClickListener(v -> {
                // Показываем контекстное меню или сразу Edit
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