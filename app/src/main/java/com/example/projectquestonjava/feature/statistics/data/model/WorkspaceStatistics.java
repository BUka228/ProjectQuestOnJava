package com.example.projectquestonjava.feature.statistics.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import com.example.projectquestonjava.core.data.model.core.Workspace;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
    private long workspaceId;

    @ColumnInfo(name = "total_tasks")
    private int totalTasks;

    @ColumnInfo(name = "completed_tasks")
    private int completedTasks;

    @ColumnInfo(name = "completion_rate")
    private float completionRate;

    @ColumnInfo(name = "last_active")
    private LocalDateTime lastActive;

    @ColumnInfo(name = "total_time_spent")
    private int totalTimeSpent;
}