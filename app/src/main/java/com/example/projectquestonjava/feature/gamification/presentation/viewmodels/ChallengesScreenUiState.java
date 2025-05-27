package com.example.projectquestonjava.feature.gamification.presentation.viewmodels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengePeriod;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeProgressFullDetails;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.With;

/**
 * Состояние UI для экрана списка челленджей (ChallengesFragment).
 */
@Value
@Builder(toBuilder = true)
public class ChallengesScreenUiState {

    @With boolean isLoading;
    @With @Nullable String error;
    @With @Nullable ChallengePeriod selectedPeriod;
    @With ChallengeSortOption sortOption;
    @With Set<ChallengeFilterOption> filterOptions;
    @With List<ChallengeProgressFullDetails> challenges;

    public static ChallengesScreenUiState createDefault() {
        return ChallengesScreenUiState.builder()
                .isLoading(true)
                .error(null)
                .selectedPeriod(null)
                .sortOption(ChallengeSortOption.DEADLINE_ASC)
                .filterOptions(Collections.singleton(ChallengeFilterOption.ACTIVE))
                .challenges(Collections.emptyList())
                .build();
    }
}