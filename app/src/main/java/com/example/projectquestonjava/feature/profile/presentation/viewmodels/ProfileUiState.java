package com.example.projectquestonjava.feature.profile.presentation.viewmodels;

import com.example.projectquestonjava.core.data.model.core.UserAuth;
import com.example.projectquestonjava.feature.gamification.data.model.Badge;
import com.example.projectquestonjava.feature.gamification.data.model.Gamification;
import com.example.projectquestonjava.feature.statistics.data.model.GamificationHistory;

import java.util.Collections;
import java.util.List;

import lombok.Getter;

@Getter
public class ProfileUiState { // Убираем public, если используется только здесь
    public final boolean isLoading;
    public final boolean isUpdatingAvatar; // Оставляем, если обновление аватара будет в этом ViewModel
    public final UserAuth user;
    public final Gamification gamification;
    public final int earnedBadgesCount;
    public final List<Badge> recentBadges;
    public final List<GamificationHistory> recentHistory;
    public final String error;

    public ProfileUiState(boolean isLoading, boolean isUpdatingAvatar, UserAuth user, Gamification gamification,
                          int earnedBadgesCount, List<Badge> recentBadges, List<GamificationHistory> recentHistory, String error) {
        this.isLoading = isLoading;
        this.isUpdatingAvatar = isUpdatingAvatar;
        this.user = user;
        this.gamification = gamification;
        this.earnedBadgesCount = earnedBadgesCount;
        this.recentBadges = recentBadges != null ? recentBadges : Collections.emptyList();
        this.recentHistory = recentHistory != null ? recentHistory : Collections.emptyList();
        this.error = error;
    }

    // Конструктор по умолчанию
    public ProfileUiState() {
        this(true, false, null, null, 0, Collections.emptyList(), Collections.emptyList(), null);
    }

    // Метод copy для удобства обновления
    public ProfileUiState copy(Boolean isLoading, Boolean isUpdatingAvatar, UserAuth user, Gamification gamification,
                               Integer earnedBadgesCount, List<Badge> recentBadges, List<GamificationHistory> recentHistory,
                               String error, boolean clearError) { // clearError для явного сброса ошибки
        return new ProfileUiState(
                isLoading != null ? isLoading : this.isLoading,
                isUpdatingAvatar != null ? isUpdatingAvatar : this.isUpdatingAvatar,
                user != null ? user : this.user,
                gamification != null ? gamification : this.gamification,
                earnedBadgesCount != null ? earnedBadgesCount : this.earnedBadgesCount,
                recentBadges != null ? recentBadges : this.recentBadges,
                recentHistory != null ? recentHistory : this.recentHistory,
                clearError ? null : (error != null ? error : this.error)
        );
    }
}