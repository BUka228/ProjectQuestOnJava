package com.example.projectquestonjava.feature.gamification.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.projectquestonjava.feature.gamification.data.model.Gamification;
import com.google.common.util.concurrent.ListenableFuture;

public interface GamificationRepository {
    LiveData<Gamification> getCurrentUserGamificationFlow(); // Kotlin Flow -> LiveData (Gamification? -> Gamification)
    ListenableFuture<Gamification> getGamificationByUserId(int userId);
    ListenableFuture<Gamification> getGamificationById(long id);
    ListenableFuture<Long> insertGamification(Gamification gamification); // Возвращает ID
    ListenableFuture<Void> updateGamification(Gamification gamification);
    ListenableFuture<Void> deleteGamificationForUser(int userId);

    ListenableFuture<Gamification> getCurrentUserGamificationFuture();
    Gamification getGamificationByIdSync(long id);
    void updateGamificationSync(Gamification gamification);
}