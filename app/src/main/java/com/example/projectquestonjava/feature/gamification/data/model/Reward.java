package com.example.projectquestonjava.feature.gamification.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.example.projectquestonjava.feature.gamification.domain.model.RewardType; // Убедись, что RewardType.java создан
import java.util.Objects;

@Entity(tableName = "reward")
public class Reward {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;
    private String description;

    @ColumnInfo(name = "reward_type")
    private RewardType rewardType;

    @ColumnInfo(name = "reward_value")
    private String rewardValue; // Хранит число, ID значка, тип растения или формулу

    // Основной конструктор для Room
    public Reward(long id, String name, String description, RewardType rewardType, String rewardValue) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rewardType = rewardType;
        this.rewardValue = rewardValue;
    }

    @Ignore
    public Reward(String name, String description, RewardType rewardType, String rewardValue) {
        this(0, name, description, rewardType, rewardValue);
    }

    // Геттеры
    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public RewardType getRewardType() {
        return rewardType;
    }

    public String getRewardValue() {
        return rewardValue;
    }

    // Сеттеры
    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRewardType(RewardType rewardType) {
        this.rewardType = rewardType;
    }

    public void setRewardValue(String rewardValue) {
        this.rewardValue = rewardValue;
    }
    // equals, hashCode, toString
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