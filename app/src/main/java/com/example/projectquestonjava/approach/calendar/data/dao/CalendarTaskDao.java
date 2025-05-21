package com.example.projectquestonjava.approach.calendar.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.projectquestonjava.approach.calendar.data.model.CalendarParams;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;

@Dao
public interface CalendarTaskDao {

    @Query("SELECT * FROM calendar_params WHERE task_id IN (:taskIds)")
    LiveData<List<CalendarParams>> getParamsForTasks(List<Long> taskIds);

    @Query("SELECT * FROM calendar_params WHERE task_id = :taskId")
    ListenableFuture<CalendarParams> getCalendarParamsByTaskId(long taskId);

    @Insert
    ListenableFuture<Long> insertCalendarParams(CalendarParams params);

    @Update
    ListenableFuture<Void> updateCalendarParams(CalendarParams params);

    @Delete
    ListenableFuture<Void> deleteCalendarParams(CalendarParams params);
}