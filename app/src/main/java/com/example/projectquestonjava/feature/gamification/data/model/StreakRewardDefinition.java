package com.example.projectquestonjava.feature.gamification.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import java.util.Objects;

@Entity(
        tableName = "streak_reward_definition",
        primaryKeys = {"streak_day"},
        foreignKeys = {
                @ForeignKey(
                        entity = Reward.class,
                        parentColumns = {"id"},
                        childColumns = {"reward_id"},
                        onDelete = ForeignKey.RESTRICT
                )
        },
        indices = {@Index("reward_id")}
)
public class StreakRewardDefinition {

    @ColumnInfo(name = "streak_day")
    private int streakDay;

    @ColumnInfo(name = "reward_id")
    private long rewardId;

    // Основной конструктор для Room
    public StreakRewardDefinition(int streakDay, long rewardId) {
        this.streakDay = streakDay;
        this.rewardId = rewardId;
    }

    // Геттеры и сеттеры
    public int getStreakDay() {
        return streakDay;
    }

    public void setStreakDay(int streakDay) {
        this.streakDay = streakDay;
    }

    public long getRewardId() {
        return rewardId;
    }

    public void setRewardId(long rewardId) {
        this.rewardId = rewardId;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StreakRewardDefinition that = (StreakRewardDefinition) o;
        return streakDay == that.streakDay && rewardId == that.rewardId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(streakDay, rewardId);
    }
}