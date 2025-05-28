package com.example.projectquestonjava.feature.gamification.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.projectquestonjava.feature.gamification.data.model.Badge;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;

@Dao
public interface BadgeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Long> insert(Badge badge);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Void> insertAll(List<Badge> badges);

    @Update
    ListenableFuture<Integer> update(Badge badge);

    @Delete
    ListenableFuture<Integer> delete(Badge badge);

    @Query("SELECT * FROM badge ORDER BY name ASC")
    LiveData<List<Badge>> getAllBadgesFlow();

    @Query("SELECT * FROM badge")
    ListenableFuture<List<Badge>> getAllBadges();

    @Query("SELECT * FROM badge WHERE id = :badgeId")
    ListenableFuture<Badge> getBadgeById(long badgeId);


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertSync(Badge badge);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllSync(List<Badge> badges);

    @Query("SELECT * FROM badge WHERE id = :badgeId")
    Badge getBadgeByIdSync(long badgeId);


}