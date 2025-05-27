package com.example.projectquestonjava.feature.pomodoro.data.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.example.projectquestonjava.core.data.model.core.Task;
import com.example.projectquestonjava.core.data.model.core.UserAuth;
import com.example.projectquestonjava.feature.pomodoro.domain.model.SessionType;
import java.time.LocalDateTime;
import java.util.Objects;

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
    private final int userId;

    @ColumnInfo(name = "task_id")
    private final long taskId;

    @ColumnInfo(name = "start_time")
    private final LocalDateTime startTime;

    @ColumnInfo(name = "session_type")
    private final SessionType sessionType;

    @ColumnInfo(name = "planned_duration_seconds")
    private final int plannedDurationSeconds;

    @ColumnInfo(name = "actual_duration_seconds", defaultValue = "0")
    private int actualDurationSeconds; // Не final, так как обновляется

    @ColumnInfo(defaultValue = "0")
    private int interruptions; // Не final

    @ColumnInfo(defaultValue = "0")
    public boolean completed; // Не final

    // Основной конструктор для Room
    public PomodoroSession(long id, int userId, long taskId, LocalDateTime startTime,
                           SessionType sessionType, int plannedDurationSeconds,
                           int actualDurationSeconds, int interruptions, boolean completed) {
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

    @Ignore // Конструктор для создания нового объекта перед вставкой (без id)
    public PomodoroSession(int userId, long taskId, LocalDateTime startTime,
                           SessionType sessionType, int plannedDurationSeconds) {
        this(0, userId, taskId, startTime, sessionType, plannedDurationSeconds, 0, 0, false);
    }

    // Getters
    public long getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public long getTaskId() {
        return taskId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public int getPlannedDurationSeconds() {
        return plannedDurationSeconds;
    }

    public int getActualDurationSeconds() {
        return actualDurationSeconds;
    }

    public int getInterruptions() {
        return interruptions;
    }

    public boolean isCompleted() {
        return completed;
    }

    // Setters
    public void setId(long id) {
        this.id = id;
    }

    public void setActualDurationSeconds(int actualDurationSeconds) {
        this.actualDurationSeconds = actualDurationSeconds;
    }

    public void setInterruptions(int interruptions) {
        this.interruptions = interruptions;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PomodoroSession that = (PomodoroSession) o;
        return id == that.id && userId == that.userId && taskId == that.taskId && plannedDurationSeconds == that.plannedDurationSeconds && actualDurationSeconds == that.actualDurationSeconds && interruptions == that.interruptions && completed == that.completed && Objects.equals(startTime, that.startTime) && sessionType == that.sessionType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, taskId, startTime, sessionType, plannedDurationSeconds, actualDurationSeconds, interruptions, completed);
    }

    @NonNull
    @Override
    public String toString() {
        return "PomodoroSession{" +
                "id=" + getId() +
                ", userId=" + getUserId() +
                ", taskId=" + getTaskId() +
                ", startTime=" + getStartTime() +
                ", sessionType=" + getSessionType() +
                ", plannedDurationSeconds=" + getPlannedDurationSeconds() +
                ", actualDurationSeconds=" + getActualDurationSeconds() +
                ", interruptions=" + getInterruptions() +
                ", completed=" + isCompleted() +
                '}';
    }
}