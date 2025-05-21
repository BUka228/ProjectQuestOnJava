package com.example.projectquestonjava.core.data.model.core;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.time.LocalDateTime;

@Entity(
        tableName = "workspace",
        foreignKeys = {
                @ForeignKey(
                        entity = Approach.class,
                        parentColumns = "id",
                        childColumns = "approach_id",
                        onDelete = ForeignKey.RESTRICT
                ),
                @ForeignKey(
                        entity = UserAuth.class,
                        parentColumns = "id",
                        childColumns = "user_id",
                        onDelete = ForeignKey.RESTRICT // Обычно не хотим удалять пользователя при удалении Workspace
                )
        },
        indices = {
                @Index("approach_id"),
                @Index(value = {"user_id", "name"})
        }
)
public class Workspace {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "user_id", index = true)
    private int userId;

    private String name;
    private String description;

    @ColumnInfo(name = "approach_id")
    private long approachId;

    @ColumnInfo(name = "created_at")
    private LocalDateTime createdAt;

    @ColumnInfo(name = "updated_at")
    private LocalDateTime updatedAt;

    public Workspace(long id, int userId, String name, String description, long approachId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.approachId = approachId;
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

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getApproachId() {
        return approachId;
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

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setApproachId(long approachId) {
        this.approachId = approachId;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}