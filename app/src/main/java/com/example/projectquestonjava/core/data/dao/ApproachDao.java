package com.example.projectquestonjava.core.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.projectquestonjava.core.data.model.core.Approach;

import java.util.List;

@Dao
public interface ApproachDao {

    @Query("SELECT * FROM approach")
    LiveData<List<Approach>> getAllApproaches(); 

    @Query("SELECT * FROM approach WHERE id = :id")
    LiveData<Approach> getApproachById(long id); 

    @Insert
    long insertApproach(Approach approach);

    @Insert
    void insertAll(List<Approach> approaches);

    @Update
    void updateApproach(Approach approach);

    @Delete
    void deleteApproach(Approach approach);
}
