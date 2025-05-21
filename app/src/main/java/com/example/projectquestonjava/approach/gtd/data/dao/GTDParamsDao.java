package com.example.projectquestonjava.approach.gtd.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.projectquestonjava.approach.gtd.data.model.GTDParams;
import com.google.common.util.concurrent.ListenableFuture;

@Dao
public interface GTDParamsDao {
    @Query("SELECT * FROM gtd_params WHERE task_id = :taskId")
    ListenableFuture<GTDParams> getGtdParamsByTaskId(long taskId);

    @Insert
    ListenableFuture<Void> insertGtdParams(GTDParams params);

    @Update
    ListenableFuture<Integer> updateGtdParams(GTDParams params);

    @Delete
    ListenableFuture<Integer> deleteGtdParams(GTDParams params);
}