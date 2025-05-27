package com.example.projectquestonjava.approach.gtd.data.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import com.example.projectquestonjava.core.data.model.core.Task;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity(
        tableName = "gtd_params",
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
public class GTDParams {

    @ColumnInfo(name = "task_id")
    public final long taskId;

    public final String context;

    @ColumnInfo(name = "next_review_date")
    public final LocalDateTime nextReviewDate;

    @ColumnInfo(name = "is_scheduled")
    public final boolean isScheduled;

    // Основной конструктор для Room
    public GTDParams(long taskId, String context, LocalDateTime nextReviewDate, boolean isScheduled) {
        this.taskId = taskId;
        this.context = context;
        this.nextReviewDate = nextReviewDate;
        this.isScheduled = isScheduled;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GTDParams gtdParams = (GTDParams) o;
        return taskId == gtdParams.taskId && isScheduled == gtdParams.isScheduled && Objects.equals(context, gtdParams.context) && Objects.equals(nextReviewDate, gtdParams.nextReviewDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, context, nextReviewDate, isScheduled);
    }

    @NonNull
    @Override
    public String toString() {
        return "GTDParams{" +
                "taskId=" + taskId +
                ", context='" + context + '\'' +
                ", nextReviewDate=" + nextReviewDate +
                ", isScheduled=" + isScheduled +
                '}';
    }
}