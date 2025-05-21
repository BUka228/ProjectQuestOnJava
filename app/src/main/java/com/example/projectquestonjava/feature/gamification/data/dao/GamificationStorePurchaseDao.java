package com.example.projectquestonjava.feature.gamification.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.projectquestonjava.feature.gamification.data.model.GamificationStorePurchase;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;

@Dao
public interface GamificationStorePurchaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Long> insert(GamificationStorePurchase purchase);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Void> insertAll(List<GamificationStorePurchase> purchases);

    @Update
    ListenableFuture<Integer> update(GamificationStorePurchase purchase);

    @Delete
    ListenableFuture<Integer> delete(GamificationStorePurchase purchase);

    @Query("SELECT * FROM gamification_store_purchase WHERE gamification_id = :gamificationId ORDER BY purchased_at DESC")
    LiveData<List<GamificationStorePurchase>> getPurchasesFlow(long gamificationId);

    @Query("SELECT * FROM gamification_store_purchase WHERE gamification_id = :gamificationId")
    ListenableFuture<List<GamificationStorePurchase>> getPurchases(long gamificationId);

    @Query("DELETE FROM gamification_store_purchase WHERE gamification_id = :gamificationId")
    ListenableFuture<Integer> deletePurchasesForGamification(long gamificationId);

    @Query("DELETE FROM gamification_store_purchase WHERE store_item_id = :storeItemId")
    ListenableFuture<Integer> deletePurchasesForItem(long storeItemId);
}