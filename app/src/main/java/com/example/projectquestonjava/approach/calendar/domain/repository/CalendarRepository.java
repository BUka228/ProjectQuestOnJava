package com.example.projectquestonjava.approach.calendar.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.projectquestonjava.approach.calendar.domain.model.CalendarTaskWithTagsAndPomodoro;
import com.google.common.util.concurrent.ListenableFuture;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface CalendarRepository {
    ListenableFuture<CalendarTaskWithTagsAndPomodoro> getTaskWithTagsAndPomodoroById(long workspaceId, long taskId);

    LiveData<List<CalendarTaskWithTagsAndPomodoro>> getTasksForDay(long workspaceId, LocalDateTime day);

    LiveData<List<CalendarTaskWithTagsAndPomodoro>> getTasksForMonth(long workspaceId, LocalDate startTimestamp);
}