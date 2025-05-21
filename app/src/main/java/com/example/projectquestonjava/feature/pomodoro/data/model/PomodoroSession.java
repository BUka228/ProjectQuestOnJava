package com.example.projectquestonjava.feature.pomodoro.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.example.projectquestonjava.core.data.model.core.Task;
import com.example.projectquestonjava.core.data.model.core.UserAuth;
import com.example.projectquestonjava.feature.pomodoro.domain.model.SessionType;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity(
        tableName = "pomodoro_session",
        foreignKeys = {
                @ForeignKey(
                        entity = Task.class,
                        parentColumns = {"id"},
                        childColumns = {"task_id"},
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = UserAuth.class,
                        parentColumns = {"id"},
                        childColumns = {"user_id"},
                        onDelete = ForeignKey.RESTRICT
                )
        },
        indices = {@Index("user_id"), @Index("task_id")}
)
public class PomodoroSession {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "user_id")
    private int userId;

    @ColumnInfo(name = "task_id")
    private long taskId;

    @ColumnInfo(name = "start_time")
    private LocalDateTime startTime;

    @ColumnInfo(name = "session_type")
    private SessionType sessionType;

    @ColumnInfo(name = "planned_duration_seconds")
    private int plannedDurationSeconds;

    @ColumnInfo(name = "actual_duration_seconds", defaultValue = "0")
    private int actualDurationSeconds = 0;

    @ColumnInfo(defaultValue = "0")
    private int interruptions = 0;

    @ColumnInfo(defaultValue = "0")
    private boolean completed = false;


    public PomodoroSession(long id, int userId, long taskId, LocalDateTime startTime, SessionType sessionType, int plannedDurationSeconds, int actualDurationSeconds, int interruptions, boolean completed) {
        this.id = id;
        this.userId = userId;
        this.taskId = taskId;
        this.startTime = startTime;
        this.sessionType = sessionType;
        this.plannedDurationSeconds = plannedDurationSeconds;
        this.actualDurationSeconds = actualDurationSeconds;
        this.interruptions = interruptions;
        this.completed = completed;
    }
    public PomodoroSession(int userId, long taskId, LocalDateTime startTime, SessionType sessionType, int plannedDurationSeconds) {
        this.userId = userId;
        this.taskId = taskId;
        this.startTime = startTime;
        this.sessionType = sessionType;
        this.plannedDurationSeconds = plannedDurationSeconds;
        this.actualDurationSeconds = 0;
        this.interruptions = 0;
        this.completed = false;
    }
}