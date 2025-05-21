package com.example.projectquestonjava.feature.gamification.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import java.time.LocalDateTime;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

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

    @Getter
    @ColumnInfo(name = "gamification_id")
    private final long gamificationId;

    @Getter
    @ColumnInfo(name = "challenge_id")
    private final long challengeId;

    @Getter
    @ColumnInfo(name = "rule_id")
    private final long ruleId;

    @Setter
    @Getter
    private int progress; // Может меняться, поэтому не final

    @ColumnInfo(name = "is_completed", defaultValue = "0")
    private boolean isCompleted; // Может меняться

    @Setter
    @Getter
    @ColumnInfo(name = "last_updated")
    private LocalDateTime lastUpdated; // Может меняться

    public GamificationChallengeProgress(long gamificationId, long challengeId, long ruleId, int progress, boolean isCompleted, LocalDateTime lastUpdated) {
        this.gamificationId = gamificationId;
        this.challengeId = challengeId;
        this.ruleId = ruleId;
        this.progress = progress;
        this.isCompleted = isCompleted;
        this.lastUpdated = lastUpdated != null ? lastUpdated : LocalDateTime.now();
    }

    // Конструктор с значениями по умолчанию
    public GamificationChallengeProgress(long gamificationId, long challengeId, long ruleId) {
        this(gamificationId, challengeId, ruleId, 0, false, LocalDateTime.now());
    }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

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