package com.example.projectquestonjava.feature.gamification.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengePeriod;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeStatus;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity(
        tableName = "challenge",
        foreignKeys = {
                @ForeignKey(
                        entity = Reward.class,
                        parentColumns = {"id"},
                        childColumns = {"reward_id"},
                        onDelete = ForeignKey.RESTRICT
                )
        },
        indices = {@Index("reward_id"), @Index("status"), @Index("period")}
)
public class Challenge {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;
    private String description;

    @ColumnInfo(name = "start_date")
    private LocalDateTime startDate;

    @ColumnInfo(name = "end_date")
    private LocalDateTime endDate;

    @ColumnInfo(name = "reward_id")
    private long rewardId;

    private ChallengeStatus status;
    private ChallengePeriod period;

    public Challenge(long id, String name, String description, LocalDateTime startDate, LocalDateTime endDate, long rewardId, ChallengeStatus status, ChallengePeriod period) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.rewardId = rewardId;
        this.status = status != null ? status : ChallengeStatus.ACTIVE;
        this.period = period != null ? period : ChallengePeriod.ONCE;
    }

    // Конструктор для Room
    public Challenge(String name, String description, LocalDateTime startDate, LocalDateTime endDate, long rewardId, ChallengeStatus status, ChallengePeriod period) {
        this(0, name, description, startDate, endDate, rewardId, status, period);
    }


    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public long getRewardId() { return rewardId; }
    public void setRewardId(long rewardId) { this.rewardId = rewardId; }
    public ChallengeStatus getStatus() { return status; }
    public void setStatus(ChallengeStatus status) { this.status = status; }
    public ChallengePeriod getPeriod() { return period; }
    public void setPeriod(ChallengePeriod period) { this.period = period; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Challenge challenge = (Challenge) o;
        return id == challenge.id && rewardId == challenge.rewardId && Objects.equals(name, challenge.name) && Objects.equals(description, challenge.description) && Objects.equals(startDate, challenge.startDate) && Objects.equals(endDate, challenge.endDate) && status == challenge.status && period == challenge.period;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, startDate, endDate, rewardId, status, period);
    }
}