package com.example.projectquestonjava.feature.statistics.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.example.projectquestonjava.core.data.model.core.UserAuth;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
    private int userId;

    @ColumnInfo(name = "total_workspaces")
    private int totalWorkspaces;

    @ColumnInfo(name = "total_tasks")
    private int totalTasks;

    @ColumnInfo(name = "completed_tasks")
    private int completedTasks;

    @ColumnInfo(name = "total_time_spent")
    private int totalTimeSpent; // Предполагаем, что это минуты или секунды, но не LocalDateTime

    @ColumnInfo(name = "last_active")
    private LocalDateTime lastActive;
}