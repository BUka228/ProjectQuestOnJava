package com.example.projectquestonjava.approach.eisenhower.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.projectquestonjava.approach.eisenhower.data.model.EisenhowerParams;
import com.google.common.util.concurrent.ListenableFuture;

@Dao
public interface EisenhowerParamsDao {
    @Query("SELECT * FROM eisenhower_params WHERE task_id = :taskId")
    ListenableFuture<EisenhowerParams> getEisenhowerParamsByTaskId(long taskId);

    @Insert
    ListenableFuture<Void> insertEisenhowerParams(EisenhowerParams params);

    @Update
    ListenableFuture<Integer> updateEisenhowerParams(EisenhowerParams params);

    @Delete
    ListenableFuture<Integer> deleteEisenhowerParams(EisenhowerParams params);
}