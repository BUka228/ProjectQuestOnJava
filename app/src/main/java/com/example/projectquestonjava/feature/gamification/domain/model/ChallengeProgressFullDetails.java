package com.example.projectquestonjava.feature.gamification.domain.model;

import androidx.room.Embedded;
import androidx.room.Relation;
import com.example.projectquestonjava.feature.gamification.data.model.Challenge;
import com.example.projectquestonjava.feature.gamification.data.model.ChallengeRule;
import com.example.projectquestonjava.feature.gamification.data.model.GamificationChallengeProgress;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class ChallengeProgressFullDetails {
    @Embedded
    private GamificationChallengeProgress progress;

    @Relation(
            parentColumn = "rule_id",
            entityColumn = "id",
            entity = ChallengeRule.class
    )
    private ChallengeRule rule;

    @Relation(
            parentColumn = "challenge_id",
            entityColumn = "id",
            entity = Challenge.class // Связь идет к Challenge, а не к ChallengeAndReward
    )
    private ChallengeAndReward challengeAndReward; // Тип поля остается ChallengeAndReward

    public ChallengeProgressFullDetails(GamificationChallengeProgress progress, ChallengeRule rule, ChallengeAndReward challengeAndReward) {
        this.progress = progress;
        this.rule = rule;
        this.challengeAndReward = challengeAndReward;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChallengeProgressFullDetails that = (ChallengeProgressFullDetails) o;
        return Objects.equals(progress, that.progress) && Objects.equals(rule, that.rule) && Objects.equals(challengeAndReward, that.challengeAndReward);
    }

    @Override
    public int hashCode() {
        return Objects.hash(progress, rule, challengeAndReward);
    }
}