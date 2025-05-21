package com.example.projectquestonjava.approach.calendar.extensions;

import com.example.projectquestonjava.core.data.model.enums.Priority;
import com.example.projectquestonjava.core.domain.repository.PriorityResolver;
import com.example.projectquestonjava.approach.calendar.domain.model.CalendarTaskSummary;
import com.example.projectquestonjava.approach.calendar.domain.model.CalendarTaskWithTagsAndPomodoro;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class CalendarExtensions {

    // Статический метод для преобразования
    public static List<CalendarTaskSummary> toTaskSummaries(
            List<CalendarTaskWithTagsAndPomodoro> tasksWithDetails,
            PriorityResolver priorityResolver,
            DateTimeUtils dateTimeUtils
    ) {
        if (tasksWithDetails == null) {
            return new ArrayList<>();
        }
        return tasksWithDetails.stream()
                .map(taskWithTags -> {
                    // 1. Берем dueDate из БД (он представляет UTC)
                    LocalDateTime utcDueDate = taskWithTags.getTask().getDueDate();
                    // 2. Конвертируем в локальное время
                    LocalDateTime localDueDate = dateTimeUtils.utcToLocalLocalDateTime(utcDueDate);
                    // 3. Передаем локальное время в резолвер
                    Priority calculatedPriority = priorityResolver.resolve(localDueDate, taskWithTags.getTask().getStatus());

                    // subtaskProgress пока не реализован в CalendarTaskWithTagsAndPomodoro, ставим null
                    Float subtaskProgress = null;
                    // recurrenceRule берем из CalendarParams
                    String recurrenceRule = taskWithTags.getCalendarParams() != null ? taskWithTags.getCalendarParams().getRecurrenceRule() : null;


                    return new CalendarTaskSummary(
                            taskWithTags.getTask().getId(),
                            taskWithTags.getTask().getTitle(),
                            taskWithTags.getTask().getDescription(),
                            localDueDate,
                            taskWithTags.getTask().getStatus(),
                            calculatedPriority,
                            taskWithTags.getPomodoroCount(),
                            taskWithTags.getTags(),
                            recurrenceRule,
                            subtaskProgress
                    );
                })
                .collect(Collectors.toList());
    }
}