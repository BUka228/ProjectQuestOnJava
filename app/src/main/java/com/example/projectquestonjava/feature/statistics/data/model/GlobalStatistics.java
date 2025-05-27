package com.example.projectquestonjava.feature.statistics.data.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.example.projectquestonjava.core.data.model.core.UserAuth;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity(
        tableName = "global_statistics",
        foreignKeys = {
                @ForeignKey(
                        entity = UserAuth.class,
                        parentColumns = {"id"},
                        childColumns = {"user_id"},
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index(value = {"user_id"}, unique = true)}
)
public class GlobalStatistics {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "user_id")
    private final int userId;

    @ColumnInfo(name = "total_workspaces")
    private final int totalWorkspaces;

    @ColumnInfo(name = "total_tasks")
    private final int totalTasks;

    @ColumnInfo(name = "completed_tasks")
    private final int completedTasks;

    @ColumnInfo(name = "total_time_spent")
    private final int totalTimeSpent; // Предполагаем минуты

    @ColumnInfo(name = "last_active")
    private final LocalDateTime lastActive;

    // Основной конструктор для Room
    public GlobalStatistics(long id, int userId, int totalWorkspaces, int totalTasks,
                            int completedTasks, int totalTimeSpent, LocalDateTime lastActive) {
        this.id = id;
        this.userId = userId;
        this.totalWorkspaces = totalWorkspaces;
        this.totalTasks = totalTasks;
        this.completedTasks = completedTasks;
        this.totalTimeSpent = totalTimeSpent;
        this.lastActive = lastActive;
    }

    @Ignore
    public GlobalStatistics(int userId, int totalWorkspaces, int totalTasks,
                            int completedTasks, int totalTimeSpent, LocalDateTime lastActive) {
        this(0, userId, totalWorkspaces, totalTasks, completedTasks, totalTimeSpent, lastActive);
    }

    // Геттеры и сеттеры
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public int getTotalWorkspaces() {
        return totalWorkspaces;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public int getTotalTimeSpent() {
        return totalTimeSpent;
    }

    public LocalDateTime getLastActive() {
        return lastActive;
    }

    // Сеттеры для полей, которые могут быть изменены
    // (totalWorkspaces, totalTasks, completedTasks, totalTimeSpent, lastActive)
    // В данном случае поля объявлены как final, поэтому сеттеры не нужны,
    // так как значения устанавливаются только через конструктор.
    // Если бы поля не были final, здесь были бы сеттеры.
    // Например: public void setTotalWorkspaces(int totalWorkspaces) { this.totalWorkspaces = totalWorkspaces; }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GlobalStatistics that = (GlobalStatistics) o;
        return id == that.id && userId == that.userId && totalWorkspaces == that.totalWorkspaces && totalTasks == that.totalTasks && completedTasks == that.completedTasks && totalTimeSpent == that.totalTimeSpent && Objects.equals(lastActive, that.lastActive);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, totalWorkspaces, totalTasks, completedTasks, totalTimeSpent, lastActive);
    }

    @NonNull
    @Override
    public String toString() {
        return "GlobalStatistics{" +
                "id=" + id +
                ", userId=" + userId +
                ", totalWorkspaces=" + totalWorkspaces +
                ", totalTasks=" + totalTasks +
                ", completedTasks=" + completedTasks +
                ", totalTimeSpent=" + totalTimeSpent +
                ", lastActive=" + lastActive +
                '}';
    }
}