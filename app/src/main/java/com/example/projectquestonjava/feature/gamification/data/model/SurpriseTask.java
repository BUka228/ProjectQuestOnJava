package com.example.projectquestonjava.feature.gamification.data.model;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

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

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "gamification_id")
    private long gamificationId;

    private String description;

    @ColumnInfo(name = "reward_id")
    private long rewardId;

    @ColumnInfo(name = "expiration_time")
    private LocalDateTime expirationTime;

    @ColumnInfo(name = "is_completed")
    private boolean isCompleted;

    @Nullable
    @ColumnInfo(name = "shown_date")
    private LocalDate shownDate;

    // Основной конструктор для Room
    public SurpriseTask(long id, long gamificationId, String description, long rewardId,
                        LocalDateTime expirationTime, boolean isCompleted, @Nullable LocalDate shownDate) {
        this.id = id;
        this.gamificationId = gamificationId;
        this.description = description;
        this.rewardId = rewardId;
        this.expirationTime = expirationTime;
        this.isCompleted = isCompleted;
        this.shownDate = shownDate;
    }

    @Ignore
    public SurpriseTask(long gamificationId, String description, long rewardId,
                        LocalDateTime expirationTime, boolean isCompleted, @Nullable LocalDate shownDate) {
        this(0, gamificationId, description, rewardId, expirationTime, isCompleted, shownDate);
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getGamificationId() {
        return gamificationId;
    }

    public void setGamificationId(long gamificationId) {
        this.gamificationId = gamificationId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getRewardId() {
        return rewardId;
    }

    public void setRewardId(long rewardId) {
        this.rewardId = rewardId;
    }

    public LocalDateTime getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(LocalDateTime expirationTime) {
        this.expirationTime = expirationTime;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    @Nullable
    public LocalDate getShownDate() {
        return shownDate;
    }

    public void setShownDate(@Nullable LocalDate shownDate) {
        this.shownDate = shownDate;
    }

    // Убедитесь, что Room может получить доступ к полям, если они приватные
    // Room использует геттеры и сеттеры, если они есть, или прямые поля, если они public.
    // Если вы используете Kotlin, вы можете использовать `lateinit var` или `@JvmField`
    // для полей, к которым Room должен иметь прямой доступ.
    // В Java, если вы используете private поля, убедитесь, что у вас есть геттеры и сеттеры.

    // equals, hashCode, toString
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