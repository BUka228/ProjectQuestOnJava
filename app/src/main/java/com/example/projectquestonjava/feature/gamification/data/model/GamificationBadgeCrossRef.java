package com.example.projectquestonjava.feature.gamification.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import java.time.LocalDateTime;
import java.util.Objects;

import lombok.Getter;

@Getter
@Entity(
        tableName = "gamification_badge_cross_ref",
        primaryKeys = {"gamification_id", "badge_id"},
        foreignKeys = {
                @ForeignKey(
                        entity = Gamification.class,
                        parentColumns = {"id"},
                        childColumns = {"gamification_id"},
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = Badge.class,
                        parentColumns = {"id"},
                        childColumns = {"badge_id"},
                        onDelete = ForeignKey.RESTRICT
                )
        },
        indices = {@Index("gamification_id"), @Index("badge_id")}
)
public class GamificationBadgeCrossRef {

    @ColumnInfo(name = "gamification_id")
    private final long gamificationId;

    @ColumnInfo(name = "badge_id")
    private final long badgeId;

    @ColumnInfo(name = "earned_at")
    private final LocalDateTime earnedAt;

    public GamificationBadgeCrossRef(long gamificationId, long badgeId, LocalDateTime earnedAt) {
        this.gamificationId = gamificationId;
        this.badgeId = badgeId;
        this.earnedAt = earnedAt != null ? earnedAt : LocalDateTime.now();
    }
    // Конструктор без earnedAt для удобства
    public GamificationBadgeCrossRef(long gamificationId, long badgeId) {
        this(gamificationId, badgeId, LocalDateTime.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GamificationBadgeCrossRef that = (GamificationBadgeCrossRef) o;
        return gamificationId == that.gamificationId && badgeId == that.badgeId && Objects.equals(earnedAt, that.earnedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gamificationId, badgeId, earnedAt);
    }
}