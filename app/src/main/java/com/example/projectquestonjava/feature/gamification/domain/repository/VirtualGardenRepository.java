package com.example.projectquestonjava.feature.gamification.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.projectquestonjava.feature.gamification.data.model.VirtualGarden;
import com.google.common.util.concurrent.ListenableFuture;
import java.time.LocalDateTime;
import java.util.List;

public interface VirtualGardenRepository {
    LiveData<List<VirtualGarden>> getAllPlantsFlow();
    LiveData<VirtualGarden> getPlantFlow(long plantId);
    ListenableFuture<VirtualGarden> getPlant(long plantId);
    ListenableFuture<VirtualGarden> getLatestPlant();
    ListenableFuture<Long> insertPlant(VirtualGarden plant);
    ListenableFuture<Void> updatePlant(VirtualGarden plant);
    ListenableFuture<Void> updateLastWateredForAllUserPlants(long gamificationId, LocalDateTime timestamp);
    ListenableFuture<Void> deleteVirtualGardenForGamification(long gamificationId);

    // --- SYNC ---
    long insertPlantSync(VirtualGarden plant);
    ListenableFuture<List<VirtualGarden>> getAllPlantsFuture();
    List<VirtualGarden> getAllPlantsSync(long gamificationId);
}