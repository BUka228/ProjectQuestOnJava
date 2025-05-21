package com.example.projectquestonjava.feature.gamification.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.example.projectquestonjava.core.data.model.core.UserAuth; // Убедитесь, что UserAuth.java создан
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
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

    // Конструктор для Room (id будет установлен автоматически)
    public Gamification(int userId, int level, int experience, int coins, int maxExperienceForLevel, LocalDateTime lastActive, int currentStreak, LocalDate lastClaimedDate, int maxStreak) {
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

    public Gamification(long id, int userId, int level, int experience, int coins, int maxExperienceForLevel, LocalDateTime lastActive, int currentStreak, LocalDate lastClaimedDate, int maxStreak) {
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