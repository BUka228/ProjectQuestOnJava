package com.example.projectquestonjava.feature.statistics.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.example.projectquestonjava.core.data.model.core.Task;
import com.example.projectquestonjava.core.data.model.core.UserAuth;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity(
        tableName = "task_history",
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
        // Индексы уже есть в ForeignKey
        indices = {@androidx.room.Index(value = {"task_id"}), @androidx.room.Index(value = {"user_id"})}
)
public class TaskHistory {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "user_id")
    public final int userId;

    @ColumnInfo(name = "task_id")
    public final long taskId;

    @ColumnInfo(name = "changed_field")
    public final String changedField;

    @Nullable
    @ColumnInfo(name = "old_value")
    public final String oldValue;

    @Nullable
    @ColumnInfo(name = "new_value")
    public final String newValue;

    @ColumnInfo(name = "changed_at")
    public final LocalDateTime changedAt;

    // Основной конструктор для Room
    public TaskHistory(int id, int userId, long taskId, String changedField,
                       @Nullable String oldValue, @Nullable String newValue, LocalDateTime changedAt) {
        this.id = id;
        this.userId = userId;
        this.taskId = taskId;
        this.changedField = changedField;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changedAt = changedAt != null ? changedAt : LocalDateTime.now();
    }

    @Ignore
    public TaskHistory(int userId, long taskId, String changedField,
                       @Nullable String oldValue, @Nullable String newValue) {
        this(0, userId, taskId, changedField, oldValue, newValue, LocalDateTime.now());
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskHistory that = (TaskHistory) o;
        return id == that.id && userId == that.userId && taskId == that.taskId && Objects.equals(changedField, that.changedField) && Objects.equals(oldValue, that.oldValue) && Objects.equals(newValue, that.newValue) && Objects.equals(changedAt, that.changedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, taskId, changedField, oldValue, newValue, changedAt);
    }

    @NonNull
    @Override
    public String toString() {
        return "TaskHistory{" +
                "id=" + id +
                ", userId=" + userId +
                ", taskId=" + taskId +
                ", changedField='" + changedField + '\'' +
                ", oldValue='" + oldValue + '\'' +
                ", newValue='" + newValue + '\'' +
                ", changedAt=" + changedAt +
                '}';
    }
}