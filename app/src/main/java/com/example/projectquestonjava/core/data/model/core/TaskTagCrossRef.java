package com.example.projectquestonjava.core.data.model.core;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
        tableName = "task_tag_cross_ref",
        primaryKeys = {"task_id", "tag_id"},
        foreignKeys = {
                @ForeignKey(
                        entity = Task.class,
                        parentColumns = {"id"},
                        childColumns = {"task_id"},
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = Tag.class,
                        parentColumns = {"id"},
                        childColumns = {"tag_id"},
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index(value = {"task_id"}), @Index(value = {"tag_id"})}
)
public class TaskTagCrossRef {

    @ColumnInfo(name = "task_id")
    private final long taskId;

    @ColumnInfo(name = "tag_id")
    private final long tagId;

    public TaskTagCrossRef(long taskId, long tagId) {
        this.taskId = taskId;
        this.tagId = tagId;
    }

    public long getTaskId() {
        return taskId;
    }

    public long getTagId() {
        return tagId;
    }
}
