package com.example.projectquestonjava.feature.gamification.data.model;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

@Entity(
        tableName = "surprise_task",
        foreignKeys = {
                @ForeignKey(
                        entity = Gamification.class,
                        parentColumns = {"id"},
                        childColumns = {"gamification_id"},
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = Reward.class,
                        parentColumns = {"id"},
                        childColumns = {"reward_id"},
                        onDelete = ForeignKey.RESTRICT
                )
        },
        indices = {@Index("gamification_id"), @Index("reward_id")}
)
public class SurpriseTask {

    @Setter
    @Getter
    @PrimaryKey(autoGenerate = true)
    private long id;

    @Setter
    @Getter
    @ColumnInfo(name = "gamification_id")
    private long gamificationId;

    @Setter
    @Getter
    private String description;

    @Setter
    @Getter
    @ColumnInfo(name = "reward_id")
    private long rewardId;

    @Setter
    @Getter
    @ColumnInfo(name = "expiration_time")
    private LocalDateTime expirationTime;

    @ColumnInfo(name = "is_completed")
    private boolean isCompleted;

    @Nullable
    @ColumnInfo(name = "shown_date")
    private LocalDate shownDate;

    public SurpriseTask(long id, long gamificationId, String description, long rewardId, LocalDateTime expirationTime, boolean isCompleted, @Nullable LocalDate shownDate) {
        this.id = id;
        this.gamificationId = gamificationId;
        this.description = description;
        this.rewardId = rewardId;
        this.expirationTime = expirationTime;
        this.isCompleted = isCompleted;
        this.shownDate = shownDate;
    }

    // Конструктор для Room
    public SurpriseTask(long gamificationId, String description, long rewardId, LocalDateTime expirationTime, boolean isCompleted, @Nullable LocalDate shownDate) {
        this(0, gamificationId, description, rewardId, expirationTime, isCompleted, shownDate);
    }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
    @Nullable
    public LocalDate getShownDate() { return shownDate; }
    public void setShownDate(@Nullable LocalDate shownDate) { this.shownDate = shownDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SurpriseTask that = (SurpriseTask) o;
        return id == that.id && gamificationId == that.gamificationId && rewardId == that.rewardId && isCompleted == that.isCompleted && Objects.equals(description, that.description) && Objects.equals(expirationTime, that.expirationTime) && Objects.equals(shownDate, that.shownDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, gamificationId, description, rewardId, expirationTime, isCompleted, shownDate);
    }
}