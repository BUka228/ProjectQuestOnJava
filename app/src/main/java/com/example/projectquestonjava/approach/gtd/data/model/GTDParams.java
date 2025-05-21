package com.example.projectquestonjava.approach.gtd.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import com.example.projectquestonjava.core.data.model.core.Task;
import java.time.LocalDateTime;

import lombok.Getter;

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

    @Getter
    @ColumnInfo(name = "task_id")
    private final long taskId;

    @Getter
    private final String context;

    @Getter
    @ColumnInfo(name = "next_review_date")
    private final LocalDateTime nextReviewDate;

    @ColumnInfo(name = "is_scheduled")
    private final boolean isScheduled;

    public GTDParams(long taskId, String context, LocalDateTime nextReviewDate, boolean isScheduled) {
        this.taskId = taskId;
        this.context = context;
        this.nextReviewDate = nextReviewDate;
        this.isScheduled = isScheduled;
    }

    public boolean isScheduled() {
        return isScheduled;
    }
}