package com.example.projectquestonjava.feature.pomodoro.presentation.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.core.data.model.core.Task;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

public class TaskSelectionAdapter extends ListAdapter<Task, TaskSelectionAdapter.TaskSelectionViewHolder> {

    private final OnTaskSelectedListener listener;
    private long currentSelectedTaskId = -1L; // ID текущей выбранной задачи в Pomodoro
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault());


    public interface OnTaskSelectedListener {
        void onTaskSelected(Task task);
    }

    public TaskSelectionAdapter(@NonNull OnTaskSelectedListener listener) {
        super(TASK_DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setCurrentTaskId(long taskId) {
        if (currentSelectedTaskId != taskId) {
            long oldSelectedId = currentSelectedTaskId;
            currentSelectedTaskId = taskId;
            // Уведомляем об изменении для старой и новой выбранной задачи, если они видимы
            // Это более эффективно, чем notifyDataSetChanged()
            int oldPosition = findPositionById(oldSelectedId);
            if (oldPosition != -1) notifyItemChanged(oldPosition);
            int newPosition = findPositionById(currentSelectedTaskId);
            if (newPosition != -1) notifyItemChanged(newPosition);
        }
    }

    private int findPositionById(long taskId) {
        if (taskId == -1L) return -1;
        for (int i = 0; i < getItemCount(); i++) {
            if (getItem(i).getId() == taskId) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public TaskSelectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_selection_pomodoro, parent, false);
        return new TaskSelectionViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskSelectionViewHolder holder, int position) {
        Task task = getItem(position);
        holder.bind(task, task.getId() == currentSelectedTaskId, listener, dateFormatter);
    }

    static class TaskSelectionViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final TextView dueDateTextView;
        private final ImageView selectedCheckImageView;

        public TaskSelectionViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.textView_task_item_title);
            dueDateTextView = itemView.findViewById(R.id.textView_task_item_due_date);
            selectedCheckImageView = itemView.findViewById(R.id.imageView_task_item_selected_check);
        }

        void bind(final Task task, boolean isSelected, final OnTaskSelectedListener listener, DateTimeFormatter formatter) {
            titleTextView.setText(task.getTitle());
            if (task.getDueDate() != null) {
                dueDateTextView.setText("Срок: " + task.getDueDate().toLocalDate().format(formatter));
                dueDateTextView.setVisibility(View.VISIBLE);
            } else {
                dueDateTextView.setVisibility(View.GONE);
            }

            selectedCheckImageView.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            titleTextView.setTypeface(null, isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

            itemView.setOnClickListener(v -> listener.onTaskSelected(task));
        }
    }

    private static final DiffUtil.ItemCallback<Task> TASK_DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Task>() {
                @Override
                public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
                    return oldItem.getTitle().equals(newItem.getTitle()) &&
                            Objects.equals(oldItem.getDueDate(), newItem.getDueDate());
                    // Добавьте другие поля, если они влияют на отображение в этом списке
                }
            };
}