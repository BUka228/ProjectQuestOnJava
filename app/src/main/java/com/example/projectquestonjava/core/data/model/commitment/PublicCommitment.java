package com.example.projectquestonjava.core.data.model.commitment;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.example.projectquestonjava.core.data.model.core.Task;
import com.example.projectquestonjava.core.data.model.core.UserAuth;
import java.time.LocalDateTime;
import java.util.Objects;

import lombok.Setter;

@Entity(
        tableName = "public_commitment",
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
        },
        indices = {@Index("task_id"), @Index("user_id")}
)
public class PublicCommitment {

    // Сеттер для id, если Room его требует для autoGenerate
    @Setter
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "user_id")
    private final int userId;

    @ColumnInfo(name = "task_id")
    private final long taskId;

    @ColumnInfo(name = "commitment_text")
    private final String commitmentText;

    @ColumnInfo(name = "created_at")
    private final LocalDateTime createdAt;

    @ColumnInfo(name = "updated_at")
    private final LocalDateTime updatedAt;

    public PublicCommitment(int id, int userId, long taskId, String commitmentText, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.taskId = taskId;
        this.commitmentText = commitmentText;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }
    // Конструктор без id для Room
    public PublicCommitment(int userId, long taskId, String commitmentText, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(0, userId, taskId, commitmentText, createdAt, updatedAt);
    }


    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public long getTaskId() {
        return taskId;
    }

    public String getCommitmentText() {
        return commitmentText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublicCommitment that = (PublicCommitment) o;
        return id == that.id && userId == that.userId && taskId == that.taskId && Objects.equals(commitmentText, that.commitmentText) && Objects.equals(createdAt, that.createdAt) && Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, taskId, commitmentText, createdAt, updatedAt);
    }

    @NonNull
    @Override
    public String toString() {
        return "PublicCommitment{" +
                "id=" + id +
                ", userId=" + userId +
                ", taskId=" + taskId +
                ", commitmentText='" + commitmentText + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}