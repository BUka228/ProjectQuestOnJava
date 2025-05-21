package com.example.projectquestonjava.feature.statistics.data.model;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import com.example.projectquestonjava.core.data.model.core.Task;
import com.example.projectquestonjava.core.data.model.core.UserAuth;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
        }
)
public class TaskHistory {

    @PrimaryKey(autoGenerate = true)
    private int id; // В Kotlin был Int

    @ColumnInfo(name = "user_id", index = true)
    private int userId;

    @ColumnInfo(name = "task_id", index = true)
    private long taskId;

    @ColumnInfo(name = "changed_field")
    private String changedField;

    @Nullable
    @ColumnInfo(name = "old_value")
    private String oldValue;

    @Nullable
    @ColumnInfo(name = "new_value")
    private String newValue;

    @ColumnInfo(name = "changed_at")
    private LocalDateTime changedAt = LocalDateTime.now();
}