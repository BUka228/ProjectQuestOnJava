package com.example.projectquestonjava.approach.eisenhower.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import com.example.projectquestonjava.core.data.model.core.Task;
import com.example.projectquestonjava.core.data.model.enums.Priority;

@Entity(
        tableName = "eisenhower_params",
        primaryKeys = {"task_id"},
        foreignKeys = {
                @ForeignKey(
                        entity = Task.class,
                        parentColumns = {"id"},
                        childColumns = {"task_id"},
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index("task_id")}
)
public class EisenhowerParams {

    @ColumnInfo(name = "task_id")
    private final long taskId;

    private final Priority urgency;
    private final Priority importance;

    public EisenhowerParams(long taskId, Priority urgency, Priority importance) {
        this.taskId = taskId;
        this.urgency = urgency;
        this.importance = importance;
    }

    public long getTaskId() {
        return taskId;
    }

    public Priority getUrgency() {
        return urgency;
    }

    public Priority getImportance() {
        return importance;
    }
}