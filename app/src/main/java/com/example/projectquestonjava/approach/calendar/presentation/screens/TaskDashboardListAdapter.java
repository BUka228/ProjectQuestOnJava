package com.example.projectquestonjava.approach.calendar.presentation.screens;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.approach.calendar.domain.model.CalendarTaskSummary;
import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.core.data.model.enums.Priority;
import com.example.projectquestonjava.core.data.model.enums.TaskStatus;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

public class TaskDashboardListAdapter extends ListAdapter<CalendarTaskSummary, TaskDashboardListAdapter.TaskViewHolder> {

    private final OnTaskItemClickListener listener;
    // DateTimeFormatter не нужен здесь, т.к. часы и минуты разделены

    public interface OnTaskItemClickListener {
        void onTaskClick(CalendarTaskSummary task); // Клик по всей карточке для деталей
        void onTaskCheckedChange(CalendarTaskSummary task, boolean isChecked);

        void onEditTask(CalendarTaskSummary task);

        void onPomodoroStart(CalendarTaskSummary task);

        // onEditTask и onPomodoroStart будут вызываться из BottomSheet или контекстного меню
        void onTagClick(Tag tag);
    }

    public TaskDashboardListAdapter(OnTaskItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_dashboard, parent, false);
        return new TaskViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        CalendarTaskSummary currentTask = getItem(position);
        holder.bind(currentTask, listener);
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView textTaskHour, textTaskMinute, textTaskTitle, textPomodoroCount;
        private final View viewPriorityIndicator;
        private final CheckBox checkboxTaskDone;
        private final FrameLayout pomodoroCounterContainer;
        private final ImageView iconTaskRecurrence, imageViewPomodoroTimerIcon;
        private final ChipGroup chipGroupTaskTags;
        private final Space spacerTagsEnd;
        private final LinearLayout layoutTaskTimePriority;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_task_dashboard_item);
            textTaskHour = itemView.findViewById(R.id.text_task_hour);
            textTaskMinute = itemView.findViewById(R.id.text_task_minute);
            textTaskTitle = itemView.findViewById(R.id.text_task_title);
            viewPriorityIndicator = itemView.findViewById(R.id.view_priority_indicator);
            checkboxTaskDone = itemView.findViewById(R.id.checkbox_task_done);
            pomodoroCounterContainer = itemView.findViewById(R.id.pomodoro_counter_container);
            textPomodoroCount = itemView.findViewById(R.id.text_pomodoro_count);
            imageViewPomodoroTimerIcon = itemView.findViewById(R.id.imageView_pomodoro_timer_icon); // Для фона Pomodoro
            iconTaskRecurrence = itemView.findViewById(R.id.icon_task_recurrence);
            chipGroupTaskTags = itemView.findViewById(R.id.chip_group_task_tags);
            spacerTagsEnd = itemView.findViewById(R.id.spacer_tags_end);
            layoutTaskTimePriority = itemView.findViewById(R.id.layout_task_time_priority);
        }

        public void bind(CalendarTaskSummary task, OnTaskItemClickListener listener) {
            Context context = itemView.getContext();

            // --- Настройка времени ---
            LocalTime dueTime = task.getDueDate().toLocalTime();
            textTaskHour.setText(String.format(Locale.getDefault(), "%02d", dueTime.getHour()));
            textTaskMinute.setText(String.format(Locale.getDefault(), "%02d", dueTime.getMinute()));

            // --- Настройка приоритета ---
            boolean isChecked = task.getStatus() == TaskStatus.DONE;
            int priorityColor = getPriorityColor(context, task.getPriority(), isChecked);
            // Применение цвета к viewPriorityIndicator (предполагается, что это ShapeDrawable)
            Drawable priorityBackground = viewPriorityIndicator.getBackground();
            if (priorityBackground instanceof GradientDrawable) {
                ((GradientDrawable) priorityBackground.mutate()).setColorFilter(new PorterDuffColorFilter(priorityColor, PorterDuff.Mode.SRC_IN));
            } else {
                viewPriorityIndicator.setBackgroundColor(priorityColor);
            }
            // Анимация пульсации для CRITICAL/HIGH (сложнее в XML, можно опустить или использовать ViewPropertyAnimator)
            // textTaskHour.setTextColor(isChecked ? priorityColorMuted : ContextCompat.getColor(context, R.color.onSurfaceLight));
            // textTaskMinute.setTextColor(isChecked ? priorityColorMuted : ContextCompat.getColor(context, R.color.onSurfaceLight));
            // Для простоты цвет времени пока не меняем в зависимости от isChecked

            // --- Статус задачи (чекбокс и текст) ---
            checkboxTaskDone.setChecked(isChecked);
            if (isChecked) {
                textTaskTitle.setPaintFlags(textTaskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                cardView.setAlpha(0.65f);
                // cardView.setCardElevation(context.getResources().getDimensionPixelSize(R.dimen.card_elevation_done)); // Пример
            } else {
                textTaskTitle.setPaintFlags(textTaskTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                cardView.setAlpha(1f);
                // cardView.setCardElevation(context.getResources().getDimensionPixelSize(R.dimen.card_elevation_default));
            }
            textTaskTitle.setText(task.getTitle());

            // --- Pomodoro Counter ---
            if (task.getPomodoroCount() > 0) {
                pomodoroCounterContainer.setVisibility(View.VISIBLE);
                textPomodoroCount.setText(String.valueOf(task.getPomodoroCount()));
                // imageViewPomodoroTimerIcon.setAlpha(0.2f); // Как в Compose (фон)
                pomodoroCounterContainer.setOnClickListener(v -> {
                    // В Compose был Animatable для scale, здесь можно пропустить или сделать через ObjectAnimator
                    //listener.onPomodoroStart(task); // В ViewModel будет логика навигации
                });
            } else {
                pomodoroCounterContainer.setVisibility(View.GONE);
            }

            // --- Иконка повторения ---
            iconTaskRecurrence.setVisibility(task.getRecurrenceRule() != null ? View.VISIBLE : View.GONE);

            // --- Теги ---
            chipGroupTaskTags.removeAllViews(); // Очищаем перед добавлением новых
            if (task.getTags() != null && !task.getTags().isEmpty()) {
                chipGroupTaskTags.setVisibility(View.VISIBLE);
                spacerTagsEnd.setVisibility(View.GONE);
                for (Tag tag : task.getTags()) {
                    Chip chip = new Chip(context); // Используем com.google.android.material.chip.Chip
                    chip.setText(tag.getName());
                    // Стилизация чипа (цвета, размеры) - лучше через XML стиль Widget.App.Chip
                    chip.setChipBackgroundColorResource(R.color.primaryContainerLight); // Пример, лучше из атрибутов темы
                    chip.setTextColor(ContextCompat.getColor(context, R.color.onPrimaryContainerLight));
                    // chip.setChipMinHeight(context.getResources().getDimensionPixelSize(R.dimen.chip_min_height_small));
                    // chip.setTextAppearance(R.style.TextAppearance_App_Chip);
                    chip.setEnsureMinTouchTargetSize(false); // Как в Compose
                    chip.setClickable(true);
                    chip.setFocusable(true);
                    chip.setOnClickListener(v -> listener.onTagClick(tag));
                    chipGroupTaskTags.addView(chip);
                }
            } else {
                chipGroupTaskTags.setVisibility(View.GONE);
                spacerTagsEnd.setVisibility(View.VISIBLE);
            }

            // --- Обработчики кликов ---
            cardView.setOnClickListener(v -> listener.onTaskClick(task));
            // Обработчик чекбокса, чтобы реагировать только на действия пользователя
            checkboxTaskDone.setOnCheckedChangeListener(null); // Сначала очищаем, чтобы избежать лишних вызовов при bind
            checkboxTaskDone.setChecked(isChecked);
            checkboxTaskDone.setOnCheckedChangeListener((buttonView, isNowChecked) -> {
                if (buttonView.isPressed()) { // Реагируем только если кликнул пользователь
                    listener.onTaskCheckedChange(task, isNowChecked);
                }
            });
        }

        private int getPriorityColor(Context context, Priority priority, boolean isComplete) {
            int colorRes;
            if (priority == null) priority = Priority.LOW;

            if (isComplete) {
                colorRes = R.color.secondaryLight; // Зеленый для выполненных
                // Применяем alpha к цвету
                int baseColor = ContextCompat.getColor(context, colorRes);
                return Color.argb(Math.round(Color.alpha(baseColor) * 0.6f), Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor));
            }

            switch (priority) {
                case CRITICAL: colorRes = R.color.errorLight; break;
                case HIGH:     colorRes = R.color.tertiaryLight; break; // Оранжевый для высокого
                case MEDIUM:   colorRes = R.color.primaryLight; break;   // Основной для среднего
                case LOW:
                default:       colorRes = R.color.primaryContainerLight; break; // Менее заметный для низкого
            }
            // Для невыполненных задач альфа не меняем (или делаем 1.0f)
            return ContextCompat.getColor(context, colorRes);
        }
    }

    private static final DiffUtil.ItemCallback<CalendarTaskSummary> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CalendarTaskSummary>() {
                @Override
                public boolean areItemsTheSame(@NonNull CalendarTaskSummary oldItem, @NonNull CalendarTaskSummary newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull CalendarTaskSummary oldItem, @NonNull CalendarTaskSummary newItem) {
                    return Objects.equals(oldItem.getTitle(), newItem.getTitle()) &&
                            oldItem.getStatus().equals(newItem.getStatus()) &&
                            oldItem.getPriority().equals(newItem.getPriority()) &&
                            oldItem.getDueDate().equals(newItem.getDueDate()) &&
                            oldItem.getPomodoroCount() == newItem.getPomodoroCount() &&
                            Objects.equals(oldItem.getRecurrenceRule(), newItem.getRecurrenceRule()) &&
                            Objects.equals(oldItem.getTags(), newItem.getTags()) &&
                            Objects.equals(oldItem.getSubtaskProgress(), newItem.getSubtaskProgress());
                }
            };
}