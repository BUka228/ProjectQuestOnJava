package com.example.projectquestonjava.core.data.model.core;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import java.util.Objects;

@Entity(
        tableName = "subtask_relation",
        primaryKeys = {"parent_task_id", "child_task_id"},
        foreignKeys = {
                @ForeignKey(
                        entity = Task.class,
                        parentColumns = {"id"},
                        childColumns = {"parent_task_id"},
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = Task.class,
                        parentColumns = {"id"},
                        childColumns = {"child_task_id"},
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index("parent_task_id"), @Index("child_task_id")}
)
public class SubtaskRelation {

    @ColumnInfo(name = "parent_task_id")
    private final long parentTaskId;

    @ColumnInfo(name = "child_task_id")
    private final long childTaskId;

    @ColumnInfo(name = "`order`") // `order` - зарезервированное слово, нужно экранировать для SQL
    private final int order;

    public SubtaskRelation(long parentTaskId, long childTaskId, int order) {
        this.parentTaskId = parentTaskId;
        this.childTaskId = childTaskId;
        this.order = order;
    }

    public long getParentTaskId() {
        return parentTaskId;
    }

    public long getChildTaskId() {
        return childTaskId;
    }

    public int getOrder() {
        return order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubtaskRelation that = (SubtaskRelation) o;
        return parentTaskId == that.parentTaskId && childTaskId == that.childTaskId && order == that.order;
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentTaskId, childTaskId, order);
    }

    @NonNull
    @Override
    public String toString() {
        return "SubtaskRelation{" +
                "parentTaskId=" + parentTaskId +
                ", childTaskId=" + childTaskId +
                ", order=" + order +
                '}';
    }
}