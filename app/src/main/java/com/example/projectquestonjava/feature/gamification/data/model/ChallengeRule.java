package com.example.projectquestonjava.feature.gamification.data.model;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengePeriod;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeType;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

@Entity(
        tableName = "challenge_rule",
        foreignKeys = {
                @ForeignKey(
                        entity = Challenge.class,
                        parentColumns = {"id"},
                        childColumns = {"challenge_id"},
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index("challenge_id"), @Index("type")}
)
public class ChallengeRule {

    @Setter
    @Getter
    @PrimaryKey(autoGenerate = true)
    private long id;

    @Setter
    @Getter
    @ColumnInfo(name = "challenge_id")
    private long challengeId;

    @Setter
    @Getter
    private ChallengeType type;
    @Setter
    @Getter
    private int target;

    @Nullable
    @ColumnInfo(name = "condition_json")
    private String conditionJson;

    @Getter
    @Setter
    private ChallengePeriod period;

    public ChallengeRule(long id, long challengeId, ChallengeType type, int target, @Nullable String conditionJson, ChallengePeriod period) {
        this.id = id;
        this.challengeId = challengeId;
        this.type = type;
        this.target = target;
        this.conditionJson = conditionJson;
        this.period = period;
    }
    // Конструктор для Room
    public ChallengeRule(long challengeId, ChallengeType type, int target, @Nullable String conditionJson, ChallengePeriod period) {
        this(0, challengeId, type, target, conditionJson, period);
    }


    @Nullable
    public String getConditionJson() { return conditionJson; }
    public void setConditionJson(@Nullable String conditionJson) { this.conditionJson = conditionJson; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChallengeRule that = (ChallengeRule) o;
        return id == that.id && challengeId == that.challengeId && target == that.target && type == that.type && Objects.equals(conditionJson, that.conditionJson) && period == that.period;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, challengeId, type, target, conditionJson, period);
    }
}