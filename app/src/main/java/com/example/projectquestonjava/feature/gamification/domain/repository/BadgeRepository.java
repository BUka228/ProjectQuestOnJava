package com.example.projectquestonjava.feature.gamification.domain.repository;

import androidx.lifecycle.LiveData; // Для Flow -> LiveData
import com.example.projectquestonjava.feature.gamification.data.model.Badge;
import com.example.projectquestonjava.feature.gamification.data.model.GamificationBadgeCrossRef;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;

public interface BadgeRepository {
    LiveData<List<Badge>> getAllBadgesFlow(); // Kotlin Flow -> LiveData
    ListenableFuture<List<Badge>> getAllBadges(); // suspend -> ListenableFuture
    ListenableFuture<Badge> getBadgeById(long badgeId); // suspend -> ListenableFuture (Badge? -> Badge)
    LiveData<List<GamificationBadgeCrossRef>> getEarnedBadgesFlow(); // Kotlin Flow -> LiveData
    ListenableFuture<List<GamificationBadgeCrossRef>> getEarnedBadges(); // suspend -> ListenableFuture
    ListenableFuture<Void> insertEarnedBadge(GamificationBadgeCrossRef crossRef); // suspend Result -> ListenableFuture
    ListenableFuture<Void> deleteEarnedBadgesForGamification(long gamificationId); // suspend Result -> ListenableFuture

    // --- SYNC ---
    void insertEarnedBadgeSync(GamificationBadgeCrossRef crossRef);
    Badge getBadgeByIdSync(long badgeId);
    List<GamificationBadgeCrossRef> getEarnedBadgesSync(long gamificationId);
}