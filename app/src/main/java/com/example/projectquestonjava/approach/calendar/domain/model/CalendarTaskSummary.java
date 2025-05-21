package com.example.projectquestonjava.approach.calendar.domain.model;

import androidx.annotation.Nullable;
import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.core.data.model.enums.Priority;
import com.example.projectquestonjava.core.data.model.enums.TaskStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarTaskSummary {
    private long id;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private TaskStatus status;
    private Priority priority;
    private int pomodoroCount;
    private List<Tag> tags;
    @Nullable
    private String recurrenceRule;
    @Nullable
    private Float subtaskProgress;
}