package com.example.projectquestonjava.approach.eatTheFrog.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import com.example.projectquestonjava.core.data.model.core.Task; // Убедитесь, что Task.java уже создан

import lombok.Getter;

@Entity(
        tableName = "frog_params",
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
public class FrogParams {

    @Getter
    @ColumnInfo(name = "task_id")
    private final long taskId;

    @ColumnInfo(name = "is_frog")
    private final boolean isFrog;

    @Getter
    private final Difficulty difficulty;

    public FrogParams(long taskId, boolean isFrog, Difficulty difficulty) {
        this.taskId = taskId;
        this.isFrog = isFrog;
        this.difficulty = difficulty;
    }

    public boolean isFrog() {
        return isFrog;
    }

}