package com.example.projectquestonjava.core.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.projectquestonjava.core.data.model.core.TaskTagCrossRef;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;

@Dao
public interface TaskTagCrossRefDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Void> insertTaskTag(TaskTagCrossRef taskTagCrossRef);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Void> insertAllTaskTag(List<TaskTagCrossRef> taskTagCrossRefs);

    @Delete
    ListenableFuture<Void> deleteTaskTag(TaskTagCrossRef taskTagCrossRef);

    @Query("DELETE FROM task_tag_cross_ref WHERE task_id = :taskId")
    ListenableFuture<Void> deleteTaskTagsByTaskId(long taskId);

    @Query("DELETE FROM task_tag_cross_ref WHERE tag_id = :tagId")
    ListenableFuture<Void> deleteTaskTagsByTagId(long tagId);


    // --- SYNC ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllTaskTagSync(List<TaskTagCrossRef> taskTagCrossRefs);

    @Query("DELETE FROM task_tag_cross_ref WHERE task_id = :taskId")
    void deleteTaskTagsByTaskIdSync(long taskId);
}