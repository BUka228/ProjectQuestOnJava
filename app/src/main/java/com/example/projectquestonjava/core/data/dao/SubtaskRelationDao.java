package com.example.projectquestonjava.core.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.projectquestonjava.core.data.model.core.SubtaskRelation;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;

@Dao
public interface SubtaskRelationDao {
    @Query("SELECT * FROM subtask_relation WHERE parent_task_id = :parentTaskId ORDER BY `order` ASC")
    LiveData<List<SubtaskRelation>> getSubtasksForParent(long parentTaskId);

    @Query("SELECT * FROM subtask_relation WHERE child_task_id = :childTaskId LIMIT 1")
    LiveData<SubtaskRelation> getParentForSubtask(long childTaskId);

    @Insert
    ListenableFuture<Void> insertSubtaskRelation(SubtaskRelation relation);

    @Update
    ListenableFuture<Integer> updateSubtaskRelation(SubtaskRelation relation);

    @Delete
    ListenableFuture<Integer> deleteSubtaskRelation(SubtaskRelation relation);


    // --- SYNC ---
    @Query("SELECT * FROM subtask_relation WHERE parent_task_id = :parentTaskId ORDER BY `order` ASC")
    List<SubtaskRelation> getSubtasksForParentSync(long parentTaskId);

    @Query("SELECT * FROM subtask_relation WHERE child_task_id = :childTaskId LIMIT 1")
    SubtaskRelation getParentForSubtaskSync(long childTaskId);

    @Insert
    void insertSubtaskRelationSync(SubtaskRelation relation);

    @Update
    int updateSubtaskRelationSync(SubtaskRelation relation);

    @Delete
    int deleteSubtaskRelationSync(SubtaskRelation relation);
}