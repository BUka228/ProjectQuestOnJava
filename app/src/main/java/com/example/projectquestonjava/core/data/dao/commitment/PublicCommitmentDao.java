package com.example.projectquestonjava.core.data.dao.commitment;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.projectquestonjava.core.data.model.commitment.PublicCommitment;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;

@Dao
public interface PublicCommitmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Long> insert(PublicCommitment publicCommitment);

    @Update
    ListenableFuture<Integer> update(PublicCommitment publicCommitment);

    @Delete
    ListenableFuture<Integer> delete(PublicCommitment publicCommitment);

    @Query("SELECT * FROM public_commitment WHERE id = :id AND user_id = :userId")
    LiveData<PublicCommitment> getById(int id, int userId);

    @Query("SELECT * FROM public_commitment WHERE task_id = :taskId AND user_id = :userId")
    LiveData<List<PublicCommitment>> getByTaskId(long taskId, int userId);
}