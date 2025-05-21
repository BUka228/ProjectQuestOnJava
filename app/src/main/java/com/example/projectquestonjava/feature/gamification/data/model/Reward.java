package com.example.projectquestonjava.feature.gamification.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.example.projectquestonjava.feature.gamification.domain.model.RewardType;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(tableName = "reward")
public class Reward {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;
    private String description;

    @ColumnInfo(name = "reward_type")
    private RewardType rewardType;

    @ColumnInfo(name = "reward_value")
    private String rewardValue;

    public Reward(long id, String name, String description, RewardType rewardType, String rewardValue) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rewardType = rewardType;
        this.rewardValue = rewardValue;
    }
    // Конструктор для Room
    public Reward(String name, String description, RewardType rewardType, String rewardValue) {
        this(0, name, description, rewardType, rewardValue);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reward reward = (Reward) o;
        return id == reward.id && Objects.equals(name, reward.name) && Objects.equals(description, reward.description) && rewardType == reward.rewardType && Objects.equals(rewardValue, reward.rewardValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, rewardType, rewardValue);
    }
}