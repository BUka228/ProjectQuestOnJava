package com.example.projectquestonjava.feature.gamification.presentation.viewmodels;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengePeriod;

import lombok.Data;
import lombok.Getter;



public record ChallengeCardInfo(@Getter long id, @DrawableRes int iconResId, String name,
                                String description, float progress, String progressText,
                                @Nullable String deadlineText,
                                @DrawableRes @Nullable Integer rewardIconResId,
                                @Nullable String rewardName, boolean isUrgent,
                                ChallengePeriod period) {
}