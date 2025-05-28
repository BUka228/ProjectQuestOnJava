package com.example.projectquestonjava.feature.gamification.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.projectquestonjava.feature.gamification.data.model.Gamification;
import com.google.common.util.concurrent.ListenableFuture;

@Dao
public interface GamificationDao {
    // --- ASYNC / LiveData ---
    @Query("SELECT * FROM gamification WHERE id = :id")
    ListenableFuture<Gamification> getById(long id);

    @Query("SELECT * FROM gamification WHERE user_id = :userId LIMIT 1")
    ListenableFuture<Gamification> getByUserId(int userId);

    @Query("SELECT * FROM gamification WHERE id = :id")
    LiveData<Gamification> getByIdFlow(long id);

    @Query("SELECT * FROM gamification WHERE user_id = :userId LIMIT 1")
    LiveData<Gamification> getByUserIdFlow(int userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Long> insert(Gamification gamification);

    @Update
    ListenableFuture<Integer> update(Gamification gamification);

    @Query("DELETE FROM gamification WHERE user_id = :userId")
    ListenableFuture<Integer> deleteGamificationForUser(int userId);

    // --- SYNC ---
    @Query("SELECT * FROM gamification WHERE id = :id")
    Gamification getByIdSync(long id); // Уже был

    @Query("SELECT * FROM gamification WHERE user_id = :userId LIMIT 1")
    Gamification getByUserIdSync(int userId); // Уже был

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertSync(Gamification gamification); // Уже был

    @Update
    int updateSync(Gamification gamification); // Уже был
}