package com.example.projectquestonjava.feature.gamification.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.projectquestonjava.feature.gamification.data.model.Reward;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;

@Dao
public interface RewardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Long> insert(Reward reward);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Void> insertAll(List<Reward> rewards);

    @Update
    ListenableFuture<Integer> update(Reward reward);

    @Delete
    ListenableFuture<Integer> delete(Reward reward);

    @Query("SELECT * FROM reward WHERE id = :id")
    ListenableFuture<Reward> getById(long id);

    @Query("SELECT * FROM reward ORDER BY name ASC")
    ListenableFuture<List<Reward>> getAll();

    @Query("SELECT * FROM reward ORDER BY name ASC")
    LiveData<List<Reward>> getAllFlow();
}