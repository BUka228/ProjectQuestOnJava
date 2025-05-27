package com.example.projectquestonjava.feature.gamification.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.example.projectquestonjava.core.data.model.core.UserAuth;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity(
        tableName = "gamification",
        foreignKeys = {
                @ForeignKey(
                        entity = UserAuth.class,
                        parentColumns = {"id"},
                        childColumns = {"user_id"},
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index(value = {"user_id"}, unique = true)}
)
public class Gamification {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "user_id")
    private int userId;

    private int level;
    private int experience;
    private int coins;

    @ColumnInfo(name = "max_experience_for_level")
    private int maxExperienceForLevel;

    @ColumnInfo(name = "last_active")
    private LocalDateTime lastActive;

    @ColumnInfo(name = "current_streak", defaultValue = "0")
    private int currentStreak;

    @ColumnInfo(name = "last_claimed_date", defaultValue = "-365243219162") // LocalDate.MIN.toEpochDay()
    private LocalDate lastClaimedDate;

    @ColumnInfo(name = "max_streak", defaultValue = "0")
    private int maxStreak;

    // Основной конструктор для Room
    public Gamification(long id, int userId, int level, int experience, int coins,
                        int maxExperienceForLevel, LocalDateTime lastActive,
                        int currentStreak, LocalDate lastClaimedDate, int maxStreak) {
        this.id = id;
        this.userId = userId;
        this.level = level;
        this.experience = experience;
        this.coins = coins;
        this.maxExperienceForLevel = maxExperienceForLevel;
        this.lastActive = lastActive != null ? lastActive : LocalDateTime.now();
        this.currentStreak = currentStreak;
        this.lastClaimedDate = lastClaimedDate != null ? lastClaimedDate : LocalDate.MIN;
        this.maxStreak = maxStreak;
    }

    // Конструктор для создания нового объекта перед вставкой (Room его проигнорирует)
    @Ignore
    public Gamification(int userId, int level, int experience, int coins,
                        int maxExperienceForLevel, LocalDateTime lastActive,
                        int currentStreak, LocalDate lastClaimedDate, int maxStreak) {
        this(0, userId, level, experience, coins, maxExperienceForLevel, lastActive, currentStreak, lastClaimedDate, maxStreak);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public int getMaxExperienceForLevel() {
        return maxExperienceForLevel;
    }

    public void setMaxExperienceForLevel(int maxExperienceForLevel) {
        this.maxExperienceForLevel = maxExperienceForLevel;
    }

    public LocalDateTime getLastActive() {
        return lastActive;
    }

    public void setLastActive(LocalDateTime lastActive) {
        this.lastActive = lastActive;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public LocalDate getLastClaimedDate() {
        return lastClaimedDate;
    }

    public void setLastClaimedDate(LocalDate lastClaimedDate) {
        this.lastClaimedDate = lastClaimedDate;
    }

    public int getMaxStreak() {
        return maxStreak;
    }

    public void setMaxStreak(int maxStreak) {
        this.maxStreak = maxStreak;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Gamification that = (Gamification) o;
        return id == that.id && userId == that.userId && level == that.level && experience == that.experience && coins == that.coins && maxExperienceForLevel == that.maxExperienceForLevel && currentStreak == that.currentStreak && maxStreak == that.maxStreak && Objects.equals(lastActive, that.lastActive) && Objects.equals(lastClaimedDate, that.lastClaimedDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, level, experience, coins, maxExperienceForLevel, lastActive, currentStreak, lastClaimedDate, maxStreak);
    }

}