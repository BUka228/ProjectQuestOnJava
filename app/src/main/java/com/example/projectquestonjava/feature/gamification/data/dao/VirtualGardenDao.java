package com.example.projectquestonjava.feature.gamification.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.projectquestonjava.feature.gamification.data.model.VirtualGarden;
import com.google.common.util.concurrent.ListenableFuture;
import java.time.LocalDateTime;
import java.util.List;

@Dao
public interface VirtualGardenDao {
    @Query("SELECT * FROM virtual_garden WHERE gamification_id = :gamificationId ORDER BY id DESC LIMIT 1")
    ListenableFuture<VirtualGarden> getLatestPlant(long gamificationId);

    @Query("SELECT * FROM virtual_garden WHERE gamification_id = :gamificationId ORDER BY id ASC")
    LiveData<List<VirtualGarden>> getAllPlantsFlow(long gamificationId);

    @Query("SELECT * FROM virtual_garden WHERE gamification_id = :gamificationId ORDER BY id ASC")
    ListenableFuture<List<VirtualGarden>> getAllPlants(long gamificationId);

    @Query("SELECT * FROM virtual_garden WHERE id = :plantId")
    ListenableFuture<VirtualGarden> getPlantById(long plantId);

    @Query("SELECT * FROM virtual_garden WHERE id = :plantId")
    LiveData<VirtualGarden> getPlantByIdFlow(long plantId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Long> insert(VirtualGarden plant);

    @Update
    ListenableFuture<Integer> update(VirtualGarden plant);

    @Query("UPDATE virtual_garden SET last_watered = :timestamp WHERE gamification_id = :gamificationId")
    ListenableFuture<Integer> updateLastWateredForAllUserPlants(long gamificationId, LocalDateTime timestamp);

    @Query("DELETE FROM virtual_garden WHERE gamification_id = :gamificationId")
    ListenableFuture<Integer> deleteVirtualGardenForGamification(long gamificationId);
}