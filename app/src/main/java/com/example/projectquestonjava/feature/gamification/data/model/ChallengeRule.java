package com.example.projectquestonjava.feature.gamification.data.model;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengePeriod;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeType;
import java.util.Objects;

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

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "challenge_id")
    private long challengeId;

    private ChallengeType type;
    private int target;

    @Nullable
    @ColumnInfo(name = "condition_json")
    private String conditionJson;

    private ChallengePeriod period;

    // Основной конструктор для Room
    public ChallengeRule(long id, long challengeId, ChallengeType type, int target,
                         @Nullable String conditionJson, ChallengePeriod period) {
        this.id = id;
        this.challengeId = challengeId;
        this.type = type;
        this.target = target;
        this.conditionJson = conditionJson;
        this.period = period;
    }

    @Ignore
    public ChallengeRule(long challengeId, ChallengeType type, int target,
                         @Nullable String conditionJson, ChallengePeriod period) {
        this(0, challengeId, type, target, conditionJson, period);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(long challengeId) {
        this.challengeId = challengeId;
    }

    public ChallengeType getType() {
        return type;
    }

    public void setType(ChallengeType type) {
        this.type = type;
    }

    public int getTarget() {
        return target;
    }

    public void setTarget(int target) {
        this.target = target;
    }

    @Nullable
    public String getConditionJson() {
        return conditionJson;
    }

    public void setConditionJson(@Nullable String conditionJson) {
        this.conditionJson = conditionJson;
    }

    public ChallengePeriod getPeriod() {
        return period;
    }

    public void setPeriod(ChallengePeriod period) {
        this.period = period;
    }




    // equals, hashCode, toString
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