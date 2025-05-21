package com.example.projectquestonjava.feature.statistics.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.annotation.Nullable;
import com.example.projectquestonjava.core.data.model.core.Task;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(
        tableName = "task_statistics",
        primaryKeys = {"task_id"},
        foreignKeys = {
                @ForeignKey(
                        entity = Task.class,
                        parentColumns = {"id"},
                        childColumns = {"task_id"},
                        onDelete = ForeignKey.CASCADE
                )
        }
)
public class TaskStatistics {

    @ColumnInfo(name = "task_id")
    private long taskId;

    @Nullable
    @ColumnInfo(name = "completion_time")
    private LocalDateTime completionTime;

    @ColumnInfo(name = "time_spent_seconds", defaultValue = "0")
    private int timeSpentSeconds = 0;

    @ColumnInfo(name = "total_pomodoro_focus_seconds", defaultValue = "0")
    private int totalPomodoroFocusSeconds = 0;

    @ColumnInfo(name = "completed_pomodoro_focus_sessions", defaultValue = "0")
    private int completedPomodoroFocusSessions = 0;

    @ColumnInfo(name = "total_pomodoro_interruptions", defaultValue = "0")
    private int totalPomodoroInterruptions = 0;

    @ColumnInfo(name = "was_completed_once", defaultValue = "0")
    private boolean wasCompletedOnce = false;
}