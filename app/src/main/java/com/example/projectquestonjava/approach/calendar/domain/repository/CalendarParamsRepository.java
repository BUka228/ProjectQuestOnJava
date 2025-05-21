package com.example.projectquestonjava.approach.calendar.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.projectquestonjava.approach.calendar.data.model.CalendarParams;
import java.util.List;
import com.google.common.util.concurrent.ListenableFuture;


public interface CalendarParamsRepository {
    /** Вставляет новые параметры для задачи. */
    ListenableFuture<Long> insertParams(CalendarParams params);

    /** Обновляет существующие параметры. */
    ListenableFuture<Void> updateParams(CalendarParams params);

    /** Удаляет параметры. */
    ListenableFuture<Void> deleteParams(CalendarParams params);

    /** Получает параметры для конкретной задачи. */
    ListenableFuture<CalendarParams> getParamsByTaskId(long taskId);

    /** Получает LiveData параметров для списка задач. */
    LiveData<List<CalendarParams>> getParamsForTasksFlow(List<Long> taskIds);
}