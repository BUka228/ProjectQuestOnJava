package com.example.projectquestonjava.approach.eatTheFrog.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.projectquestonjava.approach.eatTheFrog.data.model.FrogParams;
import com.google.common.util.concurrent.ListenableFuture;

@Dao
public interface FrogParamsDao {
    @Query("SELECT * FROM frog_params WHERE task_id = :taskId")
    ListenableFuture<FrogParams> getFrogParamsByTaskId(long taskId);

    @Insert
    ListenableFuture<Void> insertFrogParams(FrogParams params);

    @Update
    ListenableFuture<Integer> updateFrogParams(FrogParams params);

    @Delete
    ListenableFuture<Integer> deleteFrogParams(FrogParams params);
}