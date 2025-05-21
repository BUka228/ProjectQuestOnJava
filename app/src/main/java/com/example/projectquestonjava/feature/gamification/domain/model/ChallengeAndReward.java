package com.example.projectquestonjava.feature.gamification.domain.model;

import androidx.room.Embedded;
import androidx.room.Relation;
import com.example.projectquestonjava.feature.gamification.data.model.Challenge;
import com.example.projectquestonjava.feature.gamification.data.model.Reward;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class ChallengeAndReward {
    @Embedded
    private Challenge challenge;

    @Relation(
            parentColumn = "reward_id",
            entityColumn = "id",
            entity = Reward.class
    )
    private Reward reward;

    public ChallengeAndReward(Challenge challenge, Reward reward) {
        this.challenge = challenge;
        this.reward = reward;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChallengeAndReward that = (ChallengeAndReward) o;
        return Objects.equals(challenge, that.challenge) && Objects.equals(reward, that.reward);
    }

    @Override
    public int hashCode() {
        return Objects.hash(challenge, reward);
    }
}