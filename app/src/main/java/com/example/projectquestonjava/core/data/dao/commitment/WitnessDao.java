package com.example.projectquestonjava.core.data.dao.commitment;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.projectquestonjava.core.data.model.commitment.Witness;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;

@Dao
public interface WitnessDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Long> insert(Witness witness);

    @Update
    ListenableFuture<Integer> update(Witness witness);

    @Delete
    ListenableFuture<Integer> delete(Witness witness);

    @Query("SELECT * FROM witness WHERE id = :id")
    LiveData<Witness> getById(int id);

    @Query("SELECT * FROM witness WHERE commitment_id = :commitmentId")
    LiveData<List<Witness>> getByCommitmentId(int commitmentId);
}