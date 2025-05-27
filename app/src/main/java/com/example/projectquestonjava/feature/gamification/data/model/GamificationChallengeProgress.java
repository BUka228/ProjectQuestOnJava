package com.example.projectquestonjava.feature.gamification.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity(
        tableName = "gamification_challenge_progress",
        primaryKeys = {"gamification_id", "challenge_id", "rule_id"},
        foreignKeys = {
                @ForeignKey(
                        entity = Gamification.class,
                        parentColumns = {"id"},
                        childColumns = {"gamification_id"},
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = Challenge.class,
                        parentColumns = {"id"},
                        childColumns = {"challenge_id"},
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = ChallengeRule.class,
                        parentColumns = {"id"},
                        childColumns = {"rule_id"},
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index("gamification_id"), @Index("challenge_id"), @Index("rule_id")}
)
public class GamificationChallengeProgress {

    @ColumnInfo(name = "gamification_id")
    private final long gamificationId;

    @ColumnInfo(name = "challenge_id")
    private final long challengeId;

    @ColumnInfo(name = "rule_id")
    private final long ruleId;

    private int progress;

    @ColumnInfo(name = "is_completed", defaultValue = "0")
    private boolean isCompleted;

    @ColumnInfo(name = "last_updated")
    private LocalDateTime lastUpdated;

    // Основной конструктор для Room
    public GamificationChallengeProgress(long gamificationId, long challengeId, long ruleId,
                                         int progress, boolean isCompleted, LocalDateTime lastUpdated) {
        this.gamificationId = gamificationId;
        this.challengeId = challengeId;
        this.ruleId = ruleId;
        this.progress = progress;
        this.isCompleted = isCompleted;
        this.lastUpdated = lastUpdated != null ? lastUpdated : LocalDateTime.now();
    }

    @Ignore
    public GamificationChallengeProgress(long gamificationId, long challengeId, long ruleId) {
        this(gamificationId, challengeId, ruleId, 0, false, LocalDateTime.now());
    }

    public long getGamificationId() {
        return gamificationId;
    }

    public long getChallengeId() {
        return challengeId;
    }

    public long getRuleId() {
        return ruleId;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GamificationChallengeProgress that = (GamificationChallengeProgress) o;
        return gamificationId == that.gamificationId && challengeId == that.challengeId && ruleId == that.ruleId && progress == that.progress && isCompleted == that.isCompleted && Objects.equals(lastUpdated, that.lastUpdated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gamificationId, challengeId, ruleId, progress, isCompleted, lastUpdated);
    }
}