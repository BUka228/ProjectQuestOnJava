package com.example.projectquestonjava.approach.eatTheFrog.data.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import com.example.projectquestonjava.core.data.model.core.Task;
import java.util.Objects;

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

    @ColumnInfo(name = "task_id")
    public final long taskId;

    @ColumnInfo(name = "is_frog")
    public final boolean isFrog;

    public final Difficulty difficulty;

    // Основной конструктор для Room
    public FrogParams(long taskId, boolean isFrog, Difficulty difficulty) {
        this.taskId = taskId;
        this.isFrog = isFrog;
        this.difficulty = difficulty;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FrogParams that = (FrogParams) o;
        return taskId == that.taskId && isFrog == that.isFrog && difficulty == that.difficulty;
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, isFrog, difficulty);
    }

    @NonNull
    @Override
    public String toString() {
        return "FrogParams{" +
                "taskId=" + taskId +
                ", isFrog=" + isFrog +
                ", difficulty=" + difficulty +
                '}';
    }
}