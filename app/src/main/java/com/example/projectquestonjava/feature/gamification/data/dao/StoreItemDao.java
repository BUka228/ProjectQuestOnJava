package com.example.projectquestonjava.feature.gamification.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.projectquestonjava.feature.gamification.data.model.StoreItem;
import com.example.projectquestonjava.feature.gamification.domain.model.StoreItemCategory;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;

@Dao
public interface StoreItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Long> insert(StoreItem storeItem);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Void> insertAll(List<StoreItem> storeItems);

    @Update
    ListenableFuture<Integer> update(StoreItem storeItem);

    @Delete
    ListenableFuture<Integer> delete(StoreItem storeItem);

    @Query("SELECT * FROM store_item WHERE id = :id")
    ListenableFuture<StoreItem> getById(long id);

    @Query("SELECT * FROM store_item WHERE category = :category ORDER BY cost ASC")
    ListenableFuture<List<StoreItem>> getByCategory(StoreItemCategory category);

    @Query("SELECT * FROM store_item ORDER BY category, cost ASC")
    ListenableFuture<List<StoreItem>> getAll();

    @Query("SELECT * FROM store_item ORDER BY category, cost ASC")
    LiveData<List<StoreItem>> getAllFlow();
}