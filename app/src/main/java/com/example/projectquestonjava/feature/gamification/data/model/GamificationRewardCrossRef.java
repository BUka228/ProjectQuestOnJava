package com.example.projectquestonjava.feature.gamification.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity(
        tableName = "gamification_reward_cross_ref",
        primaryKeys = {"gamification_id", "reward_id"},
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
                        onDelete = ForeignKey.CASCADE // Если награда удаляется, то и запись о её получении
                )
        },
        indices = {@Index("gamification_id"), @Index("reward_id")}
)
public class GamificationRewardCrossRef {

    @ColumnInfo(name = "gamification_id")
    private final long gamificationId;

    @ColumnInfo(name = "reward_id")
    private final long rewardId;

    @ColumnInfo(name = "purchased_at") // Имя колонки может быть обманчивым, если это не покупка
    private final LocalDateTime earnedAt; // Лучше earnedAt, если это общая таблица получения наград

    public GamificationRewardCrossRef(long gamificationId, long rewardId, LocalDateTime earnedAt) {
        this.gamificationId = gamificationId;
        this.rewardId = rewardId;
        this.earnedAt = earnedAt != null ? earnedAt : LocalDateTime.now();
    }
    public GamificationRewardCrossRef(long gamificationId, long rewardId) {
        this(gamificationId, rewardId, LocalDateTime.now());
    }


    public long getGamificationId() { return gamificationId; }
    public long getRewardId() { return rewardId; }
    public LocalDateTime getEarnedAt() { return earnedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GamificationRewardCrossRef that = (GamificationRewardCrossRef) o;
        return gamificationId == that.gamificationId && rewardId == that.rewardId && Objects.equals(earnedAt, that.earnedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gamificationId, rewardId, earnedAt);
    }
}