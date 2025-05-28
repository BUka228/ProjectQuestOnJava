package com.example.projectquestonjava.feature.statistics.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.projectquestonjava.feature.statistics.data.model.GamificationHistory;
// AggregatedHistoryPoint не используется в интерфейсе репозитория напрямую
import com.google.common.util.concurrent.ListenableFuture;
import java.time.LocalDateTime;
import java.util.List;

public interface GamificationHistoryRepository {
    LiveData<List<GamificationHistory>> getHistoryFlow();
    ListenableFuture<List<GamificationHistory>> getHistoryForPeriod(LocalDateTime startTime, LocalDateTime endTime);
    ListenableFuture<Long> insertHistoryEntry(GamificationHistory entry);
    ListenableFuture<Void> deleteHistoryForGamification(); // gamificationId будет браться из сессии в реализации
    // --- SYNC ---
    long insertHistoryEntrySync(GamificationHistory entry);
}