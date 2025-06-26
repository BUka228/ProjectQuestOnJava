package com.example.projectquestonjava.core.data.model.core;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.example.projectquestonjava.core.data.model.enums.TaskStatus;
import java.time.LocalDateTime;

@Entity(
        tableName = "task",
        foreignKeys = {
                @ForeignKey(
                        entity = Workspace.class,
                        parentColumns = "id",
                        childColumns = "workspace_id",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = UserAuth.class,
                        parentColumns = "id",
                        childColumns = "user_id",
                        onDelete = ForeignKey.RESTRICT
                )
        },
        indices = {
                @Index("due_date"),
                @Index("workspace_id"),
                @Index(value = {"user_id", "status"}),
                @Index(value = {"user_id", "workspace_id", "due_date"})
        }
)
public class Task {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "user_id", index = true)
    private int userId;

    @ColumnInfo(name = "workspace_id")
    private long workspaceId;

    private String title;
    private String description;

    @ColumnInfo(name = "due_date")
    private LocalDateTime dueDate;

    private TaskStatus status;

    @ColumnInfo(name = "created_at")
    private LocalDateTime createdAt;

    @ColumnInfo(name = "updated_at")
    private LocalDateTime updatedAt;

    public Task(long id, int userId, long workspaceId, String title, String description, LocalDateTime dueDate, TaskStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.workspaceId = workspaceId;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters
    public long getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public long getWorkspaceId() {
        return workspaceId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Setters
    public void setId(long id) {
        this.id = id;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setWorkspaceId(long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}