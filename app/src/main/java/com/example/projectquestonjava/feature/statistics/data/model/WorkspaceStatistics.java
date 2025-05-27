package com.example.projectquestonjava.feature.statistics.data.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import com.example.projectquestonjava.core.data.model.core.Workspace;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity(
        tableName = "workspace_statistics",
        primaryKeys = {"workspace_id"},
        foreignKeys = {
                @ForeignKey(
                        entity = Workspace.class,
                        parentColumns = {"id"},
                        childColumns = {"workspace_id"},
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index("workspace_id")}
)
public class WorkspaceStatistics {

    @ColumnInfo(name = "workspace_id")
    public final long workspaceId;

    @ColumnInfo(name = "total_tasks")
    public final int totalTasks;

    @ColumnInfo(name = "completed_tasks")
    public final int completedTasks;

    @ColumnInfo(name = "completion_rate")
    public final float completionRate;

    @ColumnInfo(name = "last_active")
    public final LocalDateTime lastActive;

    @ColumnInfo(name = "total_time_spent")
    public final int totalTimeSpent; // Секунды или минуты? В Kotlin было Int, предположим минуты

    // Основной конструктор для Room
    public WorkspaceStatistics(long workspaceId, int totalTasks, int completedTasks,
                               float completionRate, LocalDateTime lastActive, int totalTimeSpent) {
        this.workspaceId = workspaceId;
        this.totalTasks = totalTasks;
        this.completedTasks = completedTasks;
        this.completionRate = completionRate;
        this.lastActive = lastActive;
        this.totalTimeSpent = totalTimeSpent;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkspaceStatistics that = (WorkspaceStatistics) o;
        return workspaceId == that.workspaceId && totalTasks == that.totalTasks && completedTasks == that.completedTasks && Float.compare(that.completionRate, completionRate) == 0 && totalTimeSpent == that.totalTimeSpent && Objects.equals(lastActive, that.lastActive);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workspaceId, totalTasks, completedTasks, completionRate, lastActive, totalTimeSpent);
    }

    @NonNull
    @Override
    public String toString() {
        return "WorkspaceStatistics{" +
                "workspaceId=" + workspaceId +
                ", totalTasks=" + totalTasks +
                ", completedTasks=" + completedTasks +
                ", completionRate=" + completionRate +
                ", lastActive=" + lastActive +
                ", totalTimeSpent=" + totalTimeSpent +
                '}';
    }
}