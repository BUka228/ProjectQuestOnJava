package com.example.projectquestonjava.feature.statistics.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.example.projectquestonjava.feature.gamification.data.model.Gamification;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity(
        tableName = "gamification_history",
        foreignKeys = {
                @ForeignKey(
                        entity = Gamification.class,
                        parentColumns = {"id"},
                        childColumns = {"gamification_id"},
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index("gamification_id"), @Index("timestamp")}
)
public class GamificationHistory {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "gamification_id")
    private final long gamificationId;

    private final LocalDateTime timestamp;

    @ColumnInfo(name = "xp_change", defaultValue = "0")
    private final int xpChange;

    @ColumnInfo(name = "coins_change", defaultValue = "0")
    private final int coinsChange;

    @Nullable
    @ColumnInfo(name = "reason")
    private final String reason;

    @Nullable
    @ColumnInfo(name = "related_entity_id")
    private final Long relatedEntityId; // Тип Long, так как может быть ID задачи

    // Основной конструктор для Room
    public GamificationHistory(long id, long gamificationId, LocalDateTime timestamp, int xpChange,
                               int coinsChange, @Nullable String reason, @Nullable Long relatedEntityId) {
        this.id = id;
        this.gamificationId = gamificationId;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.xpChange = xpChange;
        this.coinsChange = coinsChange;
        this.reason = reason;
        this.relatedEntityId = relatedEntityId;
    }

    @Ignore
    public GamificationHistory(long gamificationId, LocalDateTime timestamp, int xpChange,
                               int coinsChange, @Nullable String reason, @Nullable Long relatedEntityId) {
        this(0, gamificationId, timestamp, xpChange, coinsChange, reason, relatedEntityId);
    }

    @Ignore // Конструктор для создания нового объекта без id и с текущим timestamp
    public GamificationHistory(long gamificationId, int xpChange, int coinsChange,
                               @Nullable String reason, @Nullable Long relatedEntityId) {
        this(0, gamificationId, LocalDateTime.now(), xpChange, coinsChange, reason, relatedEntityId);
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getXpChange() {
        return xpChange;
    }

    public int getCoinsChange() {
        return coinsChange;
    }

    @Nullable
    public String getReason() {
        return reason;
    }

    @Nullable
    public Long getRelatedEntityId() {
        return relatedEntityId;
    }



    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GamificationHistory that = (GamificationHistory) o;
        return id == that.id && gamificationId == that.gamificationId && xpChange == that.xpChange && coinsChange == that.coinsChange && Objects.equals(timestamp, that.timestamp) && Objects.equals(reason, that.reason) && Objects.equals(relatedEntityId, that.relatedEntityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, gamificationId, timestamp, xpChange, coinsChange, reason, relatedEntityId);
    }

    @NonNull
    @Override
    public String toString() {
        return "GamificationHistory{" +
                "id=" + id +
                ", gamificationId=" + gamificationId +
                ", timestamp=" + timestamp +
                ", xpChange=" + xpChange +
                ", coinsChange=" + coinsChange +
                ", reason='" + reason + '\'' +
                ", relatedEntityId=" + relatedEntityId +
                '}';
    }
}